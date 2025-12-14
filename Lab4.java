import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Lab4 extends JFrame {
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 1000;

    static class Point {
        double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return String.format("(%.1f, %.1f)", x, y);
        }
    }

    static class Polygon {
        List<Point> vertices;
        String name;

        public Polygon(String name) {
            this.name = name;
            this.vertices = new ArrayList<>();
        }

        public void addVertex(double x, double y) {
            vertices.add(new Point(x, y));
        }
    }

    public Lab4() {
        setTitle("Lab 4: Bezier Curves and Cyrus-Beck Line Clipping");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        add(new DemoPanel());
    }

    /**
     * Алгоритм Брезенхема для рисования линии
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
     * Рисование пунктирной линии
     */
    public static void drawDashedLine(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
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
        int count = 0;

        while (true) {
            if (count % 8 < 4) {  // Пунктир: 4 пикселя рисуем, 4 пропускаем
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    raster.setSample(x, y, 0, color);
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
            count++;
        }
    }

    /**
     * Рисование толстой линии
     */
    public static void drawThickLine(BufferedImage img, int x0, int y0, int x1, int y1, int color, int thickness) {
        for (int dy = -thickness / 2; dy <= thickness / 2; dy++) {
            for (int dx = -thickness / 2; dx <= thickness / 2; dx++) {
                drawLine(img, x0 + dx, y0 + dy, x1 + dx, y1 + dy, color);
            }
        }
    }

    /**
     * Рисование точки (маленького круга)
     */
    public static void drawPoint(BufferedImage img, int x, int y, int color, int size) {
        WritableRaster raster = img.getRaster();
        int width = img.getWidth();
        int height = img.getHeight();

        for (int dy = -size; dy <= size; dy++) {
            for (int dx = -size; dx <= size; dx++) {
                if (dx * dx + dy * dy <= size * size) {
                    int px = x + dx;
                    int py = y + dy;
                    if (px >= 0 && px < width && py >= 0 && py < height) {
                        raster.setSample(px, py, 0, color);
                    }
                }
            }
        }
    }

    /**
     * Построение кубической кривой Безье
     */
    public static void drawBezierCubic(BufferedImage img, Point p0, Point p1, Point p2, Point p3, int color, int steps) {
        double dt = 1.0 / steps;
        Point prev = p0;

        for (int i = 1; i <= steps; i++) {
            double t = i * dt;
            double t2 = t * t;
            double t3 = t2 * t;
            double mt = 1 - t;
            double mt2 = mt * mt;
            double mt3 = mt2 * mt;

            double x = mt3 * p0.x + 3 * mt2 * t * p1.x + 3 * mt * t2 * p2.x + t3 * p3.x;
            double y = mt3 * p0.y + 3 * mt2 * t * p1.y + 3 * mt * t2 * p2.y + t3 * p3.y;

            Point current = new Point(x, y);
            drawLine(img, (int) Math.round(prev.x), (int) Math.round(prev.y),
                    (int) Math.round(current.x), (int) Math.round(current.y), color);
            prev = current;
        }
    }

    /**
     * Рисование полигона
     */
    public static void drawPolygon(BufferedImage img, Polygon polygon, int color) {
        int n = polygon.vertices.size();
        for (int i = 0; i < n; i++) {
            Point p1 = polygon.vertices.get(i);
            Point p2 = polygon.vertices.get((i + 1) % n);
            drawLine(img, (int) Math.round(p1.x), (int) Math.round(p1.y),
                    (int) Math.round(p2.x), (int) Math.round(p2.y), color);
        }
    }

    private static class Edge {
        Point p1, p2;
        Point normal;

        Edge(Point p1, Point p2) {
            this.p1 = p1;
            this.p2 = p2;
            double dx = p2.x - p1.x;
            double dy = p2.y - p1.y;
            this.normal = new Point(dy, -dx);
            double len = Math.sqrt(normal.x * normal.x + normal.y * normal.y);
            if (len > 0) {
                normal.x /= len;
                normal.y /= len;
            }
        }
    }

    private static boolean isConvex(Polygon polygon) {
        int n = polygon.vertices.size();
        if (n < 3) return false;

        boolean hasPositive = false;
        boolean hasNegative = false;

        for (int i = 0; i < n; i++) {
            Point p1 = polygon.vertices.get(i);
            Point p2 = polygon.vertices.get((i + 1) % n);
            Point p3 = polygon.vertices.get((i + 2) % n);

            double cross = (p2.x - p1.x) * (p3.y - p2.y) - (p2.y - p1.y) * (p3.x - p2.x);

            if (cross > 0) hasPositive = true;
            if (cross < 0) hasNegative = true;
        }

        return !(hasPositive && hasNegative);
    }

    /**
     * Алгоритм отсечения Кируса-Бека
     * Возвращает null если отрезок полностью снаружи, иначе массив из двух точек
     */
    public static Point[] cyrusBeckClip(Point p1, Point p2, Polygon clipPolygon) {
        if (!isConvex(clipPolygon)) {
            System.err.println("ОШИБКА: Полигон не является выпуклым!");
            return null;
        }

        double tMin = 0.0;
        double tMax = 1.0;

        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;

        int n = clipPolygon.vertices.size();
        for (int i = 0; i < n; i++) {
            Point v1 = clipPolygon.vertices.get(i);
            Point v2 = clipPolygon.vertices.get((i + 1) % n);

            Edge edge = new Edge(v1, v2);

            // Вектор от начала ребра до начала отрезка
            double wx = p1.x - edge.p1.x;
            double wy = p1.y - edge.p1.y;

            double numerator = -(edge.normal.x * wx + edge.normal.y * wy);
            double denominator = edge.normal.x * dx + edge.normal.y * dy;

            if (Math.abs(denominator) < 1e-10) {
                // Отрезок параллелен ребру
                if (numerator < 0) {
                    // Отрезок полностью снаружи
                    return null;
                }
            } else {
                double t = numerator / denominator;

                if (denominator < 0) {
                    // Входящее ребро
                    tMin = Math.max(tMin, t);
                } else {
                    // Выходящее ребро
                    tMax = Math.min(tMax, t);
                }

                if (tMin > tMax) {
                    // Отрезок полностью снаружи
                    return null;
                }
            }
        }

        Point clippedP1 = new Point(p1.x + tMin * dx, p1.y + tMin * dy);
        Point clippedP2 = new Point(p1.x + tMax * dx, p1.y + tMax * dy);

        return new Point[]{clippedP1, clippedP2};
    }

    public static boolean saveImage(BufferedImage img, String filepath) {
        try {
            File outputFile = new File(filepath);
            ImageIO.write(img, "png", outputFile);
            System.out.println("   ✓ Сохранено: " + filepath);
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении: " + e.getMessage());
            return false;
        }
    }

    private static BufferedImage createEmptyImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = img.getRaster();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                raster.setSample(x, y, 0, 255);  // Белый фон
            }
        }
        return img;
    }

    private class DemoPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBackground(Color.WHITE);

            int cols = 4;
            int rows = 2;
            int cellWidth = getWidth() / cols;
            int cellHeight = getHeight() / rows;

            // Демонстрация кривых Безье
            drawBezierDemo(g, 20, 20, cellWidth - 40, cellHeight - 40, "1. Simple Cubic Bezier");
            drawBezierDemo2(g, cellWidth + 20, 20, cellWidth - 40, cellHeight - 40, "2. S-Curve Bezier");
            drawBezierDemo3(g, 2 * cellWidth + 20, 20, cellWidth - 40, cellHeight - 40, "3. Loop Bezier");
            drawBezierDemo4(g, 3 * cellWidth + 20, 20, cellWidth - 40, cellHeight - 40, "4. Complex Bezier");

            // Демонстрация отсечения Кируса-Бека
            int offsetY = cellHeight + 20;
            g.setColor(Color.BLACK);
            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            g.drawString("Алгоритм отсечения Кируса-Бека (см. сохраненные PNG файлы для детальных тестов)", 20, offsetY - 5);
        }

        private void drawBezierDemo(Graphics g, int offsetX, int offsetY, int w, int h, String title) {
            BufferedImage img = createEmptyImage(w, h);

            Point p0 = new Point(50, h - 50);
            Point p1 = new Point(100, 50);
            Point p2 = new Point(w - 100, 50);
            Point p3 = new Point(w - 50, h - 50);

            drawBezierCubic(img, p0, p1, p2, p3, 0, 100);
            drawDashedLine(img, (int) p0.x, (int) p0.y, (int) p1.x, (int) p1.y, 150);
            drawDashedLine(img, (int) p2.x, (int) p2.y, (int) p3.x, (int) p3.y, 150);

            g.drawImage(img, offsetX, offsetY, null);
            g.setColor(Color.BLACK);
            g.drawString(title, offsetX, offsetY - 5);
        }

        private void drawBezierDemo2(Graphics g, int offsetX, int offsetY, int w, int h, String title) {
            BufferedImage img = createEmptyImage(w, h);

            Point p0 = new Point(50, h / 2);
            Point p1 = new Point(w / 3, 50);
            Point p2 = new Point(2 * w / 3, h - 50);
            Point p3 = new Point(w - 50, h / 2);

            drawBezierCubic(img, p0, p1, p2, p3, 0, 100);
            drawDashedLine(img, (int) p0.x, (int) p0.y, (int) p1.x, (int) p1.y, 150);
            drawDashedLine(img, (int) p2.x, (int) p2.y, (int) p3.x, (int) p3.y, 150);

            g.drawImage(img, offsetX, offsetY, null);
            g.setColor(Color.BLACK);
            g.drawString(title, offsetX, offsetY - 5);
        }

        private void drawBezierDemo3(Graphics g, int offsetX, int offsetY, int w, int h, String title) {
            BufferedImage img = createEmptyImage(w, h);

            Point p0 = new Point(w / 2, h - 50);
            Point p1 = new Point(w - 50, h - 100);
            Point p2 = new Point(50, 100);
            Point p3 = new Point(w / 2, 50);

            drawBezierCubic(img, p0, p1, p2, p3, 0, 100);
            drawDashedLine(img, (int) p0.x, (int) p0.y, (int) p1.x, (int) p1.y, 150);
            drawDashedLine(img, (int) p2.x, (int) p2.y, (int) p3.x, (int) p3.y, 150);

            g.drawImage(img, offsetX, offsetY, null);
            g.setColor(Color.BLACK);
            g.drawString(title, offsetX, offsetY - 5);
        }

        private void drawBezierDemo4(Graphics g, int offsetX, int offsetY, int w, int h, String title) {
            BufferedImage img = createEmptyImage(w, h);

            Point p0 = new Point(50, h - 50);
            Point p1 = new Point(w - 50, h - 50);
            Point p2 = new Point(50, 50);
            Point p3 = new Point(w - 50, 50);

            drawBezierCubic(img, p0, p1, p2, p3, 0, 100);
            drawDashedLine(img, (int) p0.x, (int) p0.y, (int) p1.x, (int) p1.y, 150);
            drawDashedLine(img, (int) p2.x, (int) p2.y, (int) p3.x, (int) p3.y, 150);

            g.drawImage(img, offsetX, offsetY, null);
            g.setColor(Color.BLACK);
            g.drawString(title, offsetX, offsetY - 5);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Лабораторная работа №4: Кривые Безье, отсечение отрезков ===\n");

        System.out.println("ЧАСТЬ 1: Кривые Безье третьего порядка\n");

        BufferedImage bezier1 = createEmptyImage(500, 350);
        Point p0 = new Point(50, 300);
        Point p1 = new Point(150, 50);
        Point p2 = new Point(350, 50);
        Point p3 = new Point(450, 300);
        drawBezierCubic(bezier1, p0, p1, p2, p3, 0, 100);
        drawDashedLine(bezier1, (int) p0.x, (int) p0.y, (int) p1.x, (int) p1.y, 150);
        drawDashedLine(bezier1, (int) p2.x, (int) p2.y, (int) p3.x, (int) p3.y, 150);
        drawPoint(bezier1, (int) p0.x, (int) p0.y, 100, 4);
        drawPoint(bezier1, (int) p3.x, (int) p3.y, 100, 4);
        saveImage(bezier1, "res/bezier_1_simple.png");

        // Пример 2: S-образная кривая
        BufferedImage bezier2 = createEmptyImage(500, 350);
        drawBezierCubic(bezier2, new Point(50, 175), new Point(150, 50),
                       new Point(350, 300), new Point(450, 175), 0, 100);
        saveImage(bezier2, "res/bezier_2_s_curve.png");

        // Пример 3: Кривая с петлей
        BufferedImage bezier3 = createEmptyImage(500, 350);
        drawBezierCubic(bezier3, new Point(250, 300), new Point(450, 250),
                       new Point(50, 100), new Point(250, 50), 0, 100);
        saveImage(bezier3, "res/bezier_3_loop.png");

        System.out.println();

        // ========== ЧАСТЬ 2: ОТСЕЧЕНИЕ КИРУСА-БЕКА ==========
        System.out.println("ЧАСТЬ 2: Алгоритм отсечения Кируса-Бека\n");

        int imgSize = 500;

        // ===== ТЕСТ 1: Треугольник - различные случаи отсечения =====
        System.out.println("Тест 1: Отсечение треугольником");
        Polygon triangle = new Polygon("Triangle");
        triangle.addVertex(250, 100);
        triangle.addVertex(400, 350);
        triangle.addVertex(100, 350);

        BufferedImage test1 = createEmptyImage(imgSize, 400);
        drawPolygon(test1, triangle, 0);

        // Линия полностью внутри
        testClipping(test1, new Point(200, 250), new Point(300, 250), triangle, "Внутри");
        // Линия полностью снаружи
        testClipping(test1, new Point(50, 50), new Point(150, 50), triangle, "Снаружи");
        // Линия входит и выходит
        testClipping(test1, new Point(50, 250), new Point(450, 250), triangle, "Пересекает");
        // Вертикальная линия
        testClipping(test1, new Point(250, 50), new Point(250, 380), triangle, "Вертикаль");
        // Диагональ
        testClipping(test1, new Point(50, 50), new Point(450, 380), triangle, "Диагональ");

        saveImage(test1, "res/clip_01_triangle.png");

        System.out.println("Тест 2: Отсечение квадратом (гориз./верт. линии)");
        Polygon square = new Polygon("Square");
        square.addVertex(150, 150);
        square.addVertex(350, 150);
        square.addVertex(350, 350);
        square.addVertex(150, 350);

        BufferedImage test2 = createEmptyImage(imgSize, imgSize);
        drawPolygon(test2, square, 0);

        // Горизонтальные линии
        testClipping(test2, new Point(50, 250), new Point(450, 250), square, "Горизонталь через");
        testClipping(test2, new Point(200, 200), new Point(300, 200), square, "Горизонталь внутри");
        testClipping(test2, new Point(50, 100), new Point(450, 100), square, "Горизонталь выше");

        // Вертикальные линии
        testClipping(test2, new Point(250, 50), new Point(250, 450), square, "Вертикаль через");
        testClipping(test2, new Point(200, 200), new Point(200, 300), square, "Вертикаль внутри");
        testClipping(test2, new Point(100, 50), new Point(100, 450), square, "Вертикаль слева");

        saveImage(test2, "res/clip_02_square_hv_lines.png");

        System.out.println("Тест 3: Отсечение квадратом (диагонали)");
        BufferedImage test3 = createEmptyImage(imgSize, imgSize);
        drawPolygon(test3, square, 0);

        // Различные диагонали
        testClipping(test3, new Point(50, 50), new Point(450, 450), square, "Диаг. \\");
        testClipping(test3, new Point(450, 50), new Point(50, 450), square, "Диаг. /");
        testClipping(test3, new Point(100, 100), new Point(400, 400), square, "Диаг. \\ через");
        testClipping(test3, new Point(200, 200), new Point(300, 300), square, "Диаг. внутри");
        testClipping(test3, new Point(50, 250), new Point(450, 150), square, "Наклон 1");
        testClipping(test3, new Point(50, 150), new Point(450, 350), square, "Наклон 2");

        saveImage(test3, "res/clip_03_square_diagonals.png");

        // ===== ТЕСТ 4: Пятиугольник - лучевые линии из центра =====
        System.out.println("Тест 4: Отсечение пятиугольником (лучи из центра)");
        Polygon pentagon = new Polygon("Pentagon");
        int cx = 250, cy = 250, radius = 180;
        for (int i = 0; i < 5; i++) {
            double angle = -Math.PI / 2 + i * 2 * Math.PI / 5;
            pentagon.addVertex(cx + radius * Math.cos(angle), cy + radius * Math.sin(angle));
        }

        BufferedImage test4 = createEmptyImage(imgSize, imgSize);
        drawPolygon(test4, pentagon, 0);

        // Лучи из центра в разных направлениях
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8;
            int x = (int) (cx + 240 * Math.cos(angle));
            int y = (int) (cy + 240 * Math.sin(angle));
            testClipping(test4, new Point(cx, cy), new Point(x, y), pentagon, "Луч " + i);
        }

        saveImage(test4, "res/clip_04_pentagon_rays.png");

        // ===== ТЕСТ 5: Пятиугольник - параллельные линии =====
        System.out.println("Тест 5: Отсечение пятиугольником (параллели)");
        BufferedImage test5 = createEmptyImage(imgSize, imgSize);
        drawPolygon(test5, pentagon, 0);

        // Горизонтальные параллельные линии
        for (int y = 80; y <= 420; y += 40) {
            testClipping(test5, new Point(50, y), new Point(450, y), pentagon, "y=" + y);
        }

        saveImage(test5, "res/clip_05_pentagon_parallel.png");

        // ===== ТЕСТ 6: Шестиугольник - окружности =====
        System.out.println("Тест 6: Отсечение шестиугольником");
        Polygon hexagon = new Polygon("Hexagon");
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3;
            hexagon.addVertex(cx + 160 * Math.cos(angle), cy + 160 * Math.sin(angle));
        }

        BufferedImage test6 = createEmptyImage(imgSize, imgSize);
        drawPolygon(test6, hexagon, 0);

        // Линии по разным углам
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            int x1 = (int) (cx + 220 * Math.cos(angle));
            int y1 = (int) (cy + 220 * Math.sin(angle));
            int x2 = (int) (cx + 220 * Math.cos(angle + Math.PI));
            int y2 = (int) (cy + 220 * Math.sin(angle + Math.PI));
            testClipping(test6, new Point(x1, y1), new Point(x2, y2), hexagon, "Хорда " + i);
        }

        saveImage(test6, "res/clip_06_hexagon.png");

        // ===== ТЕСТ 7: Прямоугольник (горизонтальный) - граничные случаи =====
        System.out.println("Тест 7: Граничные случаи");
        Polygon rect = new Polygon("Rectangle");
        rect.addVertex(100, 200);
        rect.addVertex(400, 200);
        rect.addVertex(400, 300);
        rect.addVertex(100, 300);

        BufferedImage test7 = createEmptyImage(imgSize, imgSize);
        drawPolygon(test7, rect, 0);

        // Линии касающиеся границ
        testClipping(test7, new Point(50, 200), new Point(450, 200), rect, "Верх граница");
        testClipping(test7, new Point(50, 300), new Point(450, 300), rect, "Низ граница");
        testClipping(test7, new Point(100, 150), new Point(100, 350), rect, "Лев граница");
        testClipping(test7, new Point(400, 150), new Point(400, 350), rect, "Прав граница");

        // Линии по углам
        testClipping(test7, new Point(50, 150), new Point(100, 200), rect, "К углу TL");
        testClipping(test7, new Point(450, 150), new Point(400, 200), rect, "К углу TR");
        testClipping(test7, new Point(50, 350), new Point(100, 300), rect, "К углу BL");
        testClipping(test7, new Point(450, 350), new Point(400, 300), rect, "К углу BR");

        saveImage(test7, "res/clip_07_boundaries.png");

        // ===== ТЕСТ 8: Треугольник наклонный - множественные пересечения =====
        System.out.println("Тест 8: Наклонный треугольник");
        Polygon triangle2 = new Polygon("Triangle2");
        triangle2.addVertex(100, 150);
        triangle2.addVertex(400, 100);
        triangle2.addVertex(250, 400);

        BufferedImage test8 = createEmptyImage(imgSize, imgSize);
        drawPolygon(test8, triangle2, 0);

        // Сетка линий
        for (int x = 50; x <= 450; x += 80) {
            testClipping(test8, new Point(x, 50), new Point(x, 450), triangle2, "x=" + x);
        }
        for (int y = 50; y <= 450; y += 80) {
            testClipping(test8, new Point(50, y), new Point(450, y), triangle2, "y=" + y);
        }

        saveImage(test8, "res/clip_08_tilted_triangle.png");

        // ===== ТЕСТ 9: Квадрат повернутый (ромб) =====
        System.out.println("Тест 9: Ромб (повернутый квадрат)");
        Polygon diamond = new Polygon("Diamond");
        diamond.addVertex(250, 80);
        diamond.addVertex(420, 250);
        diamond.addVertex(250, 420);
        diamond.addVertex(80, 250);

        BufferedImage test9 = createEmptyImage(imgSize, imgSize);
        drawPolygon(test9, diamond, 0);

        // Сетка горизонтальных и вертикальных линий
        for (int i = 100; i <= 400; i += 60) {
            testClipping(test9, new Point(i, 50), new Point(i, 450), diamond, "v" + i);
            testClipping(test9, new Point(50, i), new Point(450, i), diamond, "h" + i);
        }

        saveImage(test9, "res/clip_09_diamond.png");

        // ===== ТЕСТ 10: Пятиугольник - случайные линии =====
        System.out.println("Тест 10: Разнообразные случайные линии");
        BufferedImage test10 = createEmptyImage(imgSize, imgSize);
        drawPolygon(test10, pentagon, 0);

        // Различные тестовые случаи
        testClipping(test10, new Point(100, 100), new Point(400, 100), pentagon, "Top");
        testClipping(test10, new Point(100, 400), new Point(400, 400), pentagon, "Bottom");
        testClipping(test10, new Point(100, 100), new Point(100, 400), pentagon, "Left");
        testClipping(test10, new Point(400, 100), new Point(400, 400), pentagon, "Right");
        testClipping(test10, new Point(150, 150), new Point(350, 350), pentagon, "Diag1");
        testClipping(test10, new Point(350, 150), new Point(150, 350), pentagon, "Diag2");
        testClipping(test10, new Point(100, 250), new Point(400, 150), pentagon, "Slope1");
        testClipping(test10, new Point(100, 150), new Point(400, 350), pentagon, "Slope2");
        testClipping(test10, new Point(250, 100), new Point(350, 400), pentagon, "Vert-like");
        testClipping(test10, new Point(100, 200), new Point(400, 300), pentagon, "Slight");

        saveImage(test10, "res/clip_10_various.png");

        System.out.println("\nВсе тесты завершены! Создано 13 изображений.");
        System.out.println("\n=== Запуск GUI ===\n");

        SwingUtilities.invokeLater(() -> {
            Lab4 lab = new Lab4();
            lab.setVisible(true);
        });
    }

    private static void testClipping(BufferedImage img, Point p1, Point p2, Polygon clipPoly, String label) {
        // Рисуем исходную линию пунктиром (светло-серым)
        drawDashedLine(img, (int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y, 180);

        // Отсекаем
        Point[] clipped = cyrusBeckClip(p1, p2, clipPoly);

        if (clipped != null) {
            // Рисуем отсеченную часть толстой черной линией
            drawThickLine(img, (int) Math.round(clipped[0].x), (int) Math.round(clipped[0].y),
                         (int) Math.round(clipped[1].x), (int) Math.round(clipped[1].y), 0, 2);

            // Отмечаем точки входа/выхода
            drawPoint(img, (int) Math.round(clipped[0].x), (int) Math.round(clipped[0].y), 100, 3);
            drawPoint(img, (int) Math.round(clipped[1].x), (int) Math.round(clipped[1].y), 100, 3);

            System.out.println("   • " + label + ": " + p1 + " → " + p2 +
                             " ⇒ ОТСЕЧЕНО: " + clipped[0] + " → " + clipped[1]);
        } else {
            System.out.println("   • " + label + ": " + p1 + " → " + p2 + " ⇒ ПОЛНОСТЬЮ СНАРУЖИ");
        }
    }
}