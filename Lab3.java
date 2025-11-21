import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Лабораторная работа No 3: Построение и заполнение полигонов
 * 1. Вычерчивание отрезков прямых линий толщиной в 1 пиксел (алгоритм Брезенхема)
 * 2. Вывод на экран полигона
 * 3. Определение типа полигона: простой/сложный, выпуклый/невыпуклый
 * 4. Заполнение полигона по правилам even-odd и non-zero-winding
 */
public class Lab3 extends JFrame {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    private List<Polygon> testPolygons;

    /**
     * Класс для представления полигона
     */
    static class Polygon {
        List<Point> vertices;
        String name;

        public Polygon(String name) {
            this.name = name;
            this.vertices = new ArrayList<>();
        }

        public void addVertex(int x, int y) {
            vertices.add(new Point(x, y));
        }
    }

    public Lab3() {
        setTitle("Lab 3: Polygon Drawing and Filling");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createTestPolygons();
        add(new PolygonPanel());
    }

    /**
     * Создание тестовых полигонов для демонстрации
     */
    private void createTestPolygons() {
        testPolygons = new ArrayList<>();

        // 1. Простой выпуклый треугольник
        Polygon triangle = new Polygon("Triangle (Simple, Convex)");
        triangle.addVertex(50, 50);
        triangle.addVertex(150, 50);
        triangle.addVertex(100, 150);
        testPolygons.add(triangle);

        // 2. Простой невыпуклый полигон (звезда)
        Polygon star = new Polygon("Star (Simple, Non-convex)");
        int centerX = 100, centerY = 100;
        int outerRadius = 80, innerRadius = 35;
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + i * Math.PI / 5;
            int radius = (i % 2 == 0) ? outerRadius : innerRadius;
            int x = centerX + (int) (radius * Math.cos(angle));
            int y = centerY - (int) (radius * Math.sin(angle));
            star.addVertex(x, y);
        }
        testPolygons.add(star);

        // 3. Сложный полигон с самопересечениями (пентаграмма)
        Polygon pentagram = new Polygon("Pentagram (Complex, Self-intersecting)");
        centerX = 100;
        centerY = 100;
        int radius = 80;
        int[] order = {0, 2, 4, 1, 3}; // Порядок соединения вершин для создания пересечений
        for (int i : order) {
            double angle = Math.PI / 2 + i * 2 * Math.PI / 5;
            int x = centerX + (int) (radius * Math.cos(angle));
            int y = centerY - (int) (radius * Math.sin(angle));
            pentagram.addVertex(x, y);
        }
        testPolygons.add(pentagram);

        // 4. Простой выпуклый шестиугольник
        Polygon hexagon = new Polygon("Hexagon (Simple, Convex)");
        centerX = 100;
        centerY = 100;
        radius = 70;
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3;
            int x = centerX + (int) (radius * Math.cos(angle));
            int y = centerY + (int) (radius * Math.sin(angle));
            hexagon.addVertex(x, y);
        }
        testPolygons.add(hexagon);
    }

    /**
     * Алгоритм Брезенхема для рисования линии
     */
    public static void drawLine(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        WritableRaster raster = img.getRaster();
        int width = img.getWidth();
        int height = img.getHeight();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;

        while (true) {
            // Рисуем пиксель, если он в пределах изображения
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
     * Рисование полигона (только контур)
     */
    public static void drawPolygon(BufferedImage img, Polygon polygon, int color) {
        int n = polygon.vertices.size();
        for (int i = 0; i < n; i++) {
            Point p1 = polygon.vertices.get(i);
            Point p2 = polygon.vertices.get((i + 1) % n);
            drawLine(img, p1.x, p1.y, p2.x, p2.y, color);
        }
    }

    /**
     * Проверка, является ли полигон простым (без самопересечений)
     */
    public static boolean isSimplePolygon(Polygon polygon) {
        int n = polygon.vertices.size();

        // Проверяем каждую пару несмежных ребер на пересечение
        for (int i = 0; i < n; i++) {
            Point p1 = polygon.vertices.get(i);
            Point p2 = polygon.vertices.get((i + 1) % n);

            for (int j = i + 2; j < n; j++) {
                // Не проверяем смежные ребра
                if (j == (i + n - 1) % n) continue;

                Point p3 = polygon.vertices.get(j);
                Point p4 = polygon.vertices.get((j + 1) % n);

                if (segmentsIntersect(p1, p2, p3, p4)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Проверка пересечения двух отрезков
     */
    private static boolean segmentsIntersect(Point p1, Point p2, Point p3, Point p4) {
        double d1 = direction(p3, p4, p1);
        double d2 = direction(p3, p4, p2);
        double d3 = direction(p1, p2, p3);
        double d4 = direction(p1, p2, p4);

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
            ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }

        // Проверка на коллинеарность и наложение (исключаем общие вершины)
        if (d1 == 0 && onSegment(p3, p4, p1) && !p1.equals(p3) && !p1.equals(p4)) return true;
        if (d2 == 0 && onSegment(p3, p4, p2) && !p2.equals(p3) && !p2.equals(p4)) return true;
        if (d3 == 0 && onSegment(p1, p2, p3) && !p3.equals(p1) && !p3.equals(p2)) return true;
        if (d4 == 0 && onSegment(p1, p2, p4) && !p4.equals(p1) && !p4.equals(p2)) return true;

        return false;
    }

    /**
     * Вычисление направления поворота
     */
    private static double direction(Point p1, Point p2, Point p3) {
        return (p3.x - p1.x) * (p2.y - p1.y) - (p2.x - p1.x) * (p3.y - p1.y);
    }

    /**
     * Проверка, лежит ли точка на отрезке
     */
    private static boolean onSegment(Point p1, Point p2, Point p) {
        return p.x >= Math.min(p1.x, p2.x) && p.x <= Math.max(p1.x, p2.x) &&
               p.y >= Math.min(p1.y, p2.y) && p.y <= Math.max(p1.y, p2.y);
    }

    /**
     * Проверка, является ли полигон выпуклым
     */
    public static boolean isConvexPolygon(Polygon polygon) {
        int n = polygon.vertices.size();
        if (n < 3) return false;

        if (!isSimplePolygon(polygon)) {
            return false;
        }

        boolean hasPositive = false;
        boolean hasNegative = false;

        for (int i = 0; i < n; i++) {
            Point p1 = polygon.vertices.get(i);
            Point p2 = polygon.vertices.get((i + 1) % n);
            Point p3 = polygon.vertices.get((i + 2) % n);

            double crossProduct = (p2.x - p1.x) * (p3.y - p2.y) - (p2.y - p1.y) * (p3.x - p2.x);

            if (crossProduct > 0) hasPositive = true;
            if (crossProduct < 0) hasNegative = true;

            if (hasPositive && hasNegative) return false;
        }

        return true;
    }

    /**
     * Заполнение полигона по правилу even-odd
     */
    public static void fillPolygonEvenOdd(BufferedImage img, Polygon polygon, int color) {
        WritableRaster raster = img.getRaster();
        int width = img.getWidth();
        int height = img.getHeight();

        // Находим bounding box
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (Point p : polygon.vertices) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        minX = Math.max(0, minX);
        maxX = Math.min(width - 1, maxX);
        minY = Math.max(0, minY);
        maxY = Math.min(height - 1, maxY);

        // Для каждой точки в bounding box
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (isInsideEvenOdd(polygon, x, y)) {
                    raster.setSample(x, y, 0, color);
                }
            }
        }
    }

    /**
     * Проверка, лежит ли точка на границе полигона (вершина или сторона)
     */
    private static boolean isPointOnPolygonBoundary(Polygon polygon, int x, int y) {
        int n = polygon.vertices.size();
        Point p = new Point(x, y);

        for (int i = 0; i < n; i++) {
            Point p1 = polygon.vertices.get(i);
            Point p2 = polygon.vertices.get((i + 1) % n);

            // Проверяем, лежит ли точка на отрезке p1-p2
            if (isPointOnSegment(p1, p2, p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверка, лежит ли точка на отрезке
     */
    private static boolean isPointOnSegment(Point p1, Point p2, Point p) {
        // Векторное произведение должно быть 0 (коллинеарность)
        double cross = (p.x - p1.x) * (p2.y - p1.y) - (p.y - p1.y) * (p2.x - p1.x);
        if (Math.abs(cross) > 0.0001) return false;

        // Точка должна лежать в bounding box отрезка
        return p.x >= Math.min(p1.x, p2.x) && p.x <= Math.max(p1.x, p2.x) &&
               p.y >= Math.min(p1.y, p2.y) && p.y <= Math.max(p1.y, p2.y);
    }

    /**
     * Проверка принадлежности точки полигону по правилу even-odd
     */
    private static boolean isInsideEvenOdd(Polygon polygon, int x, int y) {
        if (isPointOnPolygonBoundary(polygon, x, y)) {
            return true;
        }

        int intersections = 0;
        int n = polygon.vertices.size();

        for (int i = 0; i < n; i++) {
            Point p1 = polygon.vertices.get(i);
            Point p2 = polygon.vertices.get((i + 1) % n);

            if (rayIntersectsSegment(x, y, p1, p2)) {
                intersections++;
            }
        }

        return (intersections % 2) == 1;
    }

    /**
     * Проверка пересечения луча с отрезком (для even-odd)
     */
    private static boolean rayIntersectsSegment(int x, int y, Point p1, Point p2) {
        // Луч идет вправо от точки (x, y)
        if (p1.y > p2.y) {
            Point temp = p1;
            p1 = p2;
            p2 = temp;
        }

        if (y < p1.y || y >= p2.y) return false;
        if (x >= Math.max(p1.x, p2.x)) return false;
        if (x < Math.min(p1.x, p2.x)) return true;

        double xIntersection = (y - p1.y) * (p2.x - p1.x) / (double)(p2.y - p1.y) + p1.x;
        return x < xIntersection;
    }

    /**
     * Заполнение полигона по правилу non-zero winding
     */
    public static void fillPolygonNonZeroWinding(BufferedImage img, Polygon polygon, int color) {
        WritableRaster raster = img.getRaster();
        int width = img.getWidth();
        int height = img.getHeight();

        // Находим bounding box
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (Point p : polygon.vertices) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        minX = Math.max(0, minX);
        maxX = Math.min(width - 1, maxX);
        minY = Math.max(0, minY);
        maxY = Math.min(height - 1, maxY);

        // Для каждой точки в bounding box
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (isInsideNonZeroWinding(polygon, x, y)) {
                    raster.setSample(x, y, 0, color);
                }
            }
        }
    }

    /**
     * Проверка принадлежности точки полигону по правилу non-zero winding
     */
    private static boolean isInsideNonZeroWinding(Polygon polygon, int x, int y) {
        if (isPointOnPolygonBoundary(polygon, x, y)) {
            return true;
        }

        int windingNumber = 0;
        int n = polygon.vertices.size();

        for (int i = 0; i < n; i++) {
            Point p1 = polygon.vertices.get(i);
            Point p2 = polygon.vertices.get((i + 1) % n);

            if (p1.y <= y) {
                if (p2.y > y) {
                    // Восходящее ребро
                    if (isLeft(p1, p2, new Point(x, y)) > 0) {
                        windingNumber++;
                    }
                }
            } else {
                if (p2.y <= y) {
                    // Нисходящее ребро
                    if (isLeft(p1, p2, new Point(x, y)) < 0) {
                        windingNumber--;
                    }
                }
            }
        }

        return windingNumber != 0;
    }

    /**
     * Проверка, находится ли точка слева от линии
     */
    private static double isLeft(Point p0, Point p1, Point p2) {
        return (p1.x - p0.x) * (p2.y - p0.y) - (p2.x - p0.x) * (p1.y - p0.y);
    }

    /**
     * Сохранение изображения в файл
     */
    public static boolean saveImage(BufferedImage img, String filepath) {
        try {
            File outputFile = new File(filepath);
            ImageIO.write(img, "png", outputFile);
            System.out.println("   Сохранено: " + filepath);
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении: " + e.getMessage());
            return false;
        }
    }

    /**
     * Панель для отображения полигонов
     */
    private class PolygonPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBackground(Color.WHITE);

            int cols = 4;
            int rows = 3;
            int cellWidth = getWidth() / cols;
            int cellHeight = getHeight() / rows;

            // Рисуем каждый полигон в отдельной ячейке
            for (int i = 0; i < testPolygons.size() && i < 4; i++) {
                Polygon poly = testPolygons.get(i);
                int col = i % cols;
                int row = i / cols;

                // Смещение для каждого представления
                int offsetX = col * cellWidth + 20;
                int offsetY = row * cellHeight + 40;

                // 1. Только контур
                drawPolygonInCell(g, poly, offsetX, offsetY, "Outline", false, false);

                // 2. Even-Odd fill
                drawPolygonInCell(g, poly, offsetX, offsetY + cellHeight, "Even-Odd Fill", true, false);

                // 3. Non-Zero Winding fill
                drawPolygonInCell(g, poly, offsetX, offsetY + 2 * cellHeight, "Non-Zero Winding", true, true);
            }
        }

        private void drawPolygonInCell(Graphics g, Polygon poly, int offsetX, int offsetY,
                                        String label, boolean fill, boolean nonZeroWinding) {
            BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = img.getRaster();

            // Заполняем белым
            for (int y = 0; y < 200; y++) {
                for (int x = 0; x < 200; x++) {
                    raster.setSample(x, y, 0, 255);
                }
            }

            // Заполнение (если требуется)
            if (fill) {
                if (nonZeroWinding) {
                    fillPolygonNonZeroWinding(img, poly, 200);
                } else {
                    fillPolygonEvenOdd(img, poly, 200);
                }
            }

            // Рисуем контур
            drawPolygon(img, poly, 0);

            // Отображаем
            g.drawImage(img, offsetX, offsetY, null);

            // Подпись
            g.setColor(Color.BLACK);
            g.drawString(poly.name, offsetX, offsetY - 25);
            g.drawString(label, offsetX, offsetY - 10);

            // Информация о типе
            boolean isSimple = isSimplePolygon(poly);
            boolean isConvex = isConvexPolygon(poly);
            String typeInfo = (isSimple ? "Simple" : "Complex") + ", " +
                             (isConvex ? "Convex" : "Non-convex");
            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g.drawString(typeInfo, offsetX, offsetY + 210);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Лабораторная работа №3: Построение и заполнение полигонов ===\n");

        Lab3 lab = new Lab3();

        // Сохраняем демонстрационные изображения
        System.out.println("Создание демонстрационных изображений:\n");

        for (Polygon poly : lab.testPolygons) {
            String baseName = "res/polygon_" + poly.name.toLowerCase()
                .replaceAll("[^a-z0-9]", "_");

            // 1. Только контур
            BufferedImage outline = new BufferedImage(200, 200, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = outline.getRaster();
            for (int y = 0; y < 200; y++) {
                for (int x = 0; x < 200; x++) {
                    raster.setSample(x, y, 0, 255);
                }
            }
            drawPolygon(outline, poly, 0);
            saveImage(outline, baseName + "_outline.png");

            // 2. Even-Odd fill
            BufferedImage evenOdd = new BufferedImage(200, 200, BufferedImage.TYPE_BYTE_GRAY);
            raster = evenOdd.getRaster();
            for (int y = 0; y < 200; y++) {
                for (int x = 0; x < 200; x++) {
                    raster.setSample(x, y, 0, 255);
                }
            }
            fillPolygonEvenOdd(evenOdd, poly, 200);
            drawPolygon(evenOdd, poly, 0);
            saveImage(evenOdd, baseName + "_evenodd.png");

            // 3. Non-Zero Winding fill
            BufferedImage nonZero = new BufferedImage(200, 200, BufferedImage.TYPE_BYTE_GRAY);
            raster = nonZero.getRaster();
            for (int y = 0; y < 200; y++) {
                for (int x = 0; x < 200; x++) {
                    raster.setSample(x, y, 0, 255);
                }
            }
            fillPolygonNonZeroWinding(nonZero, poly, 200);
            drawPolygon(nonZero, poly, 0);
            saveImage(nonZero, baseName + "_nonzero.png");

            // Информация о полигоне
            boolean isSimple = isSimplePolygon(poly);
            boolean isConvex = isConvexPolygon(poly);
            System.out.println(poly.name + ":");
            System.out.println("  Тип: " + (isSimple ? "Простой" : "Сложный (самопересечения)"));
            System.out.println("  Форма: " + (isConvex ? "Выпуклый" : "Невыпуклый"));
            System.out.println();
        }

        System.out.println("=== Запуск GUI ===\n");

        SwingUtilities.invokeLater(() -> {
            lab.setVisible(true);
        });
    }
}
