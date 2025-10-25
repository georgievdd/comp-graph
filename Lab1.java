import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Лабораторная работа No 1: Смешивание изображений
 * 1. Вывод на экран круглого полутонового изображения
 * 2. Смешивание (blending) двух изображений 8 bpp с использованием альфа-канала
 */
public class Lab1 extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private BufferedImage image1;
    private BufferedImage image2;
    private BufferedImage alphaChannel;
    private BufferedImage blendedImage;

    public Lab1() {
        setTitle("Lab 1: Image Blending");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Создаем тестовые изображения
        createTestImages();

        // Выполняем смешивание
        blendImages();

        add(new ImagePanel());
    }

    /**
     * Создание круглого полутонового изображения
     */
    private BufferedImage createCircularGrayscaleImage(int width, int height, int centerX, int centerY, int radius) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double dx = x - centerX;
                double dy = y - centerY;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance <= radius) {
                    // Градиент от центра к краю
                    int intensity = (int) (255 * (1 - distance / radius));
                    int gray = (intensity << 16) | (intensity << 8) | intensity;
                    img.setRGB(x, y, gray);
                } else {
                    img.setRGB(x, y, 0); // Черный фон
                }
            }
        }

        return img;
    }

    /**
     * Создание тестовых изображений для демонстрации
     */
    private void createTestImages() {
        int imgWidth = 400;
        int imgHeight = 400;

        // Изображение 1: круглое полутоновое с градиентом
        image1 = createCircularGrayscaleImage(imgWidth, imgHeight, imgWidth / 2, imgHeight / 2, 150);

        // Изображение 2: другое круглое изображение с другим центром
        image2 = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < imgHeight; y++) {
            for (int x = 0; x < imgWidth; x++) {
                // Вертикальный градиент
                int intensity = (255 * y) / imgHeight;
                int gray = (intensity << 16) | (intensity << 8) | intensity;
                image2.setRGB(x, y, gray);
            }
        }

        // Альфа-канал: круглая маска
        alphaChannel = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
        int centerX = imgWidth / 2;
        int centerY = imgHeight / 2;
        int radius = 180;

        for (int y = 0; y < imgHeight; y++) {
            for (int x = 0; x < imgWidth; x++) {
                double dx = x - centerX;
                double dy = y - centerY;
                double distance = Math.sqrt(dx * dx + dy * dy);

                int alpha;
                if (distance <= radius) {
                    // Плавный переход альфа-канала
                    alpha = (int) (255 * distance / radius);
                } else {
                    alpha = 255;
                }

                int gray = (alpha << 16) | (alpha << 8) | alpha;
                alphaChannel.setRGB(x, y, gray);
            }
        }
    }

    /**
     * Смешивание двух изображений 8 bpp с использованием третьего изображения как альфа-канала
     * Формула: result = img1 * (1 - alpha) + img2 * alpha
     */
    private void blendImages() {
        int width = image1.getWidth();
        int height = image1.getHeight();

        blendedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Получаем значения пикселей (в диапазоне 0-255)
                int pixel1 = image1.getRGB(x, y) & 0xFF;
                int pixel2 = image2.getRGB(x, y) & 0xFF;
                int alpha = alphaChannel.getRGB(x, y) & 0xFF;

                // Нормализуем альфа к диапазону [0, 1]
                double alphaValue = alpha / 255.0;

                // Выполняем смешивание
                int blended = (int) (pixel1 * (1 - alphaValue) + pixel2 * alphaValue);
                blended = Math.max(0, Math.min(255, blended)); // Ограничиваем диапазон

                int gray = (blended << 16) | (blended << 8) | blended;
                blendedImage.setRGB(x, y, gray);
            }
        }
    }

    /**
     * Зеркальное отражение изображения по горизонтали
     */
    public static BufferedImage flipHorizontal(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage flipped = new BufferedImage(width, height, img.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                flipped.setRGB(width - 1 - x, y, img.getRGB(x, y));
            }
        }

        return flipped;
    }

    /**
     * Транспонирование изображения
     */
    public static BufferedImage transpose(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage transposed = new BufferedImage(height, width, img.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                transposed.setRGB(y, x, img.getRGB(x, y));
            }
        }

        return transposed;
    }

    /**
     * Панель для отображения изображений
     */
    private class ImagePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int margin = 10;
            int imgWidth = (getWidth() - 3 * margin) / 2;
            int imgHeight = (getHeight() - 3 * margin) / 2;

            // Рисуем исходные изображения и результат
            g.drawImage(image1, margin, margin, imgWidth, imgHeight, null);
            g.drawString("Image 1", margin, margin + imgHeight + 15);

            g.drawImage(image2, imgWidth + 2 * margin, margin, imgWidth, imgHeight, null);
            g.drawString("Image 2", imgWidth + 2 * margin, margin + imgHeight + 15);

            g.drawImage(alphaChannel, margin, imgHeight + 2 * margin, imgWidth, imgHeight, null);
            g.drawString("Alpha Channel", margin, 2 * imgHeight + 2 * margin + 15);

            g.drawImage(blendedImage, imgWidth + 2 * margin, imgHeight + 2 * margin, imgWidth, imgHeight, null);
            g.drawString("Blended Result", imgWidth + 2 * margin, 2 * imgHeight + 2 * margin + 15);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Lab1 frame = new Lab1();
            frame.setVisible(true);
        });
    }
}
