import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class HomeWork {

    static class Point {
        double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return String.format("(%.2f, %.2f)", x, y);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Point)) return false;
            Point p = (Point) obj;
            return Math.abs(x - p.x) < 1e-9 && Math.abs(y - p.y) < 1e-9;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Math.round(x * 1e9), Math.round(y * 1e9));
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

        public void addVertex(Point p) {
            vertices.add(p);
        }
    }

    static class Edge {
        Point start, end;
        public Edge(Point start, Point end) {
            this.start = start;
            this.end = end;
        }
    }

    static Point getIntersection(Point p1, Point p2, Point p3, Point p4) {
        double x1 = p1.x, y1 = p1.y;
        double x2 = p2.x, y2 = p2.y;
        double x3 = p3.x, y3 = p3.y;
        double x4 = p4.x, y4 = p4.y;

        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 1e-10) return null;

        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
        double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom;

        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            return new Point(x1 + t * (x2 - x1), y1 + t * (y2 - y1));
        }

        return null;
    }

    static boolean isPointInPolygon(Point p, Polygon polygon) {
        int count = 0;
        int n = polygon.vertices.size();

        for (int i = 0; i < n; i++) {
            Point v1 = polygon.vertices.get(i);
            Point v2 = polygon.vertices.get((i + 1) % n);

            if ((v1.y <= p.y && v2.y > p.y) || (v1.y > p.y && v2.y <= p.y)) {
                double vt = (p.y - v1.y) / (v2.y - v1.y);
                if (p.x < v1.x + vt * (v2.x - v1.x)) {
                    count++;
                }
            }
        }

        return (count % 2) == 1;
    }

    static Polygon weilerAthertonClip(Polygon subject, Polygon clip) {
        List<Point> result = new ArrayList<>();
        int n1 = subject.vertices.size();
        int n2 = clip.vertices.size();

        for (Point p : subject.vertices) {
            if (isPointInPolygon(p, clip)) {
                result.add(p);
            }
        }

        for (Point p : clip.vertices) {
            if (isPointInPolygon(p, subject)) {
                if (!result.contains(p)) {
                    result.add(p);
                }
            }
        }

        for (int i = 0; i < n1; i++) {
            Point s1 = subject.vertices.get(i);
            Point s2 = subject.vertices.get((i + 1) % n1);

            for (int j = 0; j < n2; j++) {
                Point c1 = clip.vertices.get(j);
                Point c2 = clip.vertices.get((j + 1) % n2);

                Point intersection = getIntersection(s1, s2, c1, c2);
                if (intersection != null && !result.contains(intersection)) {
                    result.add(intersection);
                }
            }
        }

        if (result.isEmpty()) return null;

        Point center = getCentroid(result);
        result.sort((p1, p2) -> {
            double angle1 = Math.atan2(p1.y - center.y, p1.x - center.x);
            double angle2 = Math.atan2(p2.y - center.y, p2.x - center.x);
            return Double.compare(angle1, angle2);
        });

        Polygon resultPoly = new Polygon("Clipped");
        for (Point p : result) {
            resultPoly.addVertex(p);
        }
        return resultPoly;
    }

    static Point getCentroid(List<Point> points) {
        double cx = 0, cy = 0;
        for (Point p : points) {
            cx += p.x;
            cy += p.y;
        }
        return new Point(cx / points.size(), cy / points.size());
    }

    static Point hermitePoint(Point p0, Point p1, Point t0, Point t1, double t) {
        double t2 = t * t;
        double t3 = t2 * t;

        double h00 = 2*t3 - 3*t2 + 1;
        double h10 = t3 - 2*t2 + t;
        double h01 = -2*t3 + 3*t2;
        double h11 = t3 - t2;

        double x = h00 * p0.x + h10 * t0.x + h01 * p1.x + h11 * t1.x;
        double y = h00 * p0.y + h10 * t0.y + h01 * p1.y + h11 * t1.y;

        return new Point(x, y);
    }

    static List<Point> drawHermiteCurve(Point[] points, Point[] tangents, int steps) {
        List<Point> curve = new ArrayList<>();

        for (int i = 0; i < points.length - 1; i++) {
            for (int j = 0; j <= steps; j++) {
                double t = (double) j / steps;
                Point p = hermitePoint(points[i], points[i+1], tangents[i], tangents[i+1], t);
                curve.add(p);
            }
        }

        return curve;
    }

    static class CMYK {
        double c, m, y, k;
        public CMYK(double c, double m, double y, double k) {
            this.c = c;
            this.m = m;
            this.y = y;
            this.k = k;
        }
    }

    static CMYK rgbToCmykGCR(int r, int g, int b, double q) {
        double rNorm = r / 255.0;
        double gNorm = g / 255.0;
        double bNorm = b / 255.0;

        double c = 1 - rNorm;
        double m = 1 - gNorm;
        double y = 1 - bNorm;

        double k_max = Math.min(Math.min(c, m), y);
        double k = k_max * (q / 100.0);

        if (k > 0) {
            c = c - k;
            m = m - k;
            y = y - k;
        }

        double remaining = 1 - k;
        if (remaining > 0.001 && k < k_max) {
            double scale = (1 - k_max) / remaining;
            c = c * scale;
            m = m * scale;
            y = y * scale;
        }

        c = Math.max(0, Math.min(1, c));
        m = Math.max(0, Math.min(1, m));
        y = Math.max(0, Math.min(1, y));
        k = Math.max(0, Math.min(1, k));

        return new CMYK(c, m, y, k);
    }

    static BufferedImage createHistogram(int[] histogram, String label, Color color) {
        int width = 512;
        int height = 350;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        int colorBoxHeight = 60;
        g.setColor(color);
        g.fillRect(10, 10, width - 20, colorBoxHeight);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawRect(10, 10, width - 20, colorBoxHeight);

        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(Color.BLACK);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    g.drawString(label + " Channel", 20 + dx, 45 + dy);
                }
            }
        }
        g.setColor(Color.WHITE);
        g.drawString(label + " Channel", 20, 45);

        int max = 0;
        for (int value : histogram) {
            max = Math.max(max, value);
        }

        int histTop = colorBoxHeight + 20;
        int histHeight = height - histTop - 50;

        g.setColor(color);
        int barWidth = 2;
        for (int i = 0; i < 256; i++) {
            if (max > 0) {
                int barHeight = (int) ((double) histogram[i] / max * histHeight);
                g.fillRect(i * barWidth, histTop + histHeight - barHeight, barWidth, barHeight);
            }
        }

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawLine(0, histTop + histHeight, width, histTop + histHeight);
        g.drawLine(0, histTop, 0, histTop + histHeight);

        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("Histogram", 10, histTop + histHeight + 20);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.drawString("0", 5, histTop + histHeight + 35);
        g.drawString("128", width / 2 - 15, histTop + histHeight + 35);
        g.drawString("255", width - 30, histTop + histHeight + 35);

        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString("Max: " + max + " pixels", width - 120, histTop + 15);

        g.dispose();
        return img;
    }

    static void createCMYKHistograms(BufferedImage img, double gcrPercent) {
        int[] histC = new int[256];
        int[] histM = new int[256];
        int[] histY = new int[256];
        int[] histK = new int[256];

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                CMYK cmyk = rgbToCmykGCR(r, g, b, gcrPercent);

                histC[(int)(cmyk.c * 255)]++;
                histM[(int)(cmyk.m * 255)]++;
                histY[(int)(cmyk.y * 255)]++;
                histK[(int)(cmyk.k * 255)]++;
            }
        }

        BufferedImage histCImg = createHistogram(histC, "Cyan", new Color(0, 255, 255));
        BufferedImage histMImg = createHistogram(histM, "Magenta", new Color(255, 0, 255));
        BufferedImage histYImg = createHistogram(histY, "Yellow", new Color(255, 255, 0));
        BufferedImage histKImg = createHistogram(histK, "Black", new Color(0, 0, 0));

        saveImage(histCImg, "res/hw_hist_c.png");
        saveImage(histMImg, "res/hw_hist_m.png");
        saveImage(histYImg, "res/hw_hist_y.png");
        saveImage(histKImg, "res/hw_hist_k.png");
        System.out.println("CMYK histograms created (GCR = " + gcrPercent + "%)");
    }

    static void drawLine(BufferedImage img, Point p1, Point p2, int gray) {
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(gray, gray, gray));
        g.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
        g.dispose();
    }

    static void drawPolygon(BufferedImage img, Polygon poly, int gray) {
        for (int i = 0; i < poly.vertices.size(); i++) {
            Point p1 = poly.vertices.get(i);
            Point p2 = poly.vertices.get((i + 1) % poly.vertices.size());
            drawLine(img, p1, p2, gray);
        }
    }

    static void fillPolygon(BufferedImage img, Polygon poly, int gray) {
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(gray, gray, gray));

        int[] xPoints = new int[poly.vertices.size()];
        int[] yPoints = new int[poly.vertices.size()];

        for (int i = 0; i < poly.vertices.size(); i++) {
            xPoints[i] = (int) poly.vertices.get(i).x;
            yPoints[i] = (int) poly.vertices.get(i).y;
        }

        g.fillPolygon(xPoints, yPoints, poly.vertices.size());
        g.dispose();
    }

    static void drawPoints(BufferedImage img, List<Point> points, int gray, int size) {
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(gray, gray, gray));

        for (Point p : points) {
            g.fillOval((int)p.x - size/2, (int)p.y - size/2, size, size);
        }

        g.dispose();
    }

    static boolean saveImage(BufferedImage img, String filepath) {
        try {
            File outputFile = new File(filepath);
            outputFile.getParentFile().mkdirs();
            ImageIO.write(img, "png", outputFile);
            System.out.println("✓ Saved: " + filepath);
            return true;
        } catch (IOException e) {
            System.err.println("Error saving image: " + e.getMessage());
            return false;
        }
    }

    static BufferedImage loadImage(String filepath) {
        try {
            return ImageIO.read(new File(filepath));
        } catch (IOException e) {
            System.err.println("Error loading image: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Домашняя работа ===\n");

        System.out.println("Задание 1: Отсечение полигонов");
        demonstratePolygonClipping();

        System.out.println("\nЗадание 2: Кривая Эрмита");
        demonstrateHermiteCurve();

        System.out.println("\nЗадание 3: Гистограммы CMYK");
        demonstrateCMYKHistograms();

        System.out.println("\n=== Выполнено ===");
    }

    static void demonstratePolygonClipping() {
        int width = 600;
        int height = 600;

        // Пятиугольник отсекается квадратом
        Polygon pentagon = new Polygon("Pentagon");
        pentagon.addVertex(300, 100);
        pentagon.addVertex(450, 250);
        pentagon.addVertex(380, 450);
        pentagon.addVertex(220, 450);
        pentagon.addVertex(150, 250);

        Polygon square = new Polygon("Square");
        square.addVertex(200, 200);
        square.addVertex(400, 200);
        square.addVertex(400, 400);
        square.addVertex(200, 400);

        BufferedImage img1 = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g1 = img1.createGraphics();
        g1.setColor(Color.WHITE);
        g1.fillRect(0, 0, width, height);
        g1.dispose();

        fillPolygon(img1, pentagon, 200);
        drawPolygon(img1, pentagon, 100);
        drawPolygon(img1, square, 0);
        saveImage(img1, "res/hw_clip_setup.png");

        Polygon clipped1 = weilerAthertonClip(pentagon, square);
        if (clipped1 != null) {
            BufferedImage result1 = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D gr1 = result1.createGraphics();
            gr1.setColor(Color.WHITE);
            gr1.fillRect(0, 0, width, height);
            gr1.dispose();

            fillPolygon(result1, clipped1, 150);
            drawPolygon(result1, clipped1, 0);
            drawPolygon(result1, square, 50);
            saveImage(result1, "res/hw_clip_result.png");
        }

        // Треугольник отсекается шестиугольником
        Polygon triangle = new Polygon("Triangle");
        triangle.addVertex(100, 450);
        triangle.addVertex(500, 450);
        triangle.addVertex(300, 100);

        Polygon hexagon = new Polygon("Hexagon");
        double cx = 300, cy = 300, r = 150;
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3;
            hexagon.addVertex(cx + r * Math.cos(angle), cy + r * Math.sin(angle));
        }

        BufferedImage img2 = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = img2.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        g2.dispose();

        fillPolygon(img2, triangle, 200);
        drawPolygon(img2, triangle, 100);
        drawPolygon(img2, hexagon, 0);
        saveImage(img2, "res/hw_clip_tri_hex_setup.png");

        Polygon clipped2 = weilerAthertonClip(triangle, hexagon);
        if (clipped2 != null) {
            BufferedImage result2 = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D gr2 = result2.createGraphics();
            gr2.setColor(Color.WHITE);
            gr2.fillRect(0, 0, width, height);
            gr2.dispose();

            fillPolygon(result2, clipped2, 150);
            drawPolygon(result2, clipped2, 0);
            drawPolygon(result2, hexagon, 50);
            saveImage(result2, "res/hw_clip_tri_hex_result.png");
        }
    }

    static void demonstrateHermiteCurve() {
        int width = 800;
        int height = 600;

        // Определяем контрольные точки
        Point[] points = {
            new Point(100, 300),
            new Point(250, 150),
            new Point(450, 450),
            new Point(650, 200),
            new Point(750, 350)
        };

        // Определяем касательные векторы
        Point[] tangents = {
            new Point(100, -50),   // Начальная касательная
            new Point(150, 100),
            new Point(100, -150),
            new Point(80, 80),
            new Point(50, 0)       // Конечная касательная
        };

        // Строим кривую
        List<Point> curve = drawHermiteCurve(points, tangents, 50);

        // Создаем изображение
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);

        // Рисуем кривую
        for (int i = 0; i < curve.size() - 1; i++) {
            Point p1 = curve.get(i);
            Point p2 = curve.get(i + 1);
            g.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
        }

        // Рисуем контрольные точки
        g.setColor(new Color(100, 100, 100));
        for (int i = 0; i < points.length; i++) {
            Point p = points[i];
            g.fillOval((int)p.x - 5, (int)p.y - 5, 10, 10);

            // Рисуем касательные векторы
            Point t = tangents[i];
            g.drawLine((int)p.x, (int)p.y, (int)(p.x + t.x), (int)(p.y + t.y));
        }

        g.dispose();
        saveImage(img, "res/hw_hermite_curve.png");

        // Создаем версию с подписями
        BufferedImage imgLabeled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = imgLabeled.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        // Рисуем кривую
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        for (int i = 0; i < curve.size() - 1; i++) {
            Point p1 = curve.get(i);
            Point p2 = curve.get(i + 1);
            g2.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
        }

        // Контрольные точки и касательные
        g2.setColor(Color.RED);
        for (int i = 0; i < points.length; i++) {
            Point p = points[i];
            g2.fillOval((int)p.x - 5, (int)p.y - 5, 10, 10);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString("P" + i, (int)p.x + 10, (int)p.y - 10);
        }

        // Касательные векторы
        g2.setColor(Color.BLUE);
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                     10, new float[]{5, 5}, 0));
        for (int i = 0; i < points.length; i++) {
            Point p = points[i];
            Point t = tangents[i];
            g2.drawLine((int)p.x, (int)p.y, (int)(p.x + t.x), (int)(p.y + t.y));
        }

        // Заголовок
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("Composite Cubic Hermite Curve", 20, 30);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.drawString("Red dots: control points", 20, 50);
        g2.drawString("Blue dashed: tangent vectors", 20, 70);

        g2.dispose();
        saveImage(imgLabeled, "res/hw_hermite_curve_labeled.png");
    }

    static void demonstrateCMYKHistograms() {
        // Создаем тестовое цветное изображение с СЕРЫМИ областями для демонстрации GCR
        int width = 600;
        int height = 400;
        BufferedImage colorImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = colorImg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Фон - белый
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // БОЛЬШАЯ СЕРАЯ ОБЛАСТЬ (главная для демонстрации GCR)
        // Градиент от темно-серого к светло-серому
        for (int i = 0; i < 250; i++) {
            int gray = 50 + i;  // от 50 до 300 (обрезается до 255)
            if (gray > 255) gray = 255;
            g.setColor(new Color(gray, gray, gray));
            g.fillRect(10 + i, 10, 2, 180);
        }

        // Темно-серый квадрат
        g.setColor(new Color(80, 80, 80));
        g.fillRect(270, 10, 90, 90);

        // Средне-серый квадрат
        g.setColor(new Color(128, 128, 128));
        g.fillRect(370, 10, 90, 90);

        // Светло-серый квадрат
        g.setColor(new Color(200, 200, 200));
        g.fillRect(470, 10, 90, 90);

        // Чистые цвета (для сравнения - на них GCR не влияет)
        g.setColor(Color.RED);
        g.fillRect(10, 210, 90, 90);

        g.setColor(Color.GREEN);
        g.fillRect(110, 210, 90, 90);

        g.setColor(Color.BLUE);
        g.fillRect(210, 210, 90, 90);

        g.setColor(Color.YELLOW);
        g.fillRect(310, 210, 90, 90);

        // Коричневые оттенки (содержат серый компонент)
        g.setColor(new Color(139, 69, 19));  // Saddle brown
        g.fillRect(410, 210, 90, 90);

        g.setColor(new Color(160, 82, 45));  // Sienna
        g.fillRect(510, 210, 90, 90);

        // Пастельные тона (содержат много серого)
        g.setColor(new Color(200, 180, 180));  // Пастельный розовый
        g.fillRect(10, 310, 90, 80);

        g.setColor(new Color(180, 200, 180));  // Пастельный зеленый
        g.fillRect(110, 310, 90, 80);

        g.setColor(new Color(180, 180, 200));  // Пастельный синий
        g.fillRect(210, 310, 90, 80);

        // Черный квадрат
        g.setColor(Color.BLACK);
        g.fillRect(310, 310, 90, 80);

        // Белый квадрат
        g.setColor(Color.WHITE);
        g.fillRect(410, 310, 90, 80);

        // Серый в центре
        g.setColor(new Color(150, 150, 150));
        g.fillRect(510, 310, 90, 80);

        // Подписи
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("Gray Gradient (GCR test area)", 15, 205);
        g.drawString("Pure Colors (no GCR effect)", 15, 330);

        g.dispose();
        saveImage(colorImg, "res/hw_color_test.png");

        // Создаем гистограммы с разными значениями GCR
        System.out.println("  Creating CMYK histograms with GCR = 0%");
        createCMYKHistograms(colorImg, 0);

        System.out.println("  Creating CMYK histograms with GCR = 50%");
        BufferedImage img50 = loadImage("res/hw_color_test.png");
        if (img50 != null) {
            createCMYKHistogramsWithSuffix(img50, 50, "_gcr50");
        }

        System.out.println("  Creating CMYK histograms with GCR = 100%");
        BufferedImage img100 = loadImage("res/hw_color_test.png");
        if (img100 != null) {
            createCMYKHistogramsWithSuffix(img100, 100, "_gcr100");
        }
    }

    static void createCMYKHistogramsWithSuffix(BufferedImage img, double gcrPercent, String suffix) {
        int[] histC = new int[256];
        int[] histM = new int[256];
        int[] histY = new int[256];
        int[] histK = new int[256];

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                CMYK cmyk = rgbToCmykGCR(r, g, b, gcrPercent);

                histC[(int)(cmyk.c * 255)]++;
                histM[(int)(cmyk.m * 255)]++;
                histY[(int)(cmyk.y * 255)]++;
                histK[(int)(cmyk.k * 255)]++;
            }
        }

        BufferedImage histCImg = createHistogram(histC, "Cyan (GCR=" + (int)gcrPercent + "%)", new Color(0, 255, 255));
        BufferedImage histMImg = createHistogram(histM, "Magenta (GCR=" + (int)gcrPercent + "%)", new Color(255, 0, 255));
        BufferedImage histYImg = createHistogram(histY, "Yellow (GCR=" + (int)gcrPercent + "%)", new Color(255, 255, 0));
        BufferedImage histKImg = createHistogram(histK, "Black (GCR=" + (int)gcrPercent + "%)", new Color(0, 0, 0));

        saveImage(histCImg, "res/hw_hist_c" + suffix + ".png");
        saveImage(histMImg, "res/hw_hist_m" + suffix + ".png");
        saveImage(histYImg, "res/hw_hist_y" + suffix + ".png");
        saveImage(histKImg, "res/hw_hist_k" + suffix + ".png");
    }
}
