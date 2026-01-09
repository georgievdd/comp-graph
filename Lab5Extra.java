import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Лабораторная работа №5 - Дополнительное задание
 *
 * Два кубоида с уникальными цветами граней движутся по круговой орбите.
 * - Кубоиды периодически заслоняют друг друга (Z-buffer)
 * - При приближении увеличиваются, при удалении уменьшаются
 * - Вращаются вокруг своих осей
 * - Двухточечная перспективная проекция:
 *   • Кубоид 1: точка схода на плоскости XZ
 *   • Кубоид 2: точка схода на плоскости YZ
 */
public class Lab5Extra extends JFrame {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    // Параметры орбиты
    private double orbitAngle = 0.0;
    private static final double ORBIT_RADIUS = 150.0;
    private static final double ORBIT_SPEED = 0.015;
    private static final double Z_OFFSET = 800.0; // Смещение от камеры

    // Параметры анимации
    private Timer animationTimer;

    // Запись кадров
    private boolean recordFrames = false;
    private int frameCount = 0;
    private static final int MAX_FRAMES = 300; // 5 секунд при 60 FPS

    // Вращение каждого кубоида вокруг своей оси
    private double rotAngle1 = 0.0;
    private double rotAngle2 = 0.0;
    private static final double ROT_SPEED1 = 0.03;
    private static final double ROT_SPEED2 = 0.02;

    // Оси вращения для каждого кубоида
    private Point3D axis1 = new Point3D(1, 1, 0).normalize();
    private Point3D axis2 = new Point3D(0, 1, 1).normalize();

    // Два цветных кубоида
    private ColoredBox box1;
    private ColoredBox box2;

    // Параметры двухточечной перспективы
    private static final double PERSPECTIVE_DX = 600.0;
    private static final double PERSPECTIVE_DY = 600.0;
    private static final double PERSPECTIVE_DZ = 500.0;

    // Параметры масштабирования
    private static final double BASE_SCALE = 1.2;
    private static final double CAMERA_DEPTH = 1000.0;

    /**
     * 3D точка/вектор
     */
    static class Point3D {
        double x, y, z;

        public Point3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Point3D add(Point3D other) {
            return new Point3D(x + other.x, y + other.y, z + other.z);
        }

        public Point3D subtract(Point3D other) {
            return new Point3D(x - other.x, y - other.y, z - other.z);
        }

        public Point3D multiply(double scalar) {
            return new Point3D(x * scalar, y * scalar, z * scalar);
        }

        public double dot(Point3D other) {
            return x * other.x + y * other.y + z * other.z;
        }

        public Point3D cross(Point3D other) {
            return new Point3D(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x
            );
        }

        public double length() {
            return Math.sqrt(x * x + y * y + z * z);
        }

        public Point3D normalize() {
            double len = length();
            return len > 0 ? new Point3D(x / len, y / len, z / len) : this;
        }

        @Override
        public String toString() {
            return String.format("(%.2f, %.2f, %.2f)", x, y, z);
        }
    }

    /**
     * 2D точка для проекций
     */
    static class Point2D {
        double x, y;

        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Точка в однородных координатах (x, y, z, w)
     */
    static class Point4D {
        double x, y, z, w;

        public Point4D(double x, double y, double z, double w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public Point2D toPoint2D() {
            if (Math.abs(w) < 1e-10) {
                return new Point2D(x * 1e6, y * 1e6);
            }
            return new Point2D(x / w, y / w);
        }
    }

    static class Edge {
        int v1, v2;

        public Edge(int v1, int v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
    }

    static class Face {
        int[] vertices;

        public Face(int v0, int v1, int v2, int v3) {
            this.vertices = new int[]{v0, v1, v2, v3};
        }

        public Point3D getNormal(Point3D[] verts) {
            Point3D v0 = verts[vertices[0]];
            Point3D v1 = verts[vertices[1]];
            Point3D v2 = verts[vertices[2]];

            Point3D edge1 = v1.subtract(v0);
            Point3D edge2 = v2.subtract(v0);

            return edge1.cross(edge2).normalize();
        }

        public boolean isVisible(Point3D[] verts, Point3D viewDir) {
            Point3D normal = getNormal(verts);
            return normal.dot(viewDir) < 0.1;
        }
    }

    /**
     * Базовый параллелепипед
     */
    static class Box {
        Point3D[] vertices;
        Edge[] edges;
        Face[] faces;

        public Box(double width, double height, double depth) {
            vertices = new Point3D[8];
            double w2 = width / 2;
            double h2 = height / 2;
            double d2 = depth / 2;

            // Нижняя грань (z = -d2)
            vertices[0] = new Point3D(-w2, -h2, -d2);
            vertices[1] = new Point3D( w2, -h2, -d2);
            vertices[2] = new Point3D( w2,  h2, -d2);
            vertices[3] = new Point3D(-w2,  h2, -d2);

            // Верхняя грань (z = +d2)
            vertices[4] = new Point3D(-w2, -h2,  d2);
            vertices[5] = new Point3D( w2, -h2,  d2);
            vertices[6] = new Point3D( w2,  h2,  d2);
            vertices[7] = new Point3D(-w2,  h2,  d2);

            // 12 ребер
            edges = new Edge[12];
            edges[0] = new Edge(0, 1);
            edges[1] = new Edge(1, 2);
            edges[2] = new Edge(2, 3);
            edges[3] = new Edge(3, 0);
            edges[4] = new Edge(4, 5);
            edges[5] = new Edge(5, 6);
            edges[6] = new Edge(6, 7);
            edges[7] = new Edge(7, 4);
            edges[8] = new Edge(0, 4);
            edges[9] = new Edge(1, 5);
            edges[10] = new Edge(2, 6);
            edges[11] = new Edge(3, 7);

            // 6 граней
            faces = new Face[6];
            faces[0] = new Face(0, 1, 2, 3); // Нижняя (z-)
            faces[1] = new Face(4, 7, 6, 5); // Верхняя (z+)
            faces[2] = new Face(0, 4, 5, 1); // Передняя (y-)
            faces[3] = new Face(2, 6, 7, 3); // Задняя (y+)
            faces[4] = new Face(1, 5, 6, 2); // Правая (x+)
            faces[5] = new Face(0, 3, 7, 4); // Левая (x-)
        }
    }

    /**
     * Цветной параллелепипед с уникальными цветами граней
     */
    static class ColoredBox extends Box {
        Color[] faceColors;

        public ColoredBox(double width, double height, double depth, Color[] colors) {
            super(width, height, depth);
            this.faceColors = colors;
        }

        public static ColoredBox createWarmBox(double w, double h, double d) {
            Color[] colors = {
                new Color(220, 50, 50),    // Красный (низ)
                new Color(50, 180, 50),    // Зелёный (верх)
                new Color(50, 100, 220),   // Синий (перед)
                new Color(255, 140, 0),    // Оранжевый (зад)
                new Color(255, 220, 50),   // Жёлтый (право)
                new Color(160, 50, 200)    // Пурпурный (лево)
            };
            return new ColoredBox(w, h, d, colors);
        }

        public static ColoredBox createCoolBox(double w, double h, double d) {
            Color[] colors = {
                new Color(0, 200, 200),    // Циан (низ)
                new Color(200, 50, 200),   // Маджента (верх)
                new Color(100, 255, 100),  // Лайм (перед)
                new Color(255, 150, 180),  // Розовый (зад)
                new Color(0, 150, 150),    // Бирюзовый (право)
                new Color(255, 200, 100)   // Золотой (лево)
            };
            return new ColoredBox(w, h, d, colors);
        }
    }

    // ===== Матричные операции =====

    public static double[][] createRotationMatrix(double angle, Point3D axis) {
        axis = axis.normalize();
        double x = axis.x;
        double y = axis.y;
        double z = axis.z;

        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double t = 1 - c;

        double[][] matrix = new double[4][4];

        matrix[0][0] = t*x*x + c;
        matrix[0][1] = t*x*y - s*z;
        matrix[0][2] = t*x*z + s*y;
        matrix[0][3] = 0;

        matrix[1][0] = t*x*y + s*z;
        matrix[1][1] = t*y*y + c;
        matrix[1][2] = t*y*z - s*x;
        matrix[1][3] = 0;

        matrix[2][0] = t*x*z - s*y;
        matrix[2][1] = t*y*z + s*x;
        matrix[2][2] = t*z*z + c;
        matrix[2][3] = 0;

        matrix[3][0] = 0;
        matrix[3][1] = 0;
        matrix[3][2] = 0;
        matrix[3][3] = 1;

        return matrix;
    }

    public static Point3D transformPoint(Point3D p, double[][] matrix) {
        double x = matrix[0][0] * p.x + matrix[0][1] * p.y + matrix[0][2] * p.z + matrix[0][3];
        double y = matrix[1][0] * p.x + matrix[1][1] * p.y + matrix[1][2] * p.z + matrix[1][3];
        double z = matrix[2][0] * p.x + matrix[2][1] * p.y + matrix[2][2] * p.z + matrix[2][3];
        return new Point3D(x, y, z);
    }

    public static Point4D transformPoint4D(Point3D p, double[][] matrix) {
        double x = matrix[0][0] * p.x + matrix[0][1] * p.y + matrix[0][2] * p.z + matrix[0][3];
        double y = matrix[1][0] * p.x + matrix[1][1] * p.y + matrix[1][2] * p.z + matrix[1][3];
        double z = matrix[2][0] * p.x + matrix[2][1] * p.y + matrix[2][2] * p.z + matrix[2][3];
        double w = matrix[3][0] * p.x + matrix[3][1] * p.y + matrix[3][2] * p.z + matrix[3][3];
        return new Point4D(x, y, z, w);
    }

    public static double[][] createTranslationMatrix(double tx, double ty, double tz) {
        double[][] matrix = new double[4][4];
        matrix[0][0] = 1;  matrix[0][1] = 0;  matrix[0][2] = 0;  matrix[0][3] = tx;
        matrix[1][0] = 0;  matrix[1][1] = 1;  matrix[1][2] = 0;  matrix[1][3] = ty;
        matrix[2][0] = 0;  matrix[2][1] = 0;  matrix[2][2] = 1;  matrix[2][3] = tz;
        matrix[3][0] = 0;  matrix[3][1] = 0;  matrix[3][2] = 0;  matrix[3][3] = 1;
        return matrix;
    }

    public static double[][] multiplyMatrices(double[][] a, double[][] b) {
        double[][] result = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result[i][j] = 0;
                for (int k = 0; k < 4; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }

    /**
     * Двухточечная перспективная проекция с точками схода на XZ
     */
    public static double[][] createTwoPointPerspectiveXZ(double dx, double dz) {
        double[][] matrix = new double[4][4];
        matrix[0][0] = 1;       matrix[0][1] = 0;  matrix[0][2] = 0;        matrix[0][3] = 0;
        matrix[1][0] = 0;       matrix[1][1] = 1;  matrix[1][2] = 0;        matrix[1][3] = 0;
        matrix[2][0] = 0;       matrix[2][1] = 0;  matrix[2][2] = 0;        matrix[2][3] = 0;
        matrix[3][0] = -1.0/dx; matrix[3][1] = 0;  matrix[3][2] = -1.0/dz;  matrix[3][3] = 1;
        return matrix;
    }

    /**
     * Двухточечная перспективная проекция с точками схода на YZ
     */
    public static double[][] createTwoPointPerspectiveYZ(double dy, double dz) {
        double[][] matrix = new double[4][4];
        matrix[0][0] = 1;  matrix[0][1] = 0;        matrix[0][2] = 0;        matrix[0][3] = 0;
        matrix[1][0] = 0;  matrix[1][1] = 1;        matrix[1][2] = 0;        matrix[1][3] = 0;
        matrix[2][0] = 0;  matrix[2][1] = 0;        matrix[2][2] = 0;        matrix[2][3] = 0;
        matrix[3][0] = 0;  matrix[3][1] = -1.0/dy;  matrix[3][2] = -1.0/dz;  matrix[3][3] = 1;
        return matrix;
    }

    // ===== Растеризация =====

    /**
     * Рисование цветной линии алгоритмом Брезенхема с учётом Z-буфера
     */
    public static void drawColorLineWithZBuffer(int[] pixels, double[] zBuffer, int width, int height,
                                                 int x0, int y0, double z0,
                                                 int x1, int y1, double z1, int rgb) {
        boolean swapped = false;
        if (x0 > x1 || (x0 == x1 && y0 > y1)) {
            int temp;
            temp = x0; x0 = x1; x1 = temp;
            temp = y0; y0 = y1; y1 = temp;
            double tempZ = z0; z0 = z1; z1 = tempZ;
            swapped = true;
        }

        int dx = x1 - x0;
        int dy = y1 - y0;
        int sx = 1;
        int sy = dy >= 0 ? 1 : -1;
        dy = Math.abs(dy);

        // Длина линии для интерполяции Z
        double lineLength = Math.sqrt(dx * dx + dy * dy);
        if (lineLength < 1e-10) lineLength = 1.0;

        int err = dx - dy;
        int x = x0;
        int y = y0;
        int steps = 0;

        while (true) {
            if (x >= 0 && x < width && y >= 0 && y < height) {
                // Интерполяция Z вдоль линии
                double t = Math.sqrt((x - x0) * (x - x0) + (y - y0) * (y - y0)) / lineLength;
                double z = z0 + (z1 - z0) * t;

                int idx = y * width + x;
                // Рисуем только если ближе к камере (с небольшим смещением для видимости)
                if (z < zBuffer[idx] + 0.5) {
                    pixels[idx] = rgb;
                }
            }

            if (x == x1 && y == y1) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
            steps++;
        }
    }

    /**
     * Заливка треугольника с Z-буфером
     */
    public static void fillTriangleWithZBuffer(
            double[] xPts, double[] yPts, double[] zPts,
            int rgb,
            int[] pixels, double[] zBuffer,
            int width, int height) {

        int minX = (int) Math.floor(Math.min(xPts[0], Math.min(xPts[1], xPts[2])));
        int maxX = (int) Math.ceil(Math.max(xPts[0], Math.max(xPts[1], xPts[2])));
        int minY = (int) Math.floor(Math.min(yPts[0], Math.min(yPts[1], yPts[2])));
        int maxY = (int) Math.ceil(Math.max(yPts[0], Math.max(yPts[1], yPts[2])));

        minX = Math.max(0, minX);
        maxX = Math.min(width - 1, maxX);
        minY = Math.max(0, minY);
        maxY = Math.min(height - 1, maxY);

        double x0 = xPts[0], y0 = yPts[0], z0 = zPts[0];
        double x1 = xPts[1], y1 = yPts[1], z1 = zPts[1];
        double x2 = xPts[2], y2 = yPts[2], z2 = zPts[2];

        double denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2);
        if (Math.abs(denom) < 1e-10) return;

        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                double w0 = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / denom;
                double w1 = ((y2 - y0) * (px - x2) + (x0 - x2) * (py - y2)) / denom;
                double w2 = 1 - w0 - w1;

                if (w0 >= 0 && w1 >= 0 && w2 >= 0) {
                    double z = w0 * z0 + w1 * z1 + w2 * z2;

                    int idx = py * width + px;
                    if (z < zBuffer[idx]) {
                        zBuffer[idx] = z;
                        pixels[idx] = rgb;
                    }
                }
            }
        }
    }

    /**
     * Заливка грани (4 вершины) с Z-буфером
     */
    public static void fillFaceWithZBuffer(
            Point2D[] pts2D, double[] zVals,
            int rgb,
            int[] pixels, double[] zBuffer,
            int width, int height) {

        fillTriangleWithZBuffer(
            new double[]{pts2D[0].x, pts2D[1].x, pts2D[2].x},
            new double[]{pts2D[0].y, pts2D[1].y, pts2D[2].y},
            new double[]{zVals[0], zVals[1], zVals[2]},
            rgb, pixels, zBuffer, width, height
        );

        fillTriangleWithZBuffer(
            new double[]{pts2D[0].x, pts2D[2].x, pts2D[3].x},
            new double[]{pts2D[0].y, pts2D[2].y, pts2D[3].y},
            new double[]{zVals[0], zVals[2], zVals[3]},
            rgb, pixels, zBuffer, width, height
        );
    }

    /**
     * Рендер цветного кубоида с Z-буфером
     */
    public static void renderColoredBox(
            ColoredBox box,
            Point3D[] vertices3D,
            double[][] projMatrix,
            int[] pixels, double[] zBuffer,
            int width, int height,
            int offsetX, int offsetY, double scale,
            boolean drawEdges) {

        // Проецируем вершины
        Point2D[] projected = new Point2D[vertices3D.length];
        double[] zValues = new double[vertices3D.length];

        for (int i = 0; i < vertices3D.length; i++) {
            Point4D p4 = transformPoint4D(vertices3D[i], projMatrix);
            Point2D p2 = p4.toPoint2D();
            projected[i] = new Point2D(p2.x * scale + offsetX, -p2.y * scale + offsetY);
            zValues[i] = vertices3D[i].z;
        }

        Point3D viewDir = new Point3D(0, 0, -1);

        // Рисуем грани
        for (int f = 0; f < box.faces.length; f++) {
            Face face = box.faces[f];

            if (!face.isVisible(vertices3D, viewDir)) continue;

            Point2D[] facePts = new Point2D[4];
            double[] faceZ = new double[4];
            for (int i = 0; i < 4; i++) {
                int vi = face.vertices[i];
                facePts[i] = projected[vi];
                faceZ[i] = zValues[vi];
            }

            int rgb = box.faceColors[f].getRGB();
            fillFaceWithZBuffer(facePts, faceZ, rgb, pixels, zBuffer, width, height);
        }

        // Рисуем рёбра с учётом Z-буфера
        if (drawEdges) {
            int edgeColor = 0xFF000000;
            for (Edge edge : box.edges) {
                Point2D p1 = projected[edge.v1];
                Point2D p2 = projected[edge.v2];
                double z1 = zValues[edge.v1];
                double z2 = zValues[edge.v2];
                drawColorLineWithZBuffer(pixels, zBuffer, width, height,
                    (int) p1.x, (int) p1.y, z1,
                    (int) p2.x, (int) p2.y, z2, edgeColor);
            }
        }
    }

    public Lab5Extra() {
        setTitle("Lab 5 Extra: Two Cuboids on Orbit with Two-Point Perspective");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Создаём два цветных кубоида
        box1 = ColoredBox.createWarmBox(80, 100, 60);
        box2 = ColoredBox.createCoolBox(70, 90, 70);

        DemoPanel panel = new DemoPanel();
        add(panel);
        startAnimation(panel);
    }

    private void startAnimation(DemoPanel panel) {
        animationTimer = new Timer(16, e -> {
            orbitAngle += ORBIT_SPEED;
            rotAngle1 += ROT_SPEED1;
            rotAngle2 += ROT_SPEED2;
            panel.repaint();
        });
        animationTimer.start();
    }

    private class DemoPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBackground(new Color(240, 240, 245));

            int w = getWidth();
            int h = getHeight();

            // Создаём RGB буфер и Z-буфер
            int[] pixels = new int[w * h];
            double[] zBuffer = new double[w * h];

            int bgColor = 0xFFF0F0F5;
            Arrays.fill(pixels, bgColor);
            Arrays.fill(zBuffer, Double.MAX_VALUE);

            int centerX = w / 2;
            int centerY = h / 2;

            // Вычисляем позиции кубоидов на орбите
            double x1 = ORBIT_RADIUS * Math.cos(orbitAngle);
            double z1 = ORBIT_RADIUS * Math.sin(orbitAngle) + Z_OFFSET;
            double x2 = ORBIT_RADIUS * Math.cos(orbitAngle + Math.PI);
            double z2 = ORBIT_RADIUS * Math.sin(orbitAngle + Math.PI) + Z_OFFSET;

            // Масштабирование на основе глубины
            double scale1 = BASE_SCALE * (CAMERA_DEPTH / (CAMERA_DEPTH + z1));
            double scale2 = BASE_SCALE * (CAMERA_DEPTH / (CAMERA_DEPTH + z2));

            // Трансформации для кубоида 1 (тёплые цвета, XZ проекция)
            double[][] rot1 = createRotationMatrix(rotAngle1, axis1);
            double[][] trans1 = createTranslationMatrix(x1, 0, z1);
            double[][] combined1 = multiplyMatrices(trans1, rot1);
            Point3D[] verts1 = new Point3D[box1.vertices.length];
            for (int i = 0; i < box1.vertices.length; i++) {
                verts1[i] = transformPoint(box1.vertices[i], combined1);
            }

            // Трансформации для кубоида 2 (холодные цвета, YZ проекция)
            double[][] rot2 = createRotationMatrix(rotAngle2, axis2);
            double[][] trans2 = createTranslationMatrix(x2, 0, z2);
            double[][] combined2 = multiplyMatrices(trans2, rot2);
            Point3D[] verts2 = new Point3D[box2.vertices.length];
            for (int i = 0; i < box2.vertices.length; i++) {
                verts2[i] = transformPoint(box2.vertices[i], combined2);
            }

            // Матрицы двухточечной перспективы
            double[][] projXZ = createTwoPointPerspectiveXZ(PERSPECTIVE_DX, PERSPECTIVE_DZ);
            double[][] projYZ = createTwoPointPerspectiveYZ(PERSPECTIVE_DY, PERSPECTIVE_DZ);

            // Рендерим кубоиды (сначала дальний)
            if (z1 > z2) {
                renderColoredBox(box1, verts1, projXZ, pixels, zBuffer, w, h, centerX, centerY, scale1, true);
                renderColoredBox(box2, verts2, projYZ, pixels, zBuffer, w, h, centerX, centerY, scale2, true);
            } else {
                renderColoredBox(box2, verts2, projYZ, pixels, zBuffer, w, h, centerX, centerY, scale2, true);
                renderColoredBox(box1, verts1, projXZ, pixels, zBuffer, w, h, centerX, centerY, scale1, true);
            }

            // Выводим буфер на экран
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, w, h, pixels, 0, w);
            g.drawImage(img, 0, 0, null);

            // Сохранение кадров для GIF
            if (recordFrames && frameCount < MAX_FRAMES) {
                saveFrame(img);
                frameCount++;
                if (frameCount >= MAX_FRAMES) {
                    recordFrames = false;
                    System.out.println("Recording complete! " + MAX_FRAMES + " frames saved.");
                    System.out.println("Create GIF with: ffmpeg -framerate 60 -i res/frames/frame_%04d.png -vf \"fps=30,scale=600:-1:flags=lanczos\" res/lab5_extra.gif");
                }
            }
        }

        private void saveFrame(BufferedImage img) {
            try {
                File outputDir = new File("res/frames");
                outputDir.mkdirs();
                String filename = String.format("res/frames/frame_%04d.png", frameCount);
                ImageIO.write(img, "png", new File(filename));
                if (frameCount % 60 == 0) {
                    System.out.println("Saved " + frameCount + " frames...");
                }
            } catch (IOException ex) {
                System.err.println("Error saving frame: " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Lab5Extra lab = new Lab5Extra();
            lab.setVisible(true);

            // Автоматически начать запись, если передан аргумент --record
            if (args.length > 0 && args[0].equals("--record")) {
                System.out.println("Starting frame recording...");
                lab.recordFrames = true;
            }
        });
    }
}
