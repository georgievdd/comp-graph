import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

/**
 * Лабораторная работа No 2: Алгоритм рассеивания ошибки
 * Преобразование изображения 8 bpp в n bpp (n < 8) с использованием
 * алгоритма рассеяния ошибки Флойда-Стенберга
 */
public class Lab2 extends JFrame {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;

    private static final double ERR_7_16 = 7.0 / 16.0;
    private static final double ERR_5_16 = 5.0 / 16.0;
    private static final double ERR_3_16 = 3.0 / 16.0;
    private static final double ERR_1_16 = 1.0 / 16.0;

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
     * Алгоритм Floyd-Steinberg dithering (МАКСИМАЛЬНО ОПТИМИЗИРОВАННАЯ ВЕРСИЯ)
     * Оптимизации:
     * 1. WritableRaster для прямого доступа к пикселям
     * 2. Предвычисленные константы коэффициентов
     * 3. Построчная обработка (O(width) памяти вместо O(width×height))
     * 4. Один проход по изображению вместо трёх
     * @param source исходное изображение
     * @param bitsPerPixel количество бит на пиксель в результате (n < 8)
     * @return изображение после применения dithering
     */
    private BufferedImage floydSteinbergDithering(BufferedImage source, int bitsPerPixel) {
        int width = source.getWidth();
        int height = source.getHeight();

        // Создаем результирующее изображение
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster sourceRaster = source.getRaster();
        WritableRaster resultRaster = result.getRaster();

        // Вычисляем количество допустимых уровней
        int levels = (int) Math.pow(2, bitsPerPixel);
        double step = 255.0 / (levels - 1);

        double[] currentRow = new double[width];
        double[] nextRow = new double[width];

        for (int x = 0; x < width; x++) {
            currentRow[x] = sourceRaster.getSample(x, 0, 0);
        }

        for (int y = 0; y < height; y++) {
            if (y + 1 < height) {
                for (int x = 0; x < width; x++) {
                    nextRow[x] = sourceRaster.getSample(x, y + 1, 0);
                }
            }

            // Обрабатываем текущую строку
            for (int x = 0; x < width; x++) {
                double oldPixel = currentRow[x];

                int newPixel = findNearestLevel(oldPixel, levels, step);

                resultRaster.setSample(x, y, 0, newPixel);

                // Вычисляем ошибку
                double error = oldPixel - newPixel;

                // Распределяем ошибку на соседние пиксели
                if (x + 1 < width) {
                    currentRow[x + 1] += error * ERR_7_16;
                }
                if (y + 1 < height) {
                    if (x - 1 >= 0) {
                        nextRow[x - 1] += error * ERR_3_16;
                    }
                    nextRow[x] += error * ERR_5_16;
                    if (x + 1 < width) {
                        nextRow[x + 1] += error * ERR_1_16;
                    }
                }
            }

            // Переключаем буферы (избегаем копирования)
            double[] temp = currentRow;
            currentRow = nextRow;
            nextRow = temp;

            // Очищаем буфер для следующей итерации
            if (y + 2 < height) {
                java.util.Arrays.fill(nextRow, 0);
            }
        }

        return result;
    }

    /**
     * Алгоритм Floyd-Steinberg с чередующимся направлением сканирования
     */
    private BufferedImage floydSteinbergDitheringAlternating(BufferedImage source, int bitsPerPixel) {
        int width = source.getWidth();
        int height = source.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster sourceRaster = source.getRaster();
        WritableRaster resultRaster = result.getRaster();

        int levels = (int) Math.pow(2, bitsPerPixel);
        double step = 255.0 / (levels - 1);

        double[] currentRow = new double[width];
        double[] nextRow = new double[width];

        // Инициализируем первую строку
        for (int x = 0; x < width; x++) {
            currentRow[x] = sourceRaster.getSample(x, 0, 0);
        }

        // Обрабатываем изображение построчно
        for (int y = 0; y < height; y++) {
            boolean leftToRight = (y % 2 == 0);

            // Загружаем следующую строку (если есть)
            if (y + 1 < height) {
                for (int x = 0; x < width; x++) {
                    nextRow[x] = sourceRaster.getSample(x, y + 1, 0);
                }
            }

            if (leftToRight) {
                // Четные строки: слева направо
                for (int x = 0; x < width; x++) {
                    processPixelOptimized(currentRow, nextRow, resultRaster, x, y, width, height, levels, step, true);
                }
            } else {
                // Нечетные строки: справа налево
                for (int x = width - 1; x >= 0; x--) {
                    processPixelOptimized(currentRow, nextRow, resultRaster, x, y, width, height, levels, step, false);
                }
            }

            // Переключаем буферы
            double[] temp = currentRow;
            currentRow = nextRow;
            nextRow = temp;

            // Очищаем буфер для следующей итерации
            if (y + 2 < height) {
                java.util.Arrays.fill(nextRow, 0);
            }
        }

        return result;
    }

    private void processPixelOptimized(double[] currentRow, double[] nextRow, WritableRaster resultRaster,
                                       int x, int y, int width, int height,
                                       int levels, double step, boolean leftToRight) {
        double oldPixel = currentRow[x];
        int newPixel = findNearestLevel(oldPixel, levels, step);

        // Сразу записываем результат
        resultRaster.setSample(x, y, 0, newPixel);

        double error = oldPixel - newPixel;

        if (leftToRight) {
            // Распространение ошибки слева направо
            if (x + 1 < width) {
                currentRow[x + 1] += error * ERR_7_16;
            }
            if (y + 1 < height) {
                if (x - 1 >= 0) {
                    nextRow[x - 1] += error * ERR_3_16;
                }
                nextRow[x] += error * ERR_5_16;
                if (x + 1 < width) {
                    nextRow[x + 1] += error * ERR_1_16;
                }
            }
        } else {
            // Распространение ошибки справа налево (зеркально)
            if (x - 1 >= 0) {
                currentRow[x - 1] += error * ERR_7_16;
            }
            if (y + 1 < height) {
                if (x + 1 < width) {
                    nextRow[x + 1] += error * ERR_3_16;
                }
                nextRow[x] += error * ERR_5_16;
                if (x - 1 >= 0) {
                    nextRow[x - 1] += error * ERR_1_16;
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

    /**
     * Загрузка изображения из файла
     */
    public static BufferedImage loadImage(String filepath) {
        try {
            File file = new File(filepath);
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                System.err.println("Не удалось загрузить изображение: " + filepath);
                return null;
            }
            return img;
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке изображения: " + e.getMessage());
            return null;
        }
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
            System.err.println("Ошибка при сохранении изображения: " + e.getMessage());
            return false;
        }
    }

    /**
     * Конвертация изображения в градации серого
     */
    public static BufferedImage convertToGrayscale(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = gray.getRaster();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int grayValue = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                raster.setSample(x, y, 0, grayValue);
            }
        }

        return gray;
    }

    public static void main(String[] args) {
        System.out.println("=== Лабораторная работа №2: Floyd-Steinberg Dithering ===\n");

        // Демонстрация работы с реальной фотографией
        System.out.println("Обработка реальной фотографии с разным количеством бит на пиксель:\n");
        String inputPath = "res/test_img.png";

        BufferedImage originalPhoto = loadImage(inputPath);
        if (originalPhoto != null) {
            // Конвертируем в grayscale
            BufferedImage grayPhoto = convertToGrayscale(originalPhoto);
            saveImage(grayPhoto, "res/test_photo_original_gray.png");

            System.out.println("Загружено: " + grayPhoto.getWidth() + "x" + grayPhoto.getHeight() + " пикселей\n");

            // Создаем временный объект для использования методов dithering
            Lab2 lab2 = new Lab2();

            // 1 bpp (2 уровня - черный и белый)
            System.out.println("1. Dithering 1 bpp (2 уровня):");
            BufferedImage dithered1bpp = lab2.floydSteinbergDithering(grayPhoto, 1);
            saveImage(dithered1bpp, "res/test_photo_1bpp.png");

            BufferedImage dithered1bppAlt = lab2.floydSteinbergDitheringAlternating(grayPhoto, 1);
            saveImage(dithered1bppAlt, "res/test_photo_1bpp_alternating.png");

            // 2 bpp (4 уровня)
            System.out.println("\n2. Dithering 2 bpp (4 уровня):");
            BufferedImage dithered2bpp = lab2.floydSteinbergDithering(grayPhoto, 2);
            saveImage(dithered2bpp, "res/test_photo_2bpp.png");

            BufferedImage dithered2bppAlt = lab2.floydSteinbergDitheringAlternating(grayPhoto, 2);
            saveImage(dithered2bppAlt, "res/test_photo_2bpp_alternating.png");

            // 3 bpp (8 уровней)
            System.out.println("\n3. Dithering 3 bpp (8 уровней):");
            BufferedImage dithered3bpp = lab2.floydSteinbergDithering(grayPhoto, 3);
            saveImage(dithered3bpp, "res/test_photo_3bpp.png");

            BufferedImage dithered3bppAlt = lab2.floydSteinbergDitheringAlternating(grayPhoto, 3);
            saveImage(dithered3bppAlt, "res/test_photo_3bpp_alternating.png");

            System.out.println("\n=== Результаты сохранены ===\n");
        }

        System.out.println("=== Запуск GUI с синтетическими изображениями ===\n");

        SwingUtilities.invokeLater(() -> {
            Lab2 frame = new Lab2();
            frame.setVisible(true);
        });
    }
}
