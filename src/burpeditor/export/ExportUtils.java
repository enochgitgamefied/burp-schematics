package burpeditor.export;


import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

public class ExportUtils {
    public static byte[] convertToByteArray(BufferedImage image, String format) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Image conversion failed", e);
        }
    }
}