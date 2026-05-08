import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public final class PrepareAssets {
    private static final int[] DENSITY_ICON_SIZES = {48, 72, 96, 144, 192};
    private static final int[] DENSITY_FOREGROUND_SIZES = {108, 162, 216, 324, 432};
    private static final double ADAPTIVE_SAFE_ZONE_SCALE = 72.0 / 108.0;
    private static final double LEGACY_ICON_SCALE = 0.82;
    private static final String[] DENSITY_DIRS = {
        "mipmap-mdpi",
        "mipmap-hdpi",
        "mipmap-xhdpi",
        "mipmap-xxhdpi",
        "mipmap-xxxhdpi",
    };

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : ".");
        Path input = root.resolve("build/input-assets");
        Path res = root.resolve("app/src/main/res");

        BufferedImage light = readRgb(input.resolve("background-light.png"));
        BufferedImage dark = readRgb(input.resolve("background-dark.png"));
        writeJpeg(light, res.resolve("drawable-nodpi/drawer_background_light.jpg"), 0.90f);
        writeJpeg(dark, res.resolve("drawable-nodpi/drawer_background_dark.jpg"), 0.90f);

        BufferedImage icon = transparentBlackCorners(readArgb(input.resolve("app-icon.png")));
        for (int i = 0; i < DENSITY_DIRS.length; i++) {
            Path dir = res.resolve(DENSITY_DIRS[i]);
            Files.createDirectories(dir);
            BufferedImage launcher = centeredResize(icon, DENSITY_ICON_SIZES[i], LEGACY_ICON_SCALE);
            ImageIO.write(launcher, "png", dir.resolve("ic_launcher.png").toFile());
            ImageIO.write(circleMask(launcher), "png", dir.resolve("ic_launcher_round.png").toFile());

            BufferedImage foreground = centeredResize(icon, DENSITY_FOREGROUND_SIZES[i], ADAPTIVE_SAFE_ZONE_SCALE);
            ImageIO.write(foreground, "png", dir.resolve("ic_launcher_foreground.png").toFile());
        }
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

    private static void writeJpeg(BufferedImage image, Path out, float quality) throws Exception {
        Files.createDirectories(out.getParent());
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out.toFile())) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
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
        int[][] seeds = {
            {0, 0},
            {width - 1, 0},
            {0, height - 1},
            {width - 1, height - 1},
        };
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
                int argb = source.getRGB(x, y);
                out.setRGB(x, y, outside[y][x] ? 0x00000000 : argb);
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

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    private static BufferedImage centeredResize(BufferedImage source, int canvasSize, double scale) {
        BufferedImage out = new BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_ARGB);
        int subjectSize = (int) Math.round(canvasSize * scale);
        int offset = (canvasSize - subjectSize) / 2;
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, offset, offset, subjectSize, subjectSize, null);
        g.dispose();
        return out;
    }

    private static BufferedImage circleMask(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setClip(new Ellipse2D.Float(0, 0, source.getWidth(), source.getHeight()));
        g.drawImage(source, 0, 0, null);
        g.setComposite(AlphaComposite.SrcOver);
        g.dispose();
        return out;
    }
}
