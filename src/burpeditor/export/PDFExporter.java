package burpeditor.export;

import com.itextpdf.text.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.*;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class PDFExporter {
    private static final float MARGIN = 36;
    private static final Font FONT_FALLBACK = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
    private static final Font FONT_HEADING = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);

    public static boolean exportToPDF(BufferedImage diagramImage, JTextPane textPane, File file) {
        Document document = new Document();
        FileOutputStream fos = null;
        
        try {
            file = ensurePdfExtension(file);
            fos = new FileOutputStream(file);
            PdfWriter writer = PdfWriter.getInstance(document, fos);
            
            document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);
            document.open();

            addTitle(document, "Network Diagram Export");
            
            if (diagramImage != null) {
                addDiagramImage(document, diagramImage);
                document.add(Chunk.NEWLINE);
            }

            // Try HTML export first
            if (!tryHtmlExport(document, writer, textPane)) {
                // Fallback to simple text export if HTML fails
                addSimpleText(document, textPane);
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeResources(document, fos);
        }
    }

    private static void addTitle(Document document, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, FONT_HEADING);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(20);
        document.add(p);
    }

    private static boolean tryHtmlExport(Document document, PdfWriter writer, JTextPane textPane) {
        try {
            String htmlContent = extractHtmlContent(textPane);
            htmlContent = cleanHtmlContent(htmlContent);
            
            XMLWorkerHelper.getInstance().parseXHtml(
                writer, 
                document, 
                new StringReader("<html><body>" + htmlContent + "</body></html>")
            );
            return true;
        } catch (Exception e) {
            System.err.println("HTML export failed: " + e.getMessage());
            return false;
        }
    }

    private static void addSimpleText(Document document, JTextPane textPane) throws DocumentException {
        try {
            String text = textPane.getDocument().getText(0, textPane.getDocument().getLength());
            document.add(new Paragraph(text, FONT_FALLBACK));
        } catch (BadLocationException e) {
            document.add(new Paragraph("Could not extract text content", FONT_FALLBACK));
        }
    }

    private static String extractHtmlContent(JTextPane textPane) throws IOException, BadLocationException {
        StringWriter writer = new StringWriter();
        HTMLEditorKit kit = (HTMLEditorKit) textPane.getEditorKit();
        kit.write(writer, textPane.getDocument(), 0, textPane.getDocument().getLength());
        return writer.toString();
    }

    private static String cleanHtmlContent(String html) {
        return html.replaceAll("<o:p>", "")
                  .replaceAll("</o:p>", "")
                  .replaceAll("<!--.*?-->", "")
                  .replaceAll("<head>.*?</head>", "")
                  .replaceAll("<style.*?>.*?</style>", "")
                  .replaceAll("<html.*?>", "<html>")
                  .replaceAll("<body.*?>", "<body>");
    }

    private static File ensurePdfExtension(File file) {
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            return new File(file.getParentFile(), file.getName() + ".pdf");
        }
        return file;
    }

    private static void addDiagramImage(Document document, BufferedImage diagramImage) throws Exception {
        Image pdfImage = Image.getInstance(ExportUtils.convertToByteArray(diagramImage, "PNG"));
        float maxWidth = document.getPageSize().getWidth() - (2 * MARGIN);
        float maxHeight = document.getPageSize().getHeight() / 2;
        
        if (pdfImage.getWidth() > maxWidth || pdfImage.getHeight() > maxHeight) {
            pdfImage.scaleToFit(maxWidth, maxHeight);
        }
        
        pdfImage.setAlignment(Image.ALIGN_CENTER);
        document.add(pdfImage);
    }

    private static void closeResources(Document document, FileOutputStream fos) {
        try {
            if (document != null && document.isOpen()) {
                document.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing document: " + e.getMessage());
        }

        try {
            if (fos != null) {
                fos.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing stream: " + e.getMessage());
        }
    }
}