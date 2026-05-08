import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class PrepareReadmeImages {
    private static final int SCREENSHOT_WIDTH = 432;
    private static final int ICON_SIZE = 128;

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : ".");
        Path input = root.resolve("build/readme-input");
        Path out = root.resolve("docs/images");
        Files.createDirectories(out);

        writeSplitScreenshot(input, out, "month-view");
        writeSplitScreenshot(input, out, "year-view");
        writeSplitScreenshot(input, out, "drawer");
        ImageIO.write(
            resize(transparentBlackCorners(readArgb(input.resolve("app-icon.png"))), ICON_SIZE, ICON_SIZE),
            "png",
            out.resolve("app-icon.png").toFile()
        );
    }

    private static void writeSplitScreenshot(Path inputDir, Path outputDir, String name) throws Exception {
        BufferedImage light = resizeToWidth(readRgb(inputDir.resolve(name + "-light.png")), SCREENSHOT_WIDTH);
        BufferedImage dark = resizeToWidth(readRgb(inputDir.resolve(name + "-dark.png")), SCREENSHOT_WIDTH);
        ImageIO.write(split(light, dark), "png", outputDir.resolve(name + ".png").toFile());
    }

    private static BufferedImage split(BufferedImage light, BufferedImage dark) {
        int width = light.getWidth();
        int height = light.getHeight();
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        double leftBoundary = height * 0.47;
        double rightBoundary = height * 0.57;
        double feather = Math.max(8.0, height * 0.012);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double boundary = leftBoundary + (rightBoundary - leftBoundary) * x / Math.max(1, width - 1);
                double t = clamp((y - boundary + feather) / (feather * 2.0), 0.0, 1.0);
                out.setRGB(x, y, blend(light.getRGB(x, y), dark.getRGB(x, y), smoothstep(t)));
            }
        }
        return out;
    }

    private static BufferedImage readRgb(Path path) throws Exception {
        BufferedImage source = ImageIO.read(path.toFile());
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private static BufferedImage readArgb(Path path) throws Exception {
        BufferedImage source = ImageIO.read(path.toFile());
        BufferedImage argb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return argb;
    }

    private static BufferedImage resizeToWidth(BufferedImage source, int width) {
        int height = Math.round(source.getHeight() * (width / (float) source.getWidth()));
        return resize(source, width, height);
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    private static BufferedImage transparentBlackCorners(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int width = source.getWidth();
        int height = source.getHeight();
        boolean[][] outside = new boolean[height][width];
        int[] queueX = new int[width * height];
        int[] queueY = new int[width * height];
        int head = 0;
        int tail = 0;
        int[][] seeds = {{0, 0}, {width - 1, 0}, {0, height - 1}, {width - 1, height - 1}};
        for (int[] seed : seeds) {
            outside[seed[1]][seed[0]] = true;
            queueX[tail] = seed[0];
            queueY[tail] = seed[1];
            tail++;
        }
        while (head < tail) {
            int x = queueX[head];
            int y = queueY[head];
            head++;
            int[][] neighbors = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};
            for (int[] next : neighbors) {
                int nx = next[0];
                int ny = next[1];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height || outside[ny][nx]) continue;
                if (isNearBlack(source.getRGB(nx, ny))) {
                    outside[ny][nx] = true;
                    queueX[tail] = nx;
                    queueY[tail] = ny;
                    tail++;
                }
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out.setRGB(x, y, outside[y][x] ? 0x00000000 : source.getRGB(x, y));
            }
        }
        return out;
    }

    private static boolean isNearBlack(int argb) {
        int r = (argb >> 16) & 0xff;
        int g = (argb >> 8) & 0xff;
        int b = argb & 0xff;
        return Math.max(r, Math.max(g, b)) < 18;
    }

    private static int blend(int first, int second, double t) {
        int r = (int) Math.round(((first >> 16) & 0xff) * (1.0 - t) + ((second >> 16) & 0xff) * t);
        int g = (int) Math.round(((first >> 8) & 0xff) * (1.0 - t) + ((second >> 8) & 0xff) * t);
        int b = (int) Math.round((first & 0xff) * (1.0 - t) + (second & 0xff) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
