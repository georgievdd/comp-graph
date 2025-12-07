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
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

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
        setTitle("Lab 4: Bezier Curves and Line Clipping");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        add(new DemoPanel());
    }

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
            this.normal = new Point(-dy, dx);
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

    public static Point[] cyrusBeckClip(Point p1, Point p2, Polygon clipPolygon) {
        if (!isConvex(clipPolygon)) {
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

            double wx = p1.x - edge.p1.x;
            double wy = p1.y - edge.p1.y;

            double numerator = -(edge.normal.x * wx + edge.normal.y * wy);
            double denominator = edge.normal.x * dx + edge.normal.y * dy;

            if (Math.abs(denominator) < 1e-10) {
                if (numerator < 0) {
                    return null;
                }
            } else {
                double t = numerator / denominator;

                if (denominator < 0) {
                    tMin = Math.max(tMin, t);
                } else {
                    tMax = Math.min(tMax, t);
                }

                if (tMin > tMax) {
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
            System.out.println("   Сохранено: " + filepath);
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении: " + e.getMessage());
            return false;
        }
    }

    private class DemoPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBackground(Color.WHITE);

            int cols = 3;
            int rows = 2;
            int cellWidth = getWidth() / cols;
            int cellHeight = getHeight() / rows;

            drawBezierDemo(g, 20, 20, cellWidth - 40, cellHeight - 40, "Simple Cubic Bezier");
            drawBezierDemo2(g, cellWidth + 20, 20, cellWidth - 40, cellHeight - 40, "S-Curve Bezier");
            drawBezierDemo3(g, 2 * cellWidth + 20, 20, cellWidth - 40, cellHeight - 40, "Loop Bezier");

            drawClippingDemo(g, 20, cellHeight + 20, cellWidth - 40, cellHeight - 40, "Line Clipping - Triangle");
            drawClippingDemo2(g, cellWidth + 20, cellHeight + 20, cellWidth - 40, cellHeight - 40, "Line Clipping - Pentagon");
            drawClippingDemo3(g, 2 * cellWidth + 20, cellHeight + 20, cellWidth - 40, cellHeight - 40, "Multiple Lines Clipping");
        }

        private void drawBezierDemo(Graphics g, int offsetX, int offsetY, int w, int h, String title) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = img.getRaster();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    raster.setSample(x, y, 0, 255);
                }
            }

            Point p0 = new Point(50, h - 50);
            Point p1 = new Point(100, 50);
            Point p2 = new Point(w - 100, 50);
            Point p3 = new Point(w - 50, h - 50);

            drawBezierCubic(img, p0, p1, p2, p3, 0, 100);

            drawLine(img, (int) p0.x, (int) p0.y, (int) p1.x, (int) p1.y, 180);
            drawLine(img, (int) p2.x, (int) p2.y, (int) p3.x, (int) p3.y, 180);

            g.drawImage(img, offsetX, offsetY, null);
            g.setColor(Color.BLACK);
            g.drawString(title, offsetX, offsetY - 5);
        }

        private void drawBezierDemo2(Graphics g, int offsetX, int offsetY, int w, int h, String title) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = img.getRaster();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    raster.setSample(x, y, 0, 255);
                }
            }

            Point p0 = new Point(50, h / 2);
            Point p1 = new Point(w / 3, 50);
            Point p2 = new Point(2 * w / 3, h - 50);
            Point p3 = new Point(w - 50, h / 2);

            drawBezierCubic(img, p0, p1, p2, p3, 0, 100);

            drawLine(img, (int) p0.x, (int) p0.y, (int) p1.x, (int) p1.y, 180);
            drawLine(img, (int) p2.x, (int) p2.y, (int) p3.x, (int) p3.y, 180);

            g.drawImage(img, offsetX, offsetY, null);
            g.setColor(Color.BLACK);
            g.drawString(title, offsetX, offsetY - 5);
        }

        private void drawBezierDemo3(Graphics g, int offsetX, int offsetY, int w, int h, String title) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = img.getRaster();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    raster.setSample(x, y, 0, 255);
                }
            }

            Point p0 = new Point(w / 2, h - 50);
            Point p1 = new Point(w - 50, h - 100);
            Point p2 = new Point(50, 100);
            Point p3 = new Point(w / 2, 50);

            drawBezierCubic(img, p0, p1, p2, p3, 0, 100);

            drawLine(img, (int) p0.x, (int) p0.y, (int) p1.x, (int) p1.y, 180);
            drawLine(img, (int) p2.x, (int) p2.y, (int) p3.x, (int) p3.y, 180);

            g.drawImage(img, offsetX, offsetY, null);
            g.setColor(Color.BLACK);
            g.drawString(title, offsetX, offsetY - 5);
        }

        private void drawClippingDemo(Graphics g, int offsetX, int offsetY, int w, int h, String title) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = img.getRaster();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    raster.setSample(x, y, 0, 255);
                }
            }

            Polygon clipPoly = new Polygon("Triangle");
            clipPoly.addVertex(w / 2, 50);
            clipPoly.addVertex(w - 50, h - 50);
            clipPoly.addVertex(50, h - 50);

            drawPolygon(img, clipPoly, 100);

            Point[] testLines = {
                new Point(20, 20), new Point(w - 20, h - 20),
                new Point(w - 20, 20), new Point(20, h - 20),
                new Point(w / 2, 20), new Point(w / 2, h - 20),
            };

            for (int i = 0; i < testLines.length; i += 2) {
                Point p1 = testLines[i];
                Point p2 = testLines[i + 1];

                drawLine(img, (int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y, 200);

                Point[] clipped = cyrusBeckClip(p1, p2, clipPoly);
                if (clipped != null) {
                    drawLine(img, (int) Math.round(clipped[0].x), (int) Math.round(clipped[0].y),
                            (int) Math.round(clipped[1].x), (int) Math.round(clipped[1].y), 0);
                }
            }

            g.drawImage(img, offsetX, offsetY, null);
            g.setColor(Color.BLACK);
            g.drawString(title, offsetX, offsetY - 5);
        }

        private void drawClippingDemo2(Graphics g, int offsetX, int offsetY, int w, int h, String title) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = img.getRaster();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    raster.setSample(x, y, 0, 255);
                }
            }

            Polygon clipPoly = new Polygon("Pentagon");
            int cx = w / 2;
            int cy = h / 2;
            int radius = Math.min(w, h) / 3;
            for (int i = 0; i < 5; i++) {
                double angle = -Math.PI / 2 + i * 2 * Math.PI / 5;
                clipPoly.addVertex(cx + radius * Math.cos(angle), cy + radius * Math.sin(angle));
            }

            drawPolygon(img, clipPoly, 100);

            Point[] testLines = {
                new Point(20, h / 2), new Point(w - 20, h / 2),
                new Point(w / 2, 20), new Point(w / 2, h - 20),
                new Point(50, 50), new Point(w - 50, h - 50),
            };

            for (int i = 0; i < testLines.length; i += 2) {
                Point p1 = testLines[i];
                Point p2 = testLines[i + 1];

                drawLine(img, (int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y, 200);

                Point[] clipped = cyrusBeckClip(p1, p2, clipPoly);
                if (clipped != null) {
                    drawLine(img, (int) Math.round(clipped[0].x), (int) Math.round(clipped[0].y),
                            (int) Math.round(clipped[1].x), (int) Math.round(clipped[1].y), 0);
                }
            }

            g.drawImage(img, offsetX, offsetY, null);
            g.setColor(Color.BLACK);
            g.drawString(title, offsetX, offsetY - 5);
        }

        private void drawClippingDemo3(Graphics g, int offsetX, int offsetY, int w, int h, String title) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = img.getRaster();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    raster.setSample(x, y, 0, 255);
                }
            }

            Polygon clipPoly = new Polygon("Rectangle");
            clipPoly.addVertex(w / 4, h / 4);
            clipPoly.addVertex(3 * w / 4, h / 4);
            clipPoly.addVertex(3 * w / 4, 3 * h / 4);
            clipPoly.addVertex(w / 4, 3 * h / 4);

            drawPolygon(img, clipPoly, 100);

            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                int x1 = (int) (w / 2 + w / 2 * Math.cos(angle));
                int y1 = (int) (h / 2 + h / 2 * Math.sin(angle));

                Point p1 = new Point(w / 2, h / 2);
                Point p2 = new Point(x1, y1);

                drawLine(img, (int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y, 200);

                Point[] clipped = cyrusBeckClip(p1, p2, clipPoly);
                if (clipped != null) {
                    drawLine(img, (int) Math.round(clipped[0].x), (int) Math.round(clipped[0].y),
                            (int) Math.round(clipped[1].x), (int) Math.round(clipped[1].y), 0);
                }
            }

            g.drawImage(img, offsetX, offsetY, null);
            g.setColor(Color.BLACK);
            g.drawString(title, offsetX, offsetY - 5);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Лабораторная работа №4: Кривые Безье, отсечение отрезков ===\n");

        System.out.println("Демонстрация кривых Безье третьего порядка:");
        BufferedImage bezier1 = new BufferedImage(400, 300, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = bezier1.getRaster();
        for (int y = 0; y < 300; y++) {
            for (int x = 0; x < 400; x++) {
                raster.setSample(x, y, 0, 255);
            }
        }

        Point p0 = new Point(50, 250);
        Point p1 = new Point(100, 50);
        Point p2 = new Point(300, 50);
        Point p3 = new Point(350, 250);

        drawBezierCubic(bezier1, p0, p1, p2, p3, 0, 100);
        drawLine(bezier1, (int) p0.x, (int) p0.y, (int) p1.x, (int) p1.y, 180);
        drawLine(bezier1, (int) p2.x, (int) p2.y, (int) p3.x, (int) p3.y, 180);
        saveImage(bezier1, "res/bezier_cubic_demo.png");

        System.out.println("\nДемонстрация алгоритма Кируса-Бека:");
        BufferedImage clipping = new BufferedImage(400, 400, BufferedImage.TYPE_BYTE_GRAY);
        raster = clipping.getRaster();
        for (int y = 0; y < 400; y++) {
            for (int x = 0; x < 400; x++) {
                raster.setSample(x, y, 0, 255);
            }
        }

        Polygon clipPoly = new Polygon("Pentagon");
        int cx = 200, cy = 200, radius = 150;
        for (int i = 0; i < 5; i++) {
            double angle = -Math.PI / 2 + i * 2 * Math.PI / 5;
            clipPoly.addVertex(cx + radius * Math.cos(angle), cy + radius * Math.sin(angle));
        }

        drawPolygon(clipping, clipPoly, 100);

        Point[] testLines = {
            new Point(50, 200), new Point(350, 200),
            new Point(200, 50), new Point(200, 350),
            new Point(50, 50), new Point(350, 350),
            new Point(350, 50), new Point(50, 350),
        };

        for (int i = 0; i < testLines.length; i += 2) {
            Point line1 = testLines[i];
            Point line2 = testLines[i + 1];

            drawLine(clipping, (int) line1.x, (int) line1.y, (int) line2.x, (int) line2.y, 200);

            Point[] clipped = cyrusBeckClip(line1, line2, clipPoly);
            if (clipped != null) {
                drawLine(clipping, (int) Math.round(clipped[0].x), (int) Math.round(clipped[0].y),
                        (int) Math.round(clipped[1].x), (int) Math.round(clipped[1].y), 0);
            }
        }

        saveImage(clipping, "res/cyrus_beck_demo.png");

        System.out.println("\n=== Запуск GUI ===\n");

        SwingUtilities.invokeLater(() -> {
            Lab4 lab = new Lab4();
            lab.setVisible(true);
        });
    }
}
