import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Лабораторная работа №5: 3D проекции и анимация
 * 1. Параллельная проекция повернутого параллелепипеда
 * 2. Перспективная проекция повернутого параллелепипеда
 * 3. Удаление невидимых ребер
 * 4. Анимация вращения вокруг произвольной оси
 */
public class Lab5 extends JFrame {
    private static final int WIDTH = 1400;
    private static final int HEIGHT = 800;

    // Параметры проекции (сильный эффект перспективы)
    private static final double PERSPECTIVE_K = 200.0; // Расстояние до центра проекции

    // Параметры анимации
    private double angle = 0.0;
    private Timer animationTimer;

    // Ось вращения (нормализованная)
    private double axisX = 1.0;
    private double axisY = 1.0;
    private double axisZ = 1.0;

    // Режим отображения
    private boolean showParallel = true;
    private boolean showPerspective = true;
    private boolean removeHiddenLines = true;
    private boolean animate = true;

    // === Дополнительное задание: два кубоида на орбите ===
    private boolean extraMode = false;  // Переключатель режима

    // Параметры орбиты
    private double orbitAngle = 0.0;
    private static final double ORBIT_RADIUS = 180.0;

    // Вращение каждого кубоида вокруг своей оси
    private double rotAngle1 = 0.0;
    private double rotAngle2 = 0.0;
    private Point3D axis1 = new Point3D(1, 1, 0).normalize();  // Ось кубоида 1
    private Point3D axis2 = new Point3D(0, 1, 1).normalize();  // Ось кубоида 2

    // Два цветных кубоида
    private ColoredBox box1;
    private ColoredBox box2;

    // Параметры двухточечной перспективы
    private static final double PERSPECTIVE_DX = 400.0;  // Точка схода по X
    private static final double PERSPECTIVE_DY = 400.0;  // Точка схода по Y
    private static final double PERSPECTIVE_DZ = 300.0;  // Точка схода по Z

    // Показывать рёбра
    private boolean showEdges = true;

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

        public Point4D(Point3D p) {
            this.x = p.x;
            this.y = p.y;
            this.z = p.z;
            this.w = 1.0;
        }

        /**
         * Преобразование из однородных координат в 3D (деление на w)
         */
        public Point3D toPoint3D() {
            if (Math.abs(w) < 1e-10) {
                return new Point3D(x * 1e6, y * 1e6, z * 1e6);
            }
            return new Point3D(x / w, y / w, z / w);
        }

        /**
         * Преобразование в 2D (проекция на z=0 с делением на w)
         */
        public Point2D toPoint2D() {
            if (Math.abs(w) < 1e-10) {
                return new Point2D(x * 1e6, y * 1e6);
            }
            return new Point2D(x / w, y / w);
        }
    }

    static class Edge {
        int v1, v2; // Индексы вершин

        public Edge(int v1, int v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
    }

    static class Face {
        int[] vertices; // Индексы вершин по часовой стрелке

        public Face(int v0, int v1, int v2, int v3) {
            this.vertices = new int[]{v0, v1, v2, v3};
        }

        /**
         * Вычисляет нормаль к грани
         */
        public Point3D getNormal(Point3D[] verts) {
            Point3D v0 = verts[vertices[0]];
            Point3D v1 = verts[vertices[1]];
            Point3D v2 = verts[vertices[2]];

            Point3D edge1 = v1.subtract(v0);
            Point3D edge2 = v2.subtract(v0);

            return edge1.cross(edge2).normalize();
        }

        /**
         * Проверяет, видима ли грань (back-face culling)
         */
        public boolean isVisible(Point3D[] verts, Point3D viewDir) {
            Point3D normal = getNormal(verts);
            // Грань видима, если нормаль направлена к наблюдателю
            return normal.dot(viewDir) > 0;
        }
    }

    /**
     * Параллелепипед
     */
    static class Box {
        Point3D[] vertices;
        Edge[] edges;
        Face[] faces;

        public Box(double width, double height, double depth) {
            // 8 вершин параллелепипеда
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
            // Нижняя грань
            edges[0] = new Edge(0, 1);
            edges[1] = new Edge(1, 2);
            edges[2] = new Edge(2, 3);
            edges[3] = new Edge(3, 0);
            // Верхняя грань
            edges[4] = new Edge(4, 5);
            edges[5] = new Edge(5, 6);
            edges[6] = new Edge(6, 7);
            edges[7] = new Edge(7, 4);
            // Вертикальные ребра
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

        /**
         * Применяет матрицу трансформации ко всем вершинам
         */
        public Point3D[] transform(double[][] matrix) {
            Point3D[] transformed = new Point3D[vertices.length];
            for (int i = 0; i < vertices.length; i++) {
                transformed[i] = transformPoint(vertices[i], matrix);
            }
            return transformed;
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

        /**
         * Создаёт кубоид с тёплыми цветами (красный, оранжевый, жёлтый...)
         */
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

        /**
         * Создаёт кубоид с холодными цветами (голубой, бирюзовый, фиолетовый...)
         */
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

    public static double[][] createRotationMatrix(double angle, Point3D axis) {
        // Нормализуем ось
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

    /**
     * Преобразование точки в однородных координатах матрицей 4x4
     */
    public static Point4D transformPoint4D(Point3D p, double[][] matrix) {
        double x = matrix[0][0] * p.x + matrix[0][1] * p.y + matrix[0][2] * p.z + matrix[0][3];
        double y = matrix[1][0] * p.x + matrix[1][1] * p.y + matrix[1][2] * p.z + matrix[1][3];
        double z = matrix[2][0] * p.x + matrix[2][1] * p.y + matrix[2][2] * p.z + matrix[2][3];
        double w = matrix[3][0] * p.x + matrix[3][1] * p.y + matrix[3][2] * p.z + matrix[3][3];
        return new Point4D(x, y, z, w);
    }

    /**
     * Матрица параллельной проекции на плоскость Z=0 (однородные координаты)
     */
    public static double[][] createParallelProjectionMatrix() {
        double[][] matrix = new double[4][4];
        matrix[0][0] = 1;  matrix[0][1] = 0;  matrix[0][2] = 0;  matrix[0][3] = 0;
        matrix[1][0] = 0;  matrix[1][1] = 1;  matrix[1][2] = 0;  matrix[1][3] = 0;
        matrix[2][0] = 0;  matrix[2][1] = 0;  matrix[2][2] = 0;  matrix[2][3] = 0;
        matrix[3][0] = 0;  matrix[3][1] = 0;  matrix[3][2] = 0;  matrix[3][3] = 1;
        return matrix;
    }

    /**
     * Матрица перспективной проекции на плоскость Z=0 с центром в (0, 0, k)
     * В однородных координатах: после умножения на матрицу получаем (x, y, z', w)
     * где w = 1 - z/k. После деления на w: x' = x*k/(k-z), y' = y*k/(k-z)
     */
    public static double[][] createPerspectiveProjectionMatrix(double k) {
        double[][] matrix = new double[4][4];
        matrix[0][0] = 1;  matrix[0][1] = 0;  matrix[0][2] = 0;       matrix[0][3] = 0;
        matrix[1][0] = 0;  matrix[1][1] = 1;  matrix[1][2] = 0;       matrix[1][3] = 0;
        matrix[2][0] = 0;  matrix[2][1] = 0;  matrix[2][2] = 0;       matrix[2][3] = 0;
        matrix[3][0] = 0;  matrix[3][1] = 0;  matrix[3][2] = -1.0/k;  matrix[3][3] = 1;
        return matrix;
    }

    /**
     * Матрица двухточечной перспективной проекции с точками схода на XZ
     * Точка схода 1: по оси X (на расстоянии dx)
     * Точка схода 2: по оси Z (на расстоянии dz)
     * Вертикальные линии (параллельные Y) остаются вертикальными
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
     * Матрица двухточечной перспективной проекции с точками схода на YZ
     * Точка схода 1: по оси Y (на расстоянии dy)
     * Точка схода 2: по оси Z (на расстоянии dz)
     * Горизонтальные линии (параллельные X) остаются горизонтальными
     */
    public static double[][] createTwoPointPerspectiveYZ(double dy, double dz) {
        double[][] matrix = new double[4][4];
        matrix[0][0] = 1;  matrix[0][1] = 0;        matrix[0][2] = 0;        matrix[0][3] = 0;
        matrix[1][0] = 0;  matrix[1][1] = 1;        matrix[1][2] = 0;        matrix[1][3] = 0;
        matrix[2][0] = 0;  matrix[2][1] = 0;        matrix[2][2] = 0;        matrix[2][3] = 0;
        matrix[3][0] = 0;  matrix[3][1] = -1.0/dy;  matrix[3][2] = -1.0/dz;  matrix[3][3] = 1;
        return matrix;
    }

    /**
     * Матрица переноса (однородные координаты)
     */
    public static double[][] createTranslationMatrix(double tx, double ty, double tz) {
        double[][] matrix = new double[4][4];
        matrix[0][0] = 1;  matrix[0][1] = 0;  matrix[0][2] = 0;  matrix[0][3] = tx;
        matrix[1][0] = 0;  matrix[1][1] = 1;  matrix[1][2] = 0;  matrix[1][3] = ty;
        matrix[2][0] = 0;  matrix[2][1] = 0;  matrix[2][2] = 1;  matrix[2][3] = tz;
        matrix[3][0] = 0;  matrix[3][1] = 0;  matrix[3][2] = 0;  matrix[3][3] = 1;
        return matrix;
    }

    /**
     * Умножение двух матриц 4x4
     */
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
     * Параллельная проекция на плоскость Z=0 (через матрицу однородных координат)
     */
    public static Point2D parallelProjection(Point3D p) {
        double[][] projMatrix = createParallelProjectionMatrix();
        Point4D result = transformPoint4D(p, projMatrix);
        return result.toPoint2D();
    }

    /**
     * Перспективная проекция с центром в (0, 0, k) (через матрицу однородных координат)
     */
    public static Point2D perspectiveProjection(Point3D p, double k) {
        double[][] projMatrix = createPerspectiveProjectionMatrix(k);
        Point4D result = transformPoint4D(p, projMatrix);
        return result.toPoint2D();
    }

    /**
     * Рисование линии алгоритмом Брезенхема
     */
    public static void drawLine(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        WritableRaster raster = img.getRaster();
        int width = img.getWidth();
        int height = img.getHeight();

        if (x0 > x1 || (x0 == x1 && y0 > y1)) {
            int temp;
            temp = x0; x0 = x1; x1 = temp;
            temp = y0; y0 = y1; y1 = temp;
        }

        int dx = x1 - x0;
        int dy = y1 - y0;
        int sx = 1;
        int sy = dy >= 0 ? 1 : -1;
        dy = Math.abs(dy);

        int err = dx - dy;
        int x = x0;
        int y = y0;

        while (true) {
            if (x >= 0 && x < width && y >= 0 && y < height) {
                raster.setSample(x, y, 0, color);
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
        }
    }

    /**
     * Рисование цветной линии в RGB буфер
     */
    public static void drawColorLine(int[] pixels, int width, int height,
                                      int x0, int y0, int x1, int y1, int rgb) {
        if (x0 > x1 || (x0 == x1 && y0 > y1)) {
            int temp;
            temp = x0; x0 = x1; x1 = temp;
            temp = y0; y0 = y1; y1 = temp;
        }

        int dx = x1 - x0;
        int dy = y1 - y0;
        int sx = 1;
        int sy = dy >= 0 ? 1 : -1;
        dy = Math.abs(dy);

        int err = dx - dy;
        int x = x0;
        int y = y0;

        while (true) {
            if (x >= 0 && x < width && y >= 0 && y < height) {
                pixels[y * width + x] = rgb;
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
        }
    }

    /**
     * Заливка треугольника с Z-буфером
     * Использует барицентрические координаты для интерполяции Z
     */
    public static void fillTriangleWithZBuffer(
            double[] xPts, double[] yPts, double[] zPts,  // 3 вершины
            int rgb,
            int[] pixels, double[] zBuffer,
            int width, int height) {

        // Bounding box
        int minX = (int) Math.floor(Math.min(xPts[0], Math.min(xPts[1], xPts[2])));
        int maxX = (int) Math.ceil(Math.max(xPts[0], Math.max(xPts[1], xPts[2])));
        int minY = (int) Math.floor(Math.min(yPts[0], Math.min(yPts[1], yPts[2])));
        int maxY = (int) Math.ceil(Math.max(yPts[0], Math.max(yPts[1], yPts[2])));

        // Clipping
        minX = Math.max(0, minX);
        maxX = Math.min(width - 1, maxX);
        minY = Math.max(0, minY);
        maxY = Math.min(height - 1, maxY);

        // Предвычисления для барицентрических координат
        double x0 = xPts[0], y0 = yPts[0], z0 = zPts[0];
        double x1 = xPts[1], y1 = yPts[1], z1 = zPts[1];
        double x2 = xPts[2], y2 = yPts[2], z2 = zPts[2];

        double denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2);
        if (Math.abs(denom) < 1e-10) return; // Вырожденный треугольник

        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                // Барицентрические координаты
                double w0 = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / denom;
                double w1 = ((y2 - y0) * (px - x2) + (x0 - x2) * (py - y2)) / denom;
                double w2 = 1 - w0 - w1;

                // Проверка, внутри ли точка треугольника
                if (w0 >= 0 && w1 >= 0 && w2 >= 0) {
                    // Интерполяция Z
                    double z = w0 * z0 + w1 * z1 + w2 * z2;

                    int idx = py * width + px;
                    // Z-тест (меньше Z = ближе к камере)
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
     * Разбивает грань на 2 треугольника
     */
    public static void fillFaceWithZBuffer(
            Point2D[] pts2D, double[] zVals,  // 4 вершины
            int rgb,
            int[] pixels, double[] zBuffer,
            int width, int height) {

        // Треугольник 1: вершины 0, 1, 2
        fillTriangleWithZBuffer(
            new double[]{pts2D[0].x, pts2D[1].x, pts2D[2].x},
            new double[]{pts2D[0].y, pts2D[1].y, pts2D[2].y},
            new double[]{zVals[0], zVals[1], zVals[2]},
            rgb, pixels, zBuffer, width, height
        );

        // Треугольник 2: вершины 0, 2, 3
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
            zValues[i] = vertices3D[i].z; // Используем Z для буфера глубины
        }

        // Определяем направление взгляда (вдоль -Z)
        Point3D viewDir = new Point3D(0, 0, -1);

        // Рисуем грани (с back-face culling)
        for (int f = 0; f < box.faces.length; f++) {
            Face face = box.faces[f];

            // Back-face culling
            if (!face.isVisible(vertices3D, viewDir)) continue;

            // Получаем 2D координаты вершин грани
            Point2D[] facePts = new Point2D[4];
            double[] faceZ = new double[4];
            for (int i = 0; i < 4; i++) {
                int vi = face.vertices[i];
                facePts[i] = projected[vi];
                faceZ[i] = zValues[vi];
            }

            // Заливка грани
            int rgb = box.faceColors[f].getRGB();
            fillFaceWithZBuffer(facePts, faceZ, rgb, pixels, zBuffer, width, height);
        }

        // Рисуем рёбра (опционально)
        if (drawEdges) {
            int edgeColor = 0xFF000000; // Чёрный
            for (Edge edge : box.edges) {
                Point2D p1 = projected[edge.v1];
                Point2D p2 = projected[edge.v2];
                drawColorLine(pixels, width, height,
                    (int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y, edgeColor);
            }
        }
    }

    /**
     * Определяет, какие ребра видимы (удаление невидимых линий)
     */
    public static boolean[] getVisibleEdges(Point3D[] vertices, Box box, boolean isPerspective, double k) {
        boolean[] visible = new boolean[box.edges.length];

        // Для каждого ребра проверяем, принадлежит ли оно хотя бы одной видимой грани
        for (int i = 0; i < box.edges.length; i++) {
            Edge edge = box.edges[i];
            visible[i] = false;

            // Проверяем все грани
            for (Face face : box.faces) {
                // Определяем направление взгляда для данной грани
                Point3D viewDir;
                if (isPerspective) {
                    // Для перспективы: вектор от наблюдателя (0,0,k) к центру грани
                    Point3D faceCenter = getFaceCenter(vertices, face);
                    viewDir = new Point3D(faceCenter.x - 0, faceCenter.y - 0, faceCenter.z - k);
                } else {
                    // Для параллельной проекции: направление взгляда вдоль -Z
                    viewDir = new Point3D(0, 0, -1);
                }

                // Если грань видима и содержит это ребро
                if (face.isVisible(vertices, viewDir) && faceContainsEdge(face, edge)) {
                    visible[i] = true;
                    break;
                }
            }
        }

        return visible;
    }

    private static Point3D getFaceCenter(Point3D[] vertices, Face face) {
        double x = 0, y = 0, z = 0;
        for (int idx : face.vertices) {
            x += vertices[idx].x;
            y += vertices[idx].y;
            z += vertices[idx].z;
        }
        int n = face.vertices.length;
        return new Point3D(x / n, y / n, z / n);
    }

    private static boolean faceContainsEdge(Face face, Edge edge) {
        for (int i = 0; i < face.vertices.length; i++) {
            int v1 = face.vertices[i];
            int v2 = face.vertices[(i + 1) % face.vertices.length];

            if ((v1 == edge.v1 && v2 == edge.v2) || (v1 == edge.v2 && v2 == edge.v1)) {
                return true;
            }
        }
        return false;
    }

    public static void drawBox(BufferedImage img, Point3D[] vertices, Box box,
                               boolean isPerspective, double k,
                               boolean removeHidden, int offsetX, int offsetY, double scale) {
        // Определяем видимые ребра
        boolean[] visibleEdges = removeHidden
            ? getVisibleEdges(vertices, box, isPerspective, k)
            : new boolean[box.edges.length];

        if (!removeHidden) {
            for (int i = 0; i < visibleEdges.length; i++) {
                visibleEdges[i] = true;
            }
        }

        // Проецируем вершины
        Point2D[] projected = new Point2D[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            if (isPerspective) {
                projected[i] = perspectiveProjection(vertices[i], k);
            } else {
                projected[i] = parallelProjection(vertices[i]);
            }
        }

        // Рисуем видимые ребра
        for (int i = 0; i < box.edges.length; i++) {
            if (visibleEdges[i]) {
                Edge edge = box.edges[i];
                Point2D p1 = projected[edge.v1];
                Point2D p2 = projected[edge.v2];

                int x1 = (int) (p1.x * scale) + offsetX;
                int y1 = (int) (-p1.y * scale) + offsetY; // Инвертируем Y
                int x2 = (int) (p2.x * scale) + offsetX;
                int y2 = (int) (-p2.y * scale) + offsetY;

                drawLine(img, x1, y1, x2, y2, 0);
            }
        }
    }

    public Lab5() {
        setTitle("Lab 5: 3D Projections and Animation");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Нормализуем ось вращения
        Point3D axis = new Point3D(axisX, axisY, axisZ).normalize();
        axisX = axis.x;
        axisY = axis.y;
        axisZ = axis.z;

        // Создаём два цветных кубоида для дополнительного задания
        box1 = ColoredBox.createWarmBox(80, 100, 60);
        box2 = ColoredBox.createCoolBox(70, 90, 70);

        DemoPanel panel = new DemoPanel();
        add(panel);
        startAnimation(panel);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        animate = !animate;
                        if (animate && animationTimer == null) {
                            startAnimation(panel);
                        }
                        break;
                    case KeyEvent.VK_H:
                        removeHiddenLines = !removeHiddenLines;
                        panel.repaint();
                        break;
                    case KeyEvent.VK_R:
                        angle = 0;
                        orbitAngle = 0;
                        rotAngle1 = 0;
                        rotAngle2 = 0;
                        panel.repaint();
                        break;
                    case KeyEvent.VK_LEFT:
                        angle -= 0.1;
                        panel.repaint();
                        break;
                    case KeyEvent.VK_RIGHT:
                        angle += 0.1;
                        panel.repaint();
                        break;
                    case KeyEvent.VK_E:
                        // Переключение в режим дополнительного задания
                        extraMode = !extraMode;
                        panel.repaint();
                        break;
                    case KeyEvent.VK_D:
                        // Показать/скрыть рёбра
                        showEdges = !showEdges;
                        panel.repaint();
                        break;
                }
            }
        });

        setFocusable(true);
    }

    private void startAnimation(DemoPanel panel) {
        animationTimer = new Timer(16, e -> {
            if (animate) {
                angle += 0.02;
                // Обновление параметров для дополнительного задания
                orbitAngle += 0.015;
                rotAngle1 += 0.03;
                rotAngle2 += 0.02;
                panel.repaint();
            }
        });
        animationTimer.start();
    }

    private class DemoPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBackground(Color.WHITE);

            int w = getWidth();
            int h = getHeight();

            if (extraMode) {
                // === Режим дополнительного задания ===
                renderExtraMode(g, w, h);
            } else {
                // === Стандартный режим ===
                renderStandardMode(g, w, h);
            }
        }

        private void renderStandardMode(Graphics g, int w, int h) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = img.getRaster();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    raster.setSample(x, y, 0, 255);
                }
            }

            Box box = new Box(100, 150, 80);

            Point3D rotationAxis = new Point3D(axisX, axisY, axisZ);
            double[][] rotationMatrix = createRotationMatrix(angle, rotationAxis);

            double scale = 2.5;

            if (showParallel) {
                Point3D[] transformedVertices = box.transform(rotationMatrix);
                int offsetX = w / 4;
                int offsetY = h / 2;
                drawBox(img, transformedVertices, box, false, 0, removeHiddenLines, offsetX, offsetY, scale);

                g.setColor(Color.BLACK);
                g.setFont(new Font("Monospaced", Font.BOLD, 14));
                g.drawString("Parallel Projection", offsetX - 80, 30);
            }

            if (showPerspective) {
                double[][] translationMatrix = createTranslationMatrix(0, 0, 50);
                double[][] combinedMatrix = multiplyMatrices(translationMatrix, rotationMatrix);

                Point3D[] perspectiveVertices = new Point3D[box.vertices.length];
                for (int i = 0; i < box.vertices.length; i++) {
                    perspectiveVertices[i] = transformPoint(box.vertices[i], combinedMatrix);
                }

                int offsetX = 3 * w / 4;
                int offsetY = h / 2;
                drawBox(img, perspectiveVertices, box, true, PERSPECTIVE_K, removeHiddenLines, offsetX, offsetY, scale);

                g.setColor(Color.BLACK);
                g.setFont(new Font("Monospaced", Font.BOLD, 14));
                g.drawString("Perspective Projection (k=" + (int)PERSPECTIVE_K + ")", offsetX - 120, 30);
            }

            g.drawImage(img, 0, 0, null);

            // Инструкции
            g.setColor(Color.BLACK);
            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            int infoY = h - 120;
            g.drawString("Controls:", 20, infoY);
            g.drawString("  SPACE - Toggle animation: " + (animate ? "ON" : "OFF"), 20, infoY + 20);
            g.drawString("  H - Toggle hidden line removal: " + (removeHiddenLines ? "ON" : "OFF"), 20, infoY + 40);
            g.drawString("  R - Reset rotation", 20, infoY + 60);
            g.drawString("  LEFT/RIGHT - Manual rotation", 20, infoY + 80);
            g.drawString("  E - Switch to EXTRA MODE (two cuboids)", 20, infoY + 100);

            g.drawString(String.format("Rotation angle: %.2f rad", angle), w - 250, infoY);
            g.drawString(String.format("Rotation axis: [%.2f, %.2f, %.2f]", axisX, axisY, axisZ), w - 250, infoY + 20);
        }

        private void renderExtraMode(Graphics g, int w, int h) {
            // Создаём RGB буфер и Z-буфер
            int[] pixels = new int[w * h];
            double[] zBuffer = new double[w * h];

            // Инициализация буферов
            int bgColor = 0xFFE8E8F0;  // Светло-серый фон
            Arrays.fill(pixels, bgColor);
            Arrays.fill(zBuffer, Double.MAX_VALUE);

            double baseScale = 2.0;
            int centerX = w / 2;
            int centerY = h / 2;

            // Глубина для расчёта масштаба (расстояние от камеры)
            final double cameraDepth = 400.0;

            // Позиции кубоидов на орбите
            double x1 = ORBIT_RADIUS * Math.cos(orbitAngle);
            double z1 = ORBIT_RADIUS * Math.sin(orbitAngle);
            double x2 = ORBIT_RADIUS * Math.cos(orbitAngle + Math.PI);
            double z2 = ORBIT_RADIUS * Math.sin(orbitAngle + Math.PI);

            // Масштабирование на основе Z-позиции (ближе = больше, дальше = меньше)
            // z < 0 означает ближе к камере, z > 0 - дальше
            double scale1 = baseScale * (cameraDepth / (cameraDepth + z1));
            double scale2 = baseScale * (cameraDepth / (cameraDepth + z2));

            // Трансформации для кубоида 1
            double[][] rot1 = createRotationMatrix(rotAngle1, axis1);
            double[][] trans1 = createTranslationMatrix(x1, 0, z1);
            double[][] combined1 = multiplyMatrices(trans1, rot1);
            Point3D[] verts1 = new Point3D[box1.vertices.length];
            for (int i = 0; i < box1.vertices.length; i++) {
                verts1[i] = transformPoint(box1.vertices[i], combined1);
            }

            // Трансформации для кубоида 2
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

            // Рендерим кубоиды в порядке от дальнего к ближнему (painter's algorithm + Z-buffer)
            // Это обеспечивает правильное заслонение
            if (z1 > z2) {
                // Кубоид 1 дальше - рисуем его первым
                renderColoredBox(box1, verts1, projXZ, pixels, zBuffer, w, h, centerX, centerY, scale1, showEdges);
                renderColoredBox(box2, verts2, projYZ, pixels, zBuffer, w, h, centerX, centerY, scale2, showEdges);
            } else {
                // Кубоид 2 дальше - рисуем его первым
                renderColoredBox(box2, verts2, projYZ, pixels, zBuffer, w, h, centerX, centerY, scale2, showEdges);
                renderColoredBox(box1, verts1, projXZ, pixels, zBuffer, w, h, centerX, centerY, scale1, showEdges);
            }

            // Создаём изображение из буфера
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, w, h, pixels, 0, w);
            g.drawImage(img, 0, 0, null);

            // Заголовок и информация
            g.setColor(Color.BLACK);
            g.setFont(new Font("Monospaced", Font.BOLD, 16));
            g.drawString("EXTRA MODE: Two Cuboids with Two-Point Perspective", 20, 30);

            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g.drawString("Cuboid 1 (warm colors): Two-point perspective on XZ plane", 20, 55);
            g.drawString("Cuboid 2 (cool colors): Two-point perspective on YZ plane", 20, 75);

            // Инструкции
            int infoY = h - 140;
            g.drawString("Controls:", 20, infoY);
            g.drawString("  SPACE - Toggle animation: " + (animate ? "ON" : "OFF"), 20, infoY + 20);
            g.drawString("  E - Switch to STANDARD MODE", 20, infoY + 40);
            g.drawString("  D - Toggle edges: " + (showEdges ? "ON" : "OFF"), 20, infoY + 60);
            g.drawString("  R - Reset all rotations", 20, infoY + 80);

            // Параметры (вычисляем текущие масштабы для отображения)
            double dispZ1 = ORBIT_RADIUS * Math.sin(orbitAngle);
            double dispZ2 = ORBIT_RADIUS * Math.sin(orbitAngle + Math.PI);
            double dispScale1 = 2.0 * (cameraDepth / (cameraDepth + dispZ1));
            double dispScale2 = 2.0 * (cameraDepth / (cameraDepth + dispZ2));

            g.drawString(String.format("Orbit angle: %.2f rad", orbitAngle), w - 280, infoY);
            g.drawString(String.format("Cuboid 1: z=%.0f, scale=%.2f", dispZ1, dispScale1), w - 280, infoY + 20);
            g.drawString(String.format("Cuboid 2: z=%.0f, scale=%.2f", dispZ2, dispScale2), w - 280, infoY + 40);
            g.drawString(String.format("Cuboid 1 axis: [%.1f, %.1f, %.1f]", axis1.x, axis1.y, axis1.z), w - 280, infoY + 60);
            g.drawString(String.format("Cuboid 2 axis: [%.1f, %.1f, %.1f]", axis2.x, axis2.y, axis2.z), w - 280, infoY + 80);

            // Легенда цветов
            int legendX = w - 150;
            int legendY = 100;
            g.setFont(new Font("Monospaced", Font.BOLD, 11));
            g.drawString("Cuboid 1:", legendX, legendY);
            for (int i = 0; i < 6; i++) {
                g.setColor(box1.faceColors[i]);
                g.fillRect(legendX, legendY + 5 + i * 15, 20, 12);
                g.setColor(Color.BLACK);
                g.drawRect(legendX, legendY + 5 + i * 15, 20, 12);
            }

            g.drawString("Cuboid 2:", legendX + 50, legendY);
            for (int i = 0; i < 6; i++) {
                g.setColor(box2.faceColors[i]);
                g.fillRect(legendX + 50, legendY + 5 + i * 15, 20, 12);
                g.setColor(Color.BLACK);
                g.drawRect(legendX + 50, legendY + 5 + i * 15, 20, 12);
            }
        }
    }

    public static boolean saveImage(BufferedImage img, String filepath) {
        try {
            File outputFile = new File(filepath);
            outputFile.getParentFile().mkdirs();
            ImageIO.write(img, "png", outputFile);
            System.out.println("   ✓ Saved: " + filepath);
            return true;
        } catch (IOException e) {
            System.err.println("Error saving image: " + e.getMessage());
            return false;
        }
    }

    public static void createDemoImages() {
        System.out.println("Creating demonstration images...\n");
        System.out.println("Все преобразования выполняются через матрицы в однородных координатах 4x4.\n");

        Box box = new Box(100, 150, 80);

        // Разные углы вращения для демонстрации
        double[] angles = {0, Math.PI / 6, Math.PI / 4, Math.PI / 3};
        String[] angleNames = {"00", "30", "45", "60"};

        Point3D axis = new Point3D(1, 1, 1).normalize();

        int imgWidth = 600;
        int imgHeight = 500;
        int offsetX = imgWidth / 2;
        int offsetY = imgHeight / 2;
        double scale = 2.5;

        // Матрица переноса для перспективы (положительное z = ближе к наблюдателю)
        double[][] translationMatrix = createTranslationMatrix(0, 0, 10);

        for (int i = 0; i < angles.length; i++) {
            double angle = angles[i];
            String angleName = angleNames[i];

            // Матрица вращения
            double[][] rotationMatrix = createRotationMatrix(angle, axis);

            // Параллельная проекция (только вращение)
            Point3D[] transformedParallel = box.transform(rotationMatrix);

            BufferedImage parallelImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster1 = parallelImg.getRaster();
            for (int y = 0; y < imgHeight; y++) {
                for (int x = 0; x < imgWidth; x++) {
                    raster1.setSample(x, y, 0, 255);
                }
            }
            drawBox(parallelImg, transformedParallel, box, false, 0, false, offsetX, offsetY, scale);
            saveImage(parallelImg, "res/lab5_parallel_" + angleName + ".png");

            // Перспективная проекция (вращение + перенос)
            double[][] combinedMatrix = multiplyMatrices(translationMatrix, rotationMatrix);
            Point3D[] transformedPerspective = new Point3D[box.vertices.length];
            for (int j = 0; j < box.vertices.length; j++) {
                transformedPerspective[j] = transformPoint(box.vertices[j], combinedMatrix);
            }

            BufferedImage perspectiveImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster2 = perspectiveImg.getRaster();
            for (int y = 0; y < imgHeight; y++) {
                for (int x = 0; x < imgWidth; x++) {
                    raster2.setSample(x, y, 0, 255);
                }
            }
            drawBox(perspectiveImg, transformedPerspective, box, true, PERSPECTIVE_K, false, offsetX, offsetY, scale);
            saveImage(perspectiveImg, "res/lab5_perspective_" + angleName + ".png");
        }

        // Сравнение с удалением невидимых линий и без
        // Угол и ось подобраны так, чтобы задняя грань была полностью скрыта передней
        double angle = Math.PI / 5;
        Point3D comparisonAxis = new Point3D(1, 0.3, 0.2).normalize();
        double[][] rotationMatrix = createRotationMatrix(angle, comparisonAxis);
        double[][] combinedMatrix = multiplyMatrices(translationMatrix, rotationMatrix);

        Point3D[] transformedPerspective = new Point3D[box.vertices.length];
        for (int j = 0; j < box.vertices.length; j++) {
            transformedPerspective[j] = transformPoint(box.vertices[j], combinedMatrix);
        }

        // Без удаления
        BufferedImage noRemoval = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster3 = noRemoval.getRaster();
        for (int y = 0; y < imgHeight; y++) {
            for (int x = 0; x < imgWidth; x++) {
                raster3.setSample(x, y, 0, 255);
            }
        }
        drawBox(noRemoval, transformedPerspective, box, true, PERSPECTIVE_K, false, offsetX, offsetY, scale);
        saveImage(noRemoval, "res/lab5_no_removal.png");

        // С удалением
        BufferedImage withRemoval = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster4 = withRemoval.getRaster();
        for (int y = 0; y < imgHeight; y++) {
            for (int x = 0; x < imgWidth; x++) {
                raster4.setSample(x, y, 0, 255);
            }
        }
        drawBox(withRemoval, transformedPerspective, box, true, PERSPECTIVE_K, true, offsetX, offsetY, scale);
        saveImage(withRemoval, "res/lab5_with_removal.png");

        System.out.println("\nDemo images created successfully!");
    }

    public static void main(String[] args) {
        System.out.println("=== Lab 5: 3D Projections and Animation ===\n");

        createDemoImages();

        System.out.println("\n=== Launching GUI ===\n");

        SwingUtilities.invokeLater(() -> {
            Lab5 lab = new Lab5();
            lab.setVisible(true);
        });
    }
}