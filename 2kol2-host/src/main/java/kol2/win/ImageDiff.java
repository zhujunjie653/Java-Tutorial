package kol2.win;

import java.awt.image.BufferedImage;

/** 把两张图缩小后比较，用来粗判终场模板。不是文字识别。 */
public final class ImageDiff {
    private ImageDiff() {
    }

    /** 平均绝对差。0 表示抽样点相同，越接近 1 差得越多。 */
    public static double mad(BufferedImage left, BufferedImage right) {
        if (left == null || right == null || left.getWidth() < 1 || right.getWidth() < 1
                || left.getHeight() < 1 || right.getHeight() < 1) {
            return 1;
        }
        int samples = 16;
        long sum = 0;
        for (int y = 0; y < samples; y++) {
            for (int x = 0; x < samples; x++) {
                int ax = Math.min(left.getWidth() - 1, x * left.getWidth() / samples);
                int ay = Math.min(left.getHeight() - 1, y * left.getHeight() / samples);
                int bx = Math.min(right.getWidth() - 1, x * right.getWidth() / samples);
                int by = Math.min(right.getHeight() - 1, y * right.getHeight() / samples);
                sum += Math.abs(gray(left.getRGB(ax, ay)) - gray(right.getRGB(bx, by)));
            }
        }
        return (sum / (double) (samples * samples)) / 255.0;
    }

    private static int gray(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return (r + g + b) / 3;
    }
}
