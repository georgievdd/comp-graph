import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Лабораторная работа No 2: Алгоритм рассеивания ошибки
 * Преобразование изображения 8 bpp в n bpp (n < 8) с использованием
 * алгоритма рассеяния ошибки Флойда-Стенберга
 */
public class Lab2 extends JFrame {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;

    private BufferedImage originalImage;
    private BufferedImage ditheredImage;
    private BufferedImage ditheredImageAlternating;

    public Lab2() {
        setTitle("Lab 2: Floyd-Steinberg Dithering");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Создаем тестовое изображение
        createTestImage();

        // Применяем алгоритм Floyd-Steinberg
        ditheredImage = floydSteinbergDithering(originalImage, 2); // 2 bpp (4 уровня)

        // Применяем алгоритм с чередующимся направлением сканирования
        ditheredImageAlternating = floydSteinbergDitheringAlternating(originalImage, 2);

        add(new ImagePanel());
    }

    /**
     * Создание тестового изображения с градиентами
     */
    private void createTestImage() {
        int imgWidth = 400;
        int imgHeight = 400;
        originalImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < imgHeight; y++) {
            for (int x = 0; x < imgWidth; x++) {
                // Создаем сложный градиент
                int intensity;

                if (x < imgWidth / 2 && y < imgHeight / 2) {
                    // Верхний левый квадрант: горизонтальный градиент
                    intensity = (255 * x) / (imgWidth / 2);
                } else if (x >= imgWidth / 2 && y < imgHeight / 2) {
                    // Верхний правый квадрант: вертикальный градиент
                    intensity = (255 * y) / (imgHeight / 2);
                } else if (x < imgWidth / 2 && y >= imgHeight / 2) {
                    // Нижний левый квадрант: диагональный градиент
                    int dx = x;
                    int dy = y - imgHeight / 2;
                    intensity = (255 * (dx + dy)) / (imgWidth / 2 + imgHeight / 2);
                } else {
                    // Нижний правый квадрант: радиальный градиент
                    int centerX = 3 * imgWidth / 4;
                    int centerY = 3 * imgHeight / 4;
                    double distance = Math.sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY));
                    double maxDistance = Math.sqrt((imgWidth / 4) * (imgWidth / 4) + (imgHeight / 4) * (imgHeight / 4));
                    intensity = (int) (255 * (1 - Math.min(distance / maxDistance, 1.0)));
                }

                intensity = Math.max(0, Math.min(255, intensity));
                int gray = (intensity << 16) | (intensity << 8) | intensity;
                originalImage.setRGB(x, y, gray);
            }
        }
    }

    /**
     * Алгоритм Floyd-Steinberg dithering
     * @param source исходное изображение
     * @param bitsPerPixel количество бит на пиксель в результате (n < 8)
     * @return изображение после применения dithering
     */
    private BufferedImage floydSteinbergDithering(BufferedImage source, int bitsPerPixel) {
        int width = source.getWidth();
        int height = source.getHeight();

        // Создаем копию для работы (храним значения с плавающей точкой для точности)
        double[][] pixels = new double[height][width];

        // Копируем исходное изображение
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y][x] = source.getRGB(x, y) & 0xFF;
            }
        }

        // Вычисляем количество допустимых уровней
        int levels = (int) Math.pow(2, bitsPerPixel);
        double step = 255.0 / (levels - 1);

        // Применяем Floyd-Steinberg dithering
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double oldPixel = pixels[y][x];

                // Находим ближайший допустимый уровень
                int newPixel = findNearestLevel(oldPixel, levels, step);
                pixels[y][x] = newPixel;

                // Вычисляем ошибку
                double error = oldPixel - newPixel;

                // Распределяем ошибку на соседние пиксели
                // Схема Floyd-Steinberg:
                //          X    7/16
                //   3/16  5/16  1/16

                if (x + 1 < width) {
                    pixels[y][x + 1] += error * 7.0 / 16.0;
                }
                if (y + 1 < height) {
                    if (x - 1 >= 0) {
                        pixels[y + 1][x - 1] += error * 3.0 / 16.0;
                    }
                    pixels[y + 1][x] += error * 5.0 / 16.0;
                    if (x + 1 < width) {
                        pixels[y + 1][x + 1] += error * 1.0 / 16.0;
                    }
                }
            }
        }

        // Создаем результирующее изображение
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = (int) Math.max(0, Math.min(255, pixels[y][x]));
                int gray = (value << 16) | (value << 8) | value;
                result.setRGB(x, y, gray);
            }
        }

        return result;
    }

    /**
     * Алгоритм Floyd-Steinberg с чередующимся направлением сканирования
     * для четных и нечетных строк
     */
    private BufferedImage floydSteinbergDitheringAlternating(BufferedImage source, int bitsPerPixel) {
        int width = source.getWidth();
        int height = source.getHeight();

        double[][] pixels = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y][x] = source.getRGB(x, y) & 0xFF;
            }
        }

        int levels = (int) Math.pow(2, bitsPerPixel);
        double step = 255.0 / (levels - 1);

        for (int y = 0; y < height; y++) {
            if (y % 2 == 0) {
                // Четные строки: слева направо
                for (int x = 0; x < width; x++) {
                    processPixel(pixels, x, y, width, height, levels, step, true);
                }
            } else {
                // Нечетные строки: справа налево
                for (int x = width - 1; x >= 0; x--) {
                    processPixel(pixels, x, y, width, height, levels, step, false);
                }
            }
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = (int) Math.max(0, Math.min(255, pixels[y][x]));
                int gray = (value << 16) | (value << 8) | value;
                result.setRGB(x, y, gray);
            }
        }

        return result;
    }

    /**
     * Обработка одного пикселя для алгоритма с чередующимся направлением
     */
    private void processPixel(double[][] pixels, int x, int y, int width, int height,
                              int levels, double step, boolean leftToRight) {
        double oldPixel = pixels[y][x];
        int newPixel = findNearestLevel(oldPixel, levels, step);
        pixels[y][x] = newPixel;
        double error = oldPixel - newPixel;

        if (leftToRight) {
            // Распространение ошибки слева направо
            if (x + 1 < width) {
                pixels[y][x + 1] += error * 7.0 / 16.0;
            }
            if (y + 1 < height) {
                if (x - 1 >= 0) {
                    pixels[y + 1][x - 1] += error * 3.0 / 16.0;
                }
                pixels[y + 1][x] += error * 5.0 / 16.0;
                if (x + 1 < width) {
                    pixels[y + 1][x + 1] += error * 1.0 / 16.0;
                }
            }
        } else {
            // Распространение ошибки справа налево (зеркально)
            if (x - 1 >= 0) {
                pixels[y][x - 1] += error * 7.0 / 16.0;
            }
            if (y + 1 < height) {
                if (x + 1 < width) {
                    pixels[y + 1][x + 1] += error * 3.0 / 16.0;
                }
                pixels[y + 1][x] += error * 5.0 / 16.0;
                if (x - 1 >= 0) {
                    pixels[y + 1][x - 1] += error * 1.0 / 16.0;
                }
            }
        }
    }

    /**
     * Находит ближайший допустимый уровень яркости
     */
    private int findNearestLevel(double value, int levels, double step) {
        int level = (int) Math.round(value / step);
        level = Math.max(0, Math.min(levels - 1, level));
        return (int) (level * step);
    }

    /**
     * Панель для отображения изображений
     */
    private class ImagePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int margin = 20;
            int imgWidth = (getWidth() - 4 * margin) / 3;
            int imgHeight = getHeight() - 2 * margin - 30;

            // Рисуем исходное изображение
            g.drawImage(originalImage, margin, margin, imgWidth, imgHeight, null);
            g.drawString("Original (8 bpp)", margin, getHeight() - 10);

            // Рисуем результат стандартного Floyd-Steinberg
            g.drawImage(ditheredImage, 2 * margin + imgWidth, margin, imgWidth, imgHeight, null);
            g.drawString("Floyd-Steinberg (2 bpp)", 2 * margin + imgWidth, getHeight() - 10);

            // Рисуем результат с чередующимся направлением
            g.drawImage(ditheredImageAlternating, 3 * margin + 2 * imgWidth, margin, imgWidth, imgHeight, null);
            g.drawString("Alternating scan (2 bpp)", 3 * margin + 2 * imgWidth, getHeight() - 10);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Lab2 frame = new Lab2();
            frame.setVisible(true);
        });
    }
}
