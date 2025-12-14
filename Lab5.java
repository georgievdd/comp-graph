import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    // Параметры проекции
    private static final double PERSPECTIVE_K = 500.0; // Расстояние до центра проекции

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
     * Параллельная проекция на плоскость Z=0
     */
    public static Point2D parallelProjection(Point3D p) {
        return new Point2D(p.x, p.y);
    }

    /**
     * Перспективная проекция с центром в (0, 0, k)
     */
    public static Point2D perspectiveProjection(Point3D p, double k) {
        // Проекция точки (x, y, z) на плоскость Z=0
        // Центр проекции: (0, 0, k)
        if (Math.abs(k - p.z) < 0.001) {
            // Точка лежит в плоскости центра проекции
            return new Point2D(p.x * 1000, p.y * 1000);
        }

        double factor = k / (k - p.z);
        return new Point2D(p.x * factor, p.y * factor);
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
                }
            }
        });

        setFocusable(true);
    }

    private void startAnimation(DemoPanel panel) {
        animationTimer = new Timer(16, e -> {
            if (animate) {
                angle += 0.02;
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

            Point3D[] transformedVertices = box.transform(rotationMatrix);

            // Параметры отрисовки
            double scale = 2.5;

            if (showParallel) {
                int offsetX = w / 4;
                int offsetY = h / 2;
                drawBox(img, transformedVertices, box, false, 0, removeHiddenLines, offsetX, offsetY, scale);

                // Подпись
                g.setColor(Color.BLACK);
                g.setFont(new Font("Monospaced", Font.BOLD, 14));
                g.drawString("Parallel Projection", offsetX - 80, 30);
            }

            if (showPerspective) {
                int offsetX = 3 * w / 4;
                int offsetY = h / 2;
                drawBox(img, transformedVertices, box, true, PERSPECTIVE_K, removeHiddenLines, offsetX, offsetY, scale);

                // Подпись
                g.setColor(Color.BLACK);
                g.setFont(new Font("Monospaced", Font.BOLD, 14));
                g.drawString("Perspective Projection (k=" + (int)PERSPECTIVE_K + ")", offsetX - 120, 30);
            }

            g.drawImage(img, 0, 0, null);

            g.setColor(Color.BLACK);
            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            int infoY = h - 100;
            g.drawString("Controls:", 20, infoY);
            g.drawString("  SPACE - Toggle animation: " + (animate ? "ON" : "OFF"), 20, infoY + 20);
            g.drawString("  H - Toggle hidden line removal: " + (removeHiddenLines ? "ON" : "OFF"), 20, infoY + 40);
            g.drawString("  R - Reset rotation", 20, infoY + 60);
            g.drawString("  LEFT/RIGHT - Manual rotation", 20, infoY + 80);

            g.drawString(String.format("Rotation angle: %.2f rad", angle), w - 250, infoY);
            g.drawString(String.format("Rotation axis: [%.2f, %.2f, %.2f]", axisX, axisY, axisZ), w - 250, infoY + 20);
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

        for (int i = 0; i < angles.length; i++) {
            double angle = angles[i];
            String angleName = angleNames[i];

            // Матрица вращения
            double[][] rotationMatrix = createRotationMatrix(angle, axis);
            Point3D[] transformed = box.transform(rotationMatrix);

            // Параллельная проекция
            BufferedImage parallelImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster1 = parallelImg.getRaster();
            for (int y = 0; y < imgHeight; y++) {
                for (int x = 0; x < imgWidth; x++) {
                    raster1.setSample(x, y, 0, 255);
                }
            }
            drawBox(parallelImg, transformed, box, false, 0, true, offsetX, offsetY, scale);
            saveImage(parallelImg, "res/lab5_parallel_" + angleName + ".png");

            // Перспективная проекция
            BufferedImage perspectiveImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster2 = perspectiveImg.getRaster();
            for (int y = 0; y < imgHeight; y++) {
                for (int x = 0; x < imgWidth; x++) {
                    raster2.setSample(x, y, 0, 255);
                }
            }
            drawBox(perspectiveImg, transformed, box, true, PERSPECTIVE_K, true, offsetX, offsetY, scale);
            saveImage(perspectiveImg, "res/lab5_perspective_" + angleName + ".png");
        }

        // Сравнение с удалением невидимых линий и без
        double angle = Math.PI / 4;
        double[][] rotationMatrix = createRotationMatrix(angle, axis);
        Point3D[] transformed = box.transform(rotationMatrix);

        // Без удаления
        BufferedImage noRemoval = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster3 = noRemoval.getRaster();
        for (int y = 0; y < imgHeight; y++) {
            for (int x = 0; x < imgWidth; x++) {
                raster3.setSample(x, y, 0, 255);
            }
        }
        drawBox(noRemoval, transformed, box, true, PERSPECTIVE_K, false, offsetX, offsetY, scale);
        saveImage(noRemoval, "res/lab5_no_removal.png");

        // С удалением
        BufferedImage withRemoval = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster4 = withRemoval.getRaster();
        for (int y = 0; y < imgHeight; y++) {
            for (int x = 0; x < imgWidth; x++) {
                raster4.setSample(x, y, 0, 255);
            }
        }
        drawBox(withRemoval, transformed, box, true, PERSPECTIVE_K, true, offsetX, offsetY, scale);
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