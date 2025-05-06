package burpeditor.export;


import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

public class PNGExporter {
    public static boolean exportToPNG(BufferedImage image, File file) {
        try {
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getParentFile(), file.getName() + ".png");
            }
            ImageIO.write(image, "PNG", file);
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "PNG Export Error: " + e.getMessage(),
                "Export Failed",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}