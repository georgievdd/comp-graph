import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Домашняя работа по компьютерной графике
 * 1. Отсечение отрезка прямой по произвольному полигону
 * 2. Построение дуги окружности с помощью кривых Безье 3-го порядка
 * 3. Гистограммы H, S, V для цветного изображения 24 bpp
 */
public class HomeWork {

    // ==================== КЛАССЫ ====================

    static class Point {
        double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Polygon {
        List<Point> vertices = new ArrayList<>();

        public void addVertex(double x, double y) {
            vertices.add(new Point(x, y));
        }
    }

    // ==================== ЗАДАНИЕ 1: ОТСЕЧЕНИЕ ОТРЕЗКА ПО ПРОИЗВОЛЬНОМУ ПОЛИГОНУ ====================

    /**
     * Отсечение отрезка по произвольному полигону (не обязательно выпуклому).
     * Возвращает список видимых сегментов внутри полигона.
     */
    public static List<Point[]> clipLineByPolygon(Point p1, Point p2, Polygon polygon) {
        List<Point[]> result = new ArrayList<>();
        List<Double> intersections = new ArrayList<>();

        int n = polygon.vertices.size();
        for (int i = 0; i < n; i++) {
            Point v1 = polygon.vertices.get(i);
            Point v2 = polygon.vertices.get((i + 1) % n);

            Double t = getLineIntersectionT(p1, p2, v1, v2);
            if (t != null && t >= 0 && t <= 1) {
                intersections.add(t);
            }
        }

        intersections.add(0.0);
        intersections.add(1.0);
        intersections.sort(Double::compareTo);

        for (int i = 0; i < intersections.size() - 1; i++) {
            double t1 = intersections.get(i);
            double t2 = intersections.get(i + 1);

            if (Math.abs(t2 - t1) < 1e-10) continue;

            double tMid = (t1 + t2) / 2;
            double midX = p1.x + tMid * (p2.x - p1.x);
            double midY = p1.y + tMid * (p2.y - p1.y);

            if (isPointInPolygon(midX, midY, polygon)) {
                Point start = new Point(p1.x + t1 * (p2.x - p1.x), p1.y + t1 * (p2.y - p1.y));
                Point end = new Point(p1.x + t2 * (p2.x - p1.x), p1.y + t2 * (p2.y - p1.y));
                result.add(new Point[]{start, end});
            }
        }

        return result;
    }

    private static Double getLineIntersectionT(Point p1, Point p2, Point v1, Point v2) {
        double dx1 = p2.x - p1.x;
        double dy1 = p2.y - p1.y;
        double dx2 = v2.x - v1.x;
        double dy2 = v2.y - v1.y;

        double denom = dx1 * dy2 - dy1 * dx2;
        if (Math.abs(denom) < 1e-10) return null;

        double t = ((v1.x - p1.x) * dy2 - (v1.y - p1.y) * dx2) / denom;
        double s = ((v1.x - p1.x) * dy1 - (v1.y - p1.y) * dx1) / denom;

        if (s >= 0 && s <= 1) {
            return t;
        }
        return null;
    }

    private static boolean isPointInPolygon(double x, double y, Polygon polygon) {
        int n = polygon.vertices.size();
        boolean inside = false;

        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point pi = polygon.vertices.get(i);
            Point pj = polygon.vertices.get(j);

            if (((pi.y > y) != (pj.y > y)) &&
                (x < (pj.x - pi.x) * (y - pi.y) / (pj.y - pi.y) + pi.x)) {
                inside = !inside;
            }
        }
        return inside;
    }

    // ==================== ЗАДАНИЕ 2: ДУГА ОКРУЖНОСТИ ЧЕРЕЗ КРИВЫЕ БЕЗЬЕ ====================

    /**
     * Строит дугу окружности с помощью кубических кривых Безье (по часовой стрелке).
     */
    public static List<Point[]> createArcWithBezier(double cx, double cy, double r,
                                                     double startAngle, double endAngle) {
        List<Point[]> curves = new ArrayList<>();

        double totalAngle = startAngle - endAngle;
        if (totalAngle < 0) totalAngle += 2 * Math.PI;

        int numSegments = (int) Math.ceil(totalAngle / (Math.PI / 2));
        double segmentAngle = totalAngle / numSegments;

        double currentAngle = startAngle;

        for (int i = 0; i < numSegments; i++) {
            double nextAngle = currentAngle - segmentAngle;
            Point[] bezierPoints = createBezierArcSegment(cx, cy, r, currentAngle, nextAngle);
            curves.add(bezierPoints);
            currentAngle = nextAngle;
        }

        return curves;
    }

    private static Point[] createBezierArcSegment(double cx, double cy, double r,
                                                   double angle1, double angle2) {
        double x1 = cx + r * Math.cos(angle1);
        double y1 = cy + r * Math.sin(angle1);
        double x2 = cx + r * Math.cos(angle2);
        double y2 = cy + r * Math.sin(angle2);

        double theta = Math.abs(angle1 - angle2);
        double k = (4.0 / 3.0) * Math.tan(theta / 4.0);

        double tx1 = -Math.sin(angle1);
        double ty1 = Math.cos(angle1);
        double tx2 = -Math.sin(angle2);
        double ty2 = Math.cos(angle2);

        double cp1x = x1 - k * r * tx1;
        double cp1y = y1 - k * r * ty1;
        double cp2x = x2 + k * r * tx2;
        double cp2y = y2 + k * r * ty2;

        return new Point[]{
            new Point(x1, y1),
            new Point(cp1x, cp1y),
            new Point(cp2x, cp2y),
            new Point(x2, y2)
        };
    }

    // ==================== ЗАДАНИЕ 3: ГИСТОГРАММЫ HSV ====================

    public static int[][] buildHSVHistograms(BufferedImage image) {
        int[] histH = new int[360];
        int[] histS = new int[256];
        int[] histV = new int[256];

        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                float[] hsv = rgbToHsv(r, g, b);
                int h = (int) hsv[0];
                int s = (int) (hsv[1] * 255);
                int v = (int) (hsv[2] * 255);

                histH[h]++;
                histS[s]++;
                histV[v]++;
            }
        }

        return new int[][]{histH, histS, histV};
    }

    public static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0;
        float s = (max == 0) ? 0 : delta / max;
        float v = max;

        if (delta != 0) {
            if (max == rf) {
                h = 60 * (((gf - bf) / delta) % 6);
            } else if (max == gf) {
                h = 60 * ((bf - rf) / delta + 2);
            } else {
                h = 60 * ((rf - gf) / delta + 4);
            }
        }

        if (h < 0) h += 360;

        return new float[]{h, s, v};
    }

    public static BufferedImage drawHistogram(int[] histogram, int width, int height,
                                               Color color, String title) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        int max = 0;
        for (int val : histogram) {
            max = Math.max(max, val);
        }
        if (max == 0) max = 1;

        int barWidth = Math.max(1, (width - 60) / histogram.length);
        int chartHeight = height - 50;
        int offsetX = 40;
        int offsetY = 20;

        g.setColor(color);
        for (int i = 0; i < histogram.length; i++) {
            int barHeight = (int) ((double) histogram[i] / max * chartHeight);
            int x = offsetX + i * barWidth;
            int y = offsetY + chartHeight - barHeight;
            g.fillRect(x, y, Math.max(1, barWidth - 1), barHeight);
        }

        g.setColor(Color.BLACK);
        g.drawLine(offsetX, offsetY, offsetX, offsetY + chartHeight);
        g.drawLine(offsetX, offsetY + chartHeight, offsetX + histogram.length * barWidth, offsetY + chartHeight);

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString(title, width / 2 - 30, height - 10);

        g.dispose();
        return img;
    }

    // ==================== УТИЛИТЫ ====================

    public static void drawLine(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        WritableRaster raster = img.getRaster();
        int width = img.getWidth();
        int height = img.getHeight();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x0 >= 0 && x0 < width && y0 >= 0 && y0 < height) {
                raster.setSample(x0, y0, 0, color);
            }
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    public static void drawPolygon(BufferedImage img, Polygon polygon, int color) {
        int n = polygon.vertices.size();
        for (int i = 0; i < n; i++) {
            Point p1 = polygon.vertices.get(i);
            Point p2 = polygon.vertices.get((i + 1) % n);
            drawLine(img, (int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y, color);
        }
    }

    public static void drawDashedLine(BufferedImage img, int x0, int y0, int x1, int y1, int color) {
        WritableRaster raster = img.getRaster();
        int width = img.getWidth();
        int height = img.getHeight();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int count = 0;

        while (true) {
            if ((count / 5) % 2 == 0 && x0 >= 0 && x0 < width && y0 >= 0 && y0 < height) {
                raster.setSample(x0, y0, 0, color);
            }
            count++;
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    public static void drawBezierCubic(BufferedImage img, Point p0, Point p1, Point p2, Point p3, int color) {
        int steps = 100;
        int prevX = (int) p0.x;
        int prevY = (int) p0.y;

        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double mt = 1 - t;

            double x = mt * mt * mt * p0.x + 3 * mt * mt * t * p1.x +
                       3 * mt * t * t * p2.x + t * t * t * p3.x;
            double y = mt * mt * mt * p0.y + 3 * mt * mt * t * p1.y +
                       3 * mt * t * t * p2.y + t * t * t * p3.y;

            drawLine(img, prevX, prevY, (int) x, (int) y, color);
            prevX = (int) x;
            prevY = (int) y;
        }
    }

    public static void drawPoint(BufferedImage img, int x, int y, int color, int radius) {
        WritableRaster raster = img.getRaster();
        int width = img.getWidth();
        int height = img.getHeight();

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy <= radius * radius) {
                    int px = x + dx;
                    int py = y + dy;
                    if (px >= 0 && px < width && py >= 0 && py < height) {
                        raster.setSample(px, py, 0, color);
                    }
                }
            }
        }
    }

    private static BufferedImage createEmptyImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = img.getRaster();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                raster.setSample(x, y, 0, 255);
            }
        }
        return img;
    }

    public static void saveImage(BufferedImage img, String filepath) {
        try {
            File outputFile = new File(filepath);
            outputFile.getParentFile().mkdirs();
            ImageIO.write(img, "png", outputFile);
            System.out.println("   Saved: " + filepath);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // ==================== ДЕМОНСТРАЦИЯ ====================

    public static void main(String[] args) {
        System.out.println("=== HomeWork Demo ===\n");

        int imgSize = 500;

        // --- Задание 1: Отсечение отрезка по произвольному полигону ---
        System.out.println("1. Line clipping by arbitrary polygon");

        Polygon star = new Polygon();
        int cx = 250, cy = 250;
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            int r = (i % 2 == 0) ? 180 : 80;
            star.addVertex(cx + r * Math.cos(angle), cy + r * Math.sin(angle));
        }

        BufferedImage clipDemo = createEmptyImage(imgSize, imgSize);
        drawPolygon(clipDemo, star, 0);

        Point[][] testLines = {
            {new Point(50, 100), new Point(450, 400)},
            {new Point(50, 250), new Point(450, 250)},
            {new Point(250, 50), new Point(250, 450)},
            {new Point(100, 450), new Point(400, 50)}
        };

        for (Point[] line : testLines) {
            drawDashedLine(clipDemo, (int) line[0].x, (int) line[0].y,
                          (int) line[1].x, (int) line[1].y, 180);

            List<Point[]> clipped = clipLineByPolygon(line[0], line[1], star);
            for (Point[] segment : clipped) {
                drawLine(clipDemo, (int) segment[0].x, (int) segment[0].y,
                        (int) segment[1].x, (int) segment[1].y, 0);
                drawPoint(clipDemo, (int) segment[0].x, (int) segment[0].y, 100, 3);
                drawPoint(clipDemo, (int) segment[1].x, (int) segment[1].y, 100, 3);
            }
        }
        saveImage(clipDemo, "res/hw_clip_arbitrary.png");

        // --- Задание 2: Дуга окружности через Безье ---
        System.out.println("2. Arc with Bezier curves");

        BufferedImage arcDemo = createEmptyImage(imgSize, imgSize);

        // Дуги разных размеров
        List<Point[]> arc1 = createArcWithBezier(250, 250, 180, 0, -Math.PI * 3 / 2);
        for (Point[] curve : arc1) {
            drawBezierCubic(arcDemo, curve[0], curve[1], curve[2], curve[3], 0);
        }

        List<Point[]> arc2 = createArcWithBezier(250, 250, 120, Math.PI / 4, -Math.PI);
        for (Point[] curve : arc2) {
            drawBezierCubic(arcDemo, curve[0], curve[1], curve[2], curve[3], 80);
        }

        List<Point[]> arc3 = createArcWithBezier(250, 250, 60, Math.PI, 0);
        for (Point[] curve : arc3) {
            drawBezierCubic(arcDemo, curve[0], curve[1], curve[2], curve[3], 120);
        }

        drawPoint(arcDemo, 250, 250, 0, 3);
        saveImage(arcDemo, "res/hw_arc_bezier.png");

        // Детальная демонстрация с контрольными точками
        BufferedImage arcDetail = createEmptyImage(imgSize, imgSize);
        List<Point[]> detailArc = createArcWithBezier(250, 250, 150, Math.PI / 6, -Math.PI * 2 / 3);

        for (Point[] curve : detailArc) {
            drawDashedLine(arcDetail, (int) curve[0].x, (int) curve[0].y,
                          (int) curve[1].x, (int) curve[1].y, 180);
            drawDashedLine(arcDetail, (int) curve[1].x, (int) curve[1].y,
                          (int) curve[2].x, (int) curve[2].y, 180);
            drawDashedLine(arcDetail, (int) curve[2].x, (int) curve[2].y,
                          (int) curve[3].x, (int) curve[3].y, 180);

            drawBezierCubic(arcDetail, curve[0], curve[1], curve[2], curve[3], 0);

            drawPoint(arcDetail, (int) curve[0].x, (int) curve[0].y, 0, 5);
            drawPoint(arcDetail, (int) curve[1].x, (int) curve[1].y, 120, 4);
            drawPoint(arcDetail, (int) curve[2].x, (int) curve[2].y, 120, 4);
            drawPoint(arcDetail, (int) curve[3].x, (int) curve[3].y, 0, 5);
        }
        drawPoint(arcDetail, 250, 250, 50, 4);
        saveImage(arcDetail, "res/hw_arc_detail.png");

        // --- Задание 3: Гистограммы HSV ---
        System.out.println("3. HSV Histograms");

        BufferedImage colorImage = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 400; x++) {
            for (int y = 0; y < 300; y++) {
                float hue = (float) x / 400;
                float sat = (float) y / 300;
                float val = 0.7f + 0.3f * (float) Math.sin(x * 0.03 + y * 0.02);
                int rgb = Color.HSBtoRGB(hue, sat, val);
                colorImage.setRGB(x, y, rgb);
            }
        }
        saveImage(colorImage, "res/hw_color_test.png");

        int[][] histograms = buildHSVHistograms(colorImage);

        saveImage(drawHistogram(histograms[0], 400, 200, new Color(200, 80, 80), "Hue (H)"),
                  "res/hw_hist_h.png");
        saveImage(drawHistogram(histograms[1], 400, 200, new Color(80, 200, 80), "Saturation (S)"),
                  "res/hw_hist_s.png");
        saveImage(drawHistogram(histograms[2], 400, 200, new Color(80, 80, 200), "Value (V)"),
                  "res/hw_hist_v.png");

        System.out.println("\nDone!");
    }
}
