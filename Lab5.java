import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class Lab5 extends JFrame {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    private AnimationPanel animationPanel;
    private Timer animationTimer;
    private double t = 0;

    static class Vector3D {
        double x, y, z;

        public Vector3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vector3D add(Vector3D v) {
            return new Vector3D(x + v.x, y + v.y, z + v.z);
        }

        public Vector3D subtract(Vector3D v) {
            return new Vector3D(x - v.x, y - v.y, z - v.z);
        }

        public Vector3D scale(double s) {
            return new Vector3D(x * s, y * s, z * s);
        }

        public double dot(Vector3D v) {
            return x * v.x + y * v.y + z * v.z;
        }

        public Vector3D cross(Vector3D v) {
            return new Vector3D(
                y * v.z - z * v.y,
                z * v.x - x * v.z,
                x * v.y - y * v.x
            );
        }

        public double length() {
            return Math.sqrt(x * x + y * y + z * z);
        }

        public Vector3D normalize() {
            double len = length();
            if (len < 1e-10) return new Vector3D(0, 0, 0);
            return new Vector3D(x / len, y / len, z / len);
        }
    }

    static class Parallelepiped {
        Vector3D[] vertices;
        int[][] edges;
        int[][] faces;
        Vector3D center;

        public Parallelepiped(double width, double height, double depth) {
            vertices = new Vector3D[8];
            double w2 = width / 2;
            double h2 = height / 2;
            double d2 = depth / 2;

            vertices[0] = new Vector3D(-w2, -h2, -d2);
            vertices[1] = new Vector3D( w2, -h2, -d2);
            vertices[2] = new Vector3D( w2,  h2, -d2);
            vertices[3] = new Vector3D(-w2,  h2, -d2);
            vertices[4] = new Vector3D(-w2, -h2,  d2);
            vertices[5] = new Vector3D( w2, -h2,  d2);
            vertices[6] = new Vector3D( w2,  h2,  d2);
            vertices[7] = new Vector3D(-w2,  h2,  d2);

            edges = new int[][] {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
            };

            faces = new int[][] {
                {0, 3, 2, 1},  // back face
                {4, 5, 6, 7},  // front face
                {0, 1, 5, 4},  // bottom face
                {3, 7, 6, 2},  // top face
                {0, 4, 7, 3},  // left face
                {1, 2, 6, 5}   // right face
            };

            center = new Vector3D(0, 0, 0);
        }

        public Parallelepiped rotate(Vector3D axis, double angle) {
            Parallelepiped result = new Parallelepiped(0, 0, 0);
            result.edges = this.edges;
            result.faces = this.faces;
            result.vertices = new Vector3D[8];
            result.center = this.center;

            Vector3D a = axis.normalize();
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double oneMinusCos = 1 - cos;

            for (int i = 0; i < 8; i++) {
                Vector3D v = vertices[i];
                double x = v.x * (cos + a.x * a.x * oneMinusCos) +
                          v.y * (a.x * a.y * oneMinusCos - a.z * sin) +
                          v.z * (a.x * a.z * oneMinusCos + a.y * sin);

                double y = v.x * (a.y * a.x * oneMinusCos + a.z * sin) +
                          v.y * (cos + a.y * a.y * oneMinusCos) +
                          v.z * (a.y * a.z * oneMinusCos - a.x * sin);

                double z = v.x * (a.z * a.x * oneMinusCos - a.y * sin) +
                          v.y * (a.z * a.y * oneMinusCos + a.x * sin) +
                          v.z * (cos + a.z * a.z * oneMinusCos);

                result.vertices[i] = new Vector3D(x, y, z);
            }

            return result;
        }

        public Parallelepiped translate(Vector3D offset) {
            Parallelepiped result = new Parallelepiped(0, 0, 0);
            result.edges = this.edges;
            result.faces = this.faces;
            result.vertices = new Vector3D[8];
            result.center = this.center.add(offset);

            for (int i = 0; i < 8; i++) {
                result.vertices[i] = vertices[i].add(offset);
            }

            return result;
        }
    }

    static class Point2D {
        int x, y;

        public Point2D(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static Vector3D bezierCubic3D(Vector3D p0, Vector3D p1, Vector3D p2, Vector3D p3, double t) {
        double mt = 1 - t;
        double mt2 = mt * mt;
        double mt3 = mt2 * mt;
        double t2 = t * t;
        double t3 = t2 * t;

        double x = mt3 * p0.x + 3 * mt2 * t * p1.x + 3 * mt * t2 * p2.x + t3 * p3.x;
        double y = mt3 * p0.y + 3 * mt2 * t * p1.y + 3 * mt * t2 * p2.y + t3 * p3.y;
        double z = mt3 * p0.z + 3 * mt2 * t * p1.z + 3 * mt * t2 * p2.z + t3 * p3.z;

        return new Vector3D(x, y, z);
    }

    public static Point2D perspectiveProjection(Vector3D point, Vector3D camera, int screenWidth, int screenHeight) {
        double k = camera.z;

        if (Math.abs(k - point.z) < 1e-10) {
            return new Point2D(screenWidth / 2, screenHeight / 2);
        }

        double scale = k / (k - point.z);
        int x = (int) (screenWidth / 2 + point.x * scale);
        int y = (int) (screenHeight / 2 - point.y * scale);
        return new Point2D(x, y);
    }

    public static void drawLine(Graphics2D g, Point2D p0, Point2D p1, Color color) {
        g.setColor(color);
        g.drawLine(p0.x, p0.y, p1.x, p1.y);
    }

    public static void fillPolygon(Graphics2D g, Point2D[] points, Color color) {
        int[] xPoints = new int[points.length];
        int[] yPoints = new int[points.length];

        for (int i = 0; i < points.length; i++) {
            xPoints[i] = points[i].x;
            yPoints[i] = points[i].y;
        }

        g.setColor(color);
        g.fillPolygon(xPoints, yPoints, points.length);
    }

    public static Vector3D getFaceNormal(Parallelepiped box, int[] face) {
        Vector3D v0 = box.vertices[face[0]];
        Vector3D v1 = box.vertices[face[1]];
        Vector3D v2 = box.vertices[face[2]];

        Vector3D edge1 = v1.subtract(v0);
        Vector3D edge2 = v2.subtract(v0);
        return edge1.cross(edge2).normalize();
    }

    public static Color gouraudShading(Vector3D normal, Vector3D lightPos, Vector3D vertexPos,
                                       Color baseColor, Vector3D viewPos) {
        Vector3D lightDir = lightPos.subtract(vertexPos).normalize();
        Vector3D viewDir = viewPos.subtract(vertexPos).normalize();

        double diffuse = Math.max(0, normal.dot(lightDir));

        Vector3D reflected = normal.scale(2 * normal.dot(lightDir)).subtract(lightDir).normalize();
        double specular = Math.pow(Math.max(0, reflected.dot(viewDir)), 32);

        double ambient = 0.2;
        double intensity = ambient + 0.6 * diffuse + 0.2 * specular;
        intensity = Math.min(1.0, Math.max(0.0, intensity));

        int r = (int) (baseColor.getRed() * intensity);
        int g = (int) (baseColor.getGreen() * intensity);
        int b = (int) (baseColor.getBlue() * intensity);

        return new Color(r, g, b);
    }

    public static void drawParallelepiped(Graphics2D g, Parallelepiped box, Vector3D camera,
                                         Vector3D lightPos, int w, int h) {
        Vector3D viewDir = new Vector3D(0, 0, -1);

        // Sort faces by average Z (painter's algorithm)
        Integer[] faceOrder = new Integer[box.faces.length];
        double[] avgZ = new double[box.faces.length];

        for (int i = 0; i < box.faces.length; i++) {
            faceOrder[i] = i;
            double sum = 0;
            for (int vi : box.faces[i]) {
                sum += box.vertices[vi].z;
            }
            avgZ[i] = sum / box.faces[i].length;
        }

        // Sort back to front
        for (int i = 0; i < faceOrder.length - 1; i++) {
            for (int j = i + 1; j < faceOrder.length; j++) {
                if (avgZ[faceOrder[i]] > avgZ[faceOrder[j]]) {
                    int temp = faceOrder[i];
                    faceOrder[i] = faceOrder[j];
                    faceOrder[j] = temp;
                }
            }
        }

        // Draw faces
        Color[] faceColors = {
            new Color(200, 100, 100),
            new Color(100, 200, 100),
            new Color(100, 100, 200),
            new Color(200, 200, 100),
            new Color(200, 100, 200),
            new Color(100, 200, 200)
        };

        for (int fi : faceOrder) {
            int[] face = box.faces[fi];
            Vector3D normal = getFaceNormal(box, face);

            // Back-face culling
            if (normal.dot(viewDir) >= 0) continue;

            // Calculate center of face for lighting
            Vector3D faceCenter = new Vector3D(0, 0, 0);
            for (int vi : face) {
                faceCenter = faceCenter.add(box.vertices[vi]);
            }
            faceCenter = faceCenter.scale(1.0 / face.length);

            Color shadedColor = gouraudShading(normal, lightPos, faceCenter, faceColors[fi], camera);

            Point2D[] projectedPoints = new Point2D[face.length];
            for (int i = 0; i < face.length; i++) {
                projectedPoints[i] = perspectiveProjection(box.vertices[face[i]], camera, w, h);
            }

            fillPolygon(g, projectedPoints, shadedColor);

            // Draw edges
            g.setColor(Color.BLACK);
            for (int i = 0; i < face.length; i++) {
                int next = (i + 1) % face.length;
                drawLine(g, projectedPoints[i], projectedPoints[next], Color.BLACK);
            }
        }
    }

    private class AnimationPanel extends JPanel {
        private Vector3D[] bezierControlPoints;

        public AnimationPanel() {
            bezierControlPoints = new Vector3D[] {
                new Vector3D(-150, -100, -50),
                new Vector3D(-50, 100, 50),
                new Vector3D(50, -100, 100),
                new Vector3D(150, 100, -50)
            };
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            setBackground(Color.WHITE);

            int cellWidth = getWidth() / 2;
            int cellHeight = getHeight() / 2;

            drawDemo(g2d, 0, 0, cellWidth, cellHeight, "Animation with Gouraud Shading", t);
            drawDemo(g2d, cellWidth, 0, cellWidth, cellHeight, "Different View Angle", t);
            drawDemo(g2d, 0, cellHeight, cellWidth, cellHeight, "Different Light Position", t);
            drawTrajectory(g2d, cellWidth, cellHeight, cellWidth, cellHeight, "Bezier Trajectory");
        }

        private void drawDemo(Graphics2D g, int offsetX, int offsetY, int w, int h,
                             String title, double time) {
            g.translate(offsetX, offsetY);

            Vector3D position = bezierCubic3D(
                bezierControlPoints[0],
                bezierControlPoints[1],
                bezierControlPoints[2],
                bezierControlPoints[3],
                time
            );

            Parallelepiped box = new Parallelepiped(60, 40, 30);

            Vector3D rotationAxis = position.normalize();
            if (rotationAxis.length() < 0.01) {
                rotationAxis = new Vector3D(0, 1, 0);
            }
            double rotationAngle = time * Math.PI * 4;

            box = box.rotate(rotationAxis, rotationAngle);
            box = box.translate(position);

            Vector3D camera, lightPos;
            if (title.contains("Different View")) {
                camera = new Vector3D(100, 50, 500);
            } else {
                camera = new Vector3D(0, 0, 500);
            }

            if (title.contains("Different Light")) {
                lightPos = new Vector3D(-200, 200, 300);
            } else {
                lightPos = new Vector3D(200, 200, 300);
            }

            drawParallelepiped(g, box, camera, lightPos, w, h);

            g.setColor(Color.BLACK);
            g.drawString(title, 10, 20);

            g.translate(-offsetX, -offsetY);
        }

        private void drawTrajectory(Graphics2D g, int offsetX, int offsetY, int w, int h, String title) {
            g.translate(offsetX, offsetY);

            Vector3D camera = new Vector3D(0, 0, 500);

            g.setColor(new Color(200, 200, 255));
            Point2D prevPoint = null;
            for (double tt = 0; tt <= 1.0; tt += 0.01) {
                Vector3D pos = bezierCubic3D(
                    bezierControlPoints[0],
                    bezierControlPoints[1],
                    bezierControlPoints[2],
                    bezierControlPoints[3],
                    tt
                );
                Point2D point = perspectiveProjection(pos, camera, w, h);

                if (prevPoint != null) {
                    g.drawLine(prevPoint.x, prevPoint.y, point.x, point.y);
                }
                prevPoint = point;
            }

            Vector3D currentPos = bezierCubic3D(
                bezierControlPoints[0],
                bezierControlPoints[1],
                bezierControlPoints[2],
                bezierControlPoints[3],
                t
            );
            Point2D currentPoint = perspectiveProjection(currentPos, camera, w, h);
            g.setColor(Color.RED);
            g.fillOval(currentPoint.x - 5, currentPoint.y - 5, 10, 10);

            g.setColor(Color.BLACK);
            g.drawString(title, 10, 20);

            g.translate(-offsetX, -offsetY);
        }
    }

    public Lab5() {
        setTitle("Lab 5: 3D Animation with Bezier Trajectory and Gouraud Shading");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        animationPanel = new AnimationPanel();
        add(animationPanel);

        animationTimer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                t += 0.01;
                if (t > 1.0) t = 0;
                animationPanel.repaint();
            }
        });
        animationTimer.start();
    }

    public static void saveImage(BufferedImage img, String filepath) {
        try {
            java.io.File outputFile = new java.io.File(filepath);
            javax.imageio.ImageIO.write(img, "png", outputFile);
            System.out.println("   Сохранено: " + filepath);
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Лабораторная работа №5: 3D анимация с закраской Гуро ===\n");

        System.out.println("Создание демонстрационных изображений:\n");

        Vector3D[] bezierCP = {
            new Vector3D(-150, -100, -50),
            new Vector3D(-50, 100, 50),
            new Vector3D(50, -100, 100),
            new Vector3D(150, 100, -50)
        };

        // Image 1: Object at t=0.3
        BufferedImage img1 = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g1 = img1.createGraphics();
        g1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g1.setColor(Color.WHITE);
        g1.fillRect(0, 0, 600, 600);

        Vector3D pos1 = bezierCubic3D(bezierCP[0], bezierCP[1], bezierCP[2], bezierCP[3], 0.3);
        Parallelepiped box1 = new Parallelepiped(60, 40, 30);
        box1 = box1.rotate(pos1.normalize(), 0.3 * Math.PI * 4);
        box1 = box1.translate(pos1);
        drawParallelepiped(g1, box1, new Vector3D(0, 0, 500), new Vector3D(200, 200, 300), 600, 600);
        g1.dispose();
        saveImage(img1, "res/lab5_animation_t03.png");

        // Image 2: Object at t=0.7
        BufferedImage img2 = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img2.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 600, 600);

        Vector3D pos2 = bezierCubic3D(bezierCP[0], bezierCP[1], bezierCP[2], bezierCP[3], 0.7);
        Parallelepiped box2 = new Parallelepiped(60, 40, 30);
        box2 = box2.rotate(pos2.normalize(), 0.7 * Math.PI * 4);
        box2 = box2.translate(pos2);
        drawParallelepiped(g2, box2, new Vector3D(0, 0, 500), new Vector3D(200, 200, 300), 600, 600);
        g2.dispose();
        saveImage(img2, "res/lab5_animation_t07.png");

        // Image 3: Bezier trajectory
        BufferedImage img3 = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g3 = img3.createGraphics();
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g3.setColor(Color.WHITE);
        g3.fillRect(0, 0, 600, 600);

        Vector3D camera = new Vector3D(0, 0, 500);
        g3.setColor(new Color(200, 200, 255));
        Point2D prevPoint = null;
        for (double tt = 0; tt <= 1.0; tt += 0.01) {
            Vector3D pos = bezierCubic3D(bezierCP[0], bezierCP[1], bezierCP[2], bezierCP[3], tt);
            Point2D point = perspectiveProjection(pos, camera, 600, 600);
            if (prevPoint != null) {
                g3.drawLine(prevPoint.x, prevPoint.y, point.x, point.y);
            }
            prevPoint = point;
        }

        // Draw control points
        g3.setColor(Color.RED);
        for (Vector3D cp : bezierCP) {
            Point2D cpPoint = perspectiveProjection(cp, camera, 600, 600);
            g3.fillOval(cpPoint.x - 5, cpPoint.y - 5, 10, 10);
        }

        g3.dispose();
        saveImage(img3, "res/lab5_bezier_trajectory.png");

        // Image 4: Gouraud shading comparison
        BufferedImage img4 = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g4 = img4.createGraphics();
        g4.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g4.setColor(Color.WHITE);
        g4.fillRect(0, 0, 600, 600);

        Vector3D pos4 = new Vector3D(0, 0, 0);
        Parallelepiped box4 = new Parallelepiped(60, 40, 30);
        box4 = box4.rotate(new Vector3D(1, 1, 0.5).normalize(), Math.PI / 4);
        box4 = box4.translate(pos4);
        drawParallelepiped(g4, box4, new Vector3D(0, 0, 500), new Vector3D(200, 200, 300), 600, 600);
        g4.dispose();
        saveImage(img4, "res/lab5_gouraud_shading.png");

        System.out.println("\n=== Запуск GUI ===\n");

        SwingUtilities.invokeLater(() -> {
            Lab5 lab = new Lab5();
            lab.setVisible(true);
        });
    }
}
