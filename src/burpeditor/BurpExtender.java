package burpeditor;

import burp.IBurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.ITab;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JColorChooser;
import javax.swing.text.html.HTML;

import burpeditor.export.PDFExporter;
import burpeditor.export.PNGExporter;

public class BurpExtender implements IBurpExtender, ITab {
    private JPanel mainPanel;
    private JTextPane textPane;
    private IBurpExtenderCallbacks callbacks;
    private Map<String, BufferedImage> iconCache = new HashMap<>();
    private JToggleButton toggleButton;
    private JPanel iconPanel;
    private JComboBox<String> fontSizeCombo;
    private JComboBox<String> fontFamilyCombo;
    private JSpinner rowsSpinner;
    private JSpinner colsSpinner;
    private JButton drawLineButton;
    private DrawingPanel drawingPanel;
    private Point lineStartPoint;
    private boolean isDrawingLine = false;
    private DraggableIcon draggedIcon = null;
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 36;
    private static final int FONT_ZOOM_INCREMENT = 1;
    

    private final String[] iconNames = { "Building.png", "cloud.png", "Cloud2.png", "CloudError.png", "cluster.png",
            "database.png", "firewall.png", "Folder.png", "hacker.png", "IPv4Firewall_Workstation.png", "laptop.png",
            "Laptop2.png", "Layer3Switch.png", "Meraki.png", "Router.png", "Server.png", "Server2.png", "Share.png",
            "User.png", "Wire.png", "Workstation.png", "Zoomin.png", "Zoomout.png","Zoomreset.png","Server3.png","email.png","browser.png","Home_Wireless_Router.png","cloud-server_icons.png","windows_cmd.png","Save.png","Alert.png","Copy.png" };

    private final String[] fontSizes = { "8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "26", "28",
            "36", "48" };
    private final String[] popularFonts = { "Segoe UI", "Calibri", "Arial", "Times New Roman", "Courier New", "Georgia",
            "Verdana", "Tahoma", "Trebuchet MS", "Consolas", "Cambria", "Palatino Linotype" };

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        callbacks.setExtensionName("Network Schematics Editor");
        SwingUtilities.invokeLater(() -> {
            initializeUI();
            callbacks.addSuiteTab(BurpExtender.this);
        });
    }

    class DrawingPanel extends JPanel {
    	
    	
    	private double scale = 1.0;
        private List<Line> lines = new ArrayList<>();
        private Line currentLine;
        private List<DraggableIcon> icons = new ArrayList<>();
        
        public void setScale(double scale) {
            this.scale = scale;
            for (Component c : getComponents()) {
                if (c instanceof DraggableIcon) {
                    // Scale icon positions
                    Point pos = c.getLocation();
                    c.setLocation(
                        (int)(pos.x * scale),
                        (int)(pos.y * scale)
                    );
                    c.setSize(
                        (int)(48 * scale),
                        (int)(48 * scale)
                    );
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D)g.create();
            
            // Apply scaling transform
            g2d.scale(scale, scale);
            
            // Draw all lines with scaling
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke((float)(2/scale))); // Adjust stroke width for zoom
            
            for (Line line : lines) {
                g2d.drawLine(line.x1, line.y1, line.x2, line.y2);
            }

            if (currentLine != null) {
                g2d.drawLine(currentLine.x1, currentLine.y1, currentLine.x2, currentLine.y2);
            }
            
            g2d.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            // Calculate size needed for all components at current zoom
            int width = 800; // Base width
            int height = 600; // Base height
            
            // Account for icon positions
            for (Component c : getComponents()) {
                Rectangle bounds = c.getBounds();
                width = Math.max(width, bounds.x + bounds.width);
                height = Math.max(height, bounds.y + bounds.height);
            }
            
            return new Dimension(
                (int)(width * scale),
                (int)(height * scale)
            );
        }

    	
     	public DrawingPanel() {
            setOpaque(false);
            setLayout(null);
        }
        
        public void removeAllIcons() {
            for (Component c : getComponents()) {
                if (c instanceof DraggableIcon) {
                    remove(c);
                }
            }
            icons.clear();
        }
        public void removeIcon(DraggableIcon icon) {
            icons.remove(icon);
            remove(icon);
            revalidate();
            repaint();
        }

        
        
        

        public void addLine(Line line) {
            lines.add(line);
            repaint();
        }

        public void setCurrentLine(Line line) {
            this.currentLine = line;
            repaint();
        }

        public void clearLines() {
            lines.clear();
            repaint();
        }

        public void addIcon(DraggableIcon icon) {
            icons.add(icon);
            add(icon);
            revalidate();
            repaint();
        }
    }

    static class Line {
        int x1, y1, x2, y2;
        public Line(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    class DraggableIcon extends JButton {
        private Point anchorPoint;
        private String iconName;

        public DraggableIcon(ImageIcon icon, String iconName) {
            super(icon);
            this.iconName = iconName;
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Create right-click context menu
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem deleteItem = new JMenuItem("Delete");
            deleteItem.addActionListener(e -> {
                Container parent = getParent();
                if (parent instanceof DrawingPanel) {
                    ((DrawingPanel)parent).removeIcon(this);
                }
            });
            popupMenu.add(deleteItem);

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    } else {
                        anchorPoint = e.getPoint();
                        draggedIcon = DraggableIcon.this;
                    }
                }

                public void mouseReleased(MouseEvent e) {
                    if (!SwingUtilities.isRightMouseButton(e)) {
                        draggedIcon = null;
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (!SwingUtilities.isRightMouseButton(e)) {
                        int anchorX = anchorPoint.x;
                        int anchorY = anchorPoint.y;

                        Point parentOnScreen = getParent().getLocationOnScreen();
                        Point mouseOnScreen = e.getLocationOnScreen();
                        Point position = new Point(mouseOnScreen.x - parentOnScreen.x - anchorX,
                                mouseOnScreen.y - parentOnScreen.y - anchorY);
                        setLocation(position);
                        getParent().repaint();
                    }
                }
            });
        }

        public String getIconName() {
            return iconName;
        }
    }
    private void initializeUI() {
        mainPanel = new JPanel(new BorderLayout());

        // Create top toolbar
        JToolBar topToolbar = new JToolBar();
        topToolbar.setFloatable(false);
        topToolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xcccccc)));
        topToolbar.setBackground(new Color(0xf3f3f3));
        topToolbar.setPreferredSize(new Dimension(800, 42));

        // File/Export section
        JPanel filePanel = createToolbarSection("Export");
        JButton exportPngButton = createToolbarButton("PNG", new Color(76, 175, 80), "image.png");
        exportPngButton.addActionListener(e -> exportToPNG());
        JButton exportPdfButton = createToolbarButton("PDF", new Color(244, 67, 54), "pdf.png");
        exportPdfButton.addActionListener(e -> exportToPDF());
        filePanel.add(exportPngButton);
        filePanel.add(Box.createHorizontalStrut(5));
        filePanel.add(exportPdfButton);
        topToolbar.add(filePanel);

        // Insert section
        JPanel insertPanel = createToolbarSection("Insert");
        JButton tableButton = createToolbarButton("Table", new Color(66, 133, 244), "table.png");
        tableButton.addActionListener(e -> showTableInsertDialog());
        insertPanel.add(tableButton);
        topToolbar.add(insertPanel);

        // Font section
        JPanel fontPanel = createToolbarSection("Font");
        fontFamilyCombo = new JComboBox<>(popularFonts);
        fontFamilyCombo.setSelectedItem("Segoe UI");
        fontFamilyCombo.setMaximumRowCount(15);
        fontFamilyCombo.setPreferredSize(new Dimension(140, 26));
        fontFamilyCombo.addActionListener(e -> updateFont());

        fontSizeCombo = new JComboBox<>(fontSizes);
        fontSizeCombo.setSelectedItem("12");
        fontSizeCombo.setPreferredSize(new Dimension(50, 26));
        fontSizeCombo.setEditable(true);
        fontSizeCombo.addActionListener(e -> updateFont());

     // Zoom controls
        JButton zoomInButton = createToolbarButton("", new Color(100, 150, 255), "Zoomin.png");
        zoomInButton.setToolTipText("Zoom In");
        zoomInButton.addActionListener(e -> zoomIn());

        JButton zoomOutButton = createToolbarButton("", new Color(100, 150, 255), "Zoomout.png");
        zoomOutButton.setToolTipText("Zoom Out");
        zoomOutButton.addActionListener(e -> zoomOut());

        JButton zoomResetButton = createToolbarButton("", new Color(100, 150, 255), "Zoomreset.png");
        zoomResetButton.setToolTipText("Reset Zoom");
        zoomResetButton.addActionListener(e -> zoomReset());
        JButton clearButton = createToolbarButton("", new Color(239, 83, 80), "trash.png");
        clearButton.addActionListener(e -> clearCanvas());

        JButton colorButton = createToolbarButton("", Color.WHITE, "color.png");
        colorButton.setToolTipText("Text Color");
        colorButton.addActionListener(e -> changeTextColor());

        drawLineButton = createToolbarButton("Draw Line", new Color(150, 100, 255), "line.png");
        drawLineButton.addActionListener(e -> {
            isDrawingLine = !isDrawingLine;
            if (isDrawingLine) {
                drawingPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                textPane.setCursor(Cursor.getDefaultCursor());
            } else {
                drawingPanel.setCursor(Cursor.getDefaultCursor());
            }
        });

        fontPanel.add(new JLabel("Font:"));
        fontPanel.add(fontFamilyCombo);
        fontPanel.add(Box.createHorizontalStrut(5));
        fontPanel.add(new JLabel("Size:"));
        fontPanel.add(fontSizeCombo);
        fontPanel.add(Box.createHorizontalStrut(10));
        fontPanel.add(zoomInButton);
        fontPanel.add(zoomOutButton);
        fontPanel.add(zoomResetButton);
        fontPanel.add(colorButton);
        fontPanel.add(clearButton);
        fontPanel.add(drawLineButton);
        topToolbar.add(fontPanel);

        // Main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.7);
        splitPane.setDividerSize(5);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        // Drawing panel setup
        drawingPanel = new DrawingPanel();
        drawingPanel.setLayout(null);
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isDrawingLine) {
                    lineStartPoint = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDrawingLine && lineStartPoint != null) {
                    drawingPanel.addLine(new Line(
                        lineStartPoint.x, lineStartPoint.y,
                        e.getX(), e.getY()
                    ));
                    lineStartPoint = null;
                    drawingPanel.setCurrentLine(null);
                }
            }
        });

        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDrawingLine && lineStartPoint != null) {
                    drawingPanel.setCurrentLine(new Line(
                        lineStartPoint.x, lineStartPoint.y,
                        e.getX(), e.getY()
                    ));
                }
            }
        });

        JScrollPane drawingScroll = new JScrollPane(drawingPanel);
        drawingScroll.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setTopComponent(drawingScroll);

        // Text editor setup
        textPane = new JTextPane();
        textPane.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        textPane.setContentType("text/html");
        JScrollPane editorScroll = new JScrollPane(textPane);
        editorScroll.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setBottomComponent(editorScroll);

        // Style sheet setup
        StyleSheet styleSheet = ((HTMLDocument) textPane.getDocument()).getStyleSheet();
        styleSheet.addRule("table { border-collapse: collapse; }");
        styleSheet.addRule("td { border: 1px solid #ddd; padding: 5px; min-width: 20px; min-height: 20px; }");
        styleSheet.addRule("table[contenteditable='true'] { resize: both; overflow: auto; display: inline-block; }");

        // Icon panel setup
        iconPanel = createIconPanel();
        iconPanel.setPreferredSize(new Dimension(260, -1));

        // Toggle button for icon panel
        toggleButton = new JToggleButton(" Icons ");
        toggleButton.setSelected(true);
        toggleButton.setPreferredSize(new Dimension(80, 30));
        toggleButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        toggleButton.setBorder(
                BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(0xdddddd)),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        toggleButton.setBackground(new Color(0xf8f8f8));
        toggleButton.setFocusPainted(false);
        toggleButton.setHorizontalAlignment(SwingConstants.CENTER);
        toggleButton.setIcon(new ArrowIcon(false));
        toggleButton.setSelectedIcon(new ArrowIcon(true));
        toggleButton.addActionListener(e -> {
            iconPanel.setVisible(toggleButton.isSelected());
            mainPanel.revalidate();
        });

        // Left panel with icons
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(toggleButton, BorderLayout.NORTH);
        leftPanel.add(iconPanel, BorderLayout.CENTER);

        // Assemble main panel
        mainPanel.add(topToolbar, BorderLayout.NORTH);
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Add table context menu
        addTableContextMenu();
    }
    
    
    
 

    // Add these methods
    private BufferedImage captureDiagramImage() {
        BufferedImage image = new BufferedImage(
            drawingPanel.getWidth(), 
            drawingPanel.getHeight(),
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        drawingPanel.paint(g);
        g.dispose();
        return image;
    }

    private BufferedImage captureCombinedImage() {
        BufferedImage image = new BufferedImage(
            Math.max(drawingPanel.getWidth(), textPane.getWidth()),
            drawingPanel.getHeight() + textPane.getHeight(),
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        drawingPanel.paint(g);
        g.translate(0, drawingPanel.getHeight());
        textPane.paint(g);
        g.dispose();
        return image;
    }

    private void exportToPNG() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save as PNG");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Files", "png"));
        
        if (chooser.showSaveDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            boolean success = PNGExporter.exportToPNG(
                captureCombinedImage(),
                chooser.getSelectedFile()
            );
            
            if (success) {
                JOptionPane.showMessageDialog(mainPanel,
                    "PNG exported successfully!",
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void exportToPDF() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save as PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        
        if (chooser.showSaveDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            boolean success = PDFExporter.exportToPDF(
                captureDiagramImage(),
                textPane,
                chooser.getSelectedFile()
            );
            
            if (success) {
                JOptionPane.showMessageDialog(mainPanel,
                    "PDF exported successfully!",
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    private JDialog createProgressDialog() {
        JDialog dialog = new JDialog((Frame)null, "Exporting PDF", true);
        dialog.setSize(300, 100);
        dialog.setLocationRelativeTo(mainPanel);
        dialog.setLayout(new BorderLayout());
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        dialog.add(progressBar, BorderLayout.CENTER);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            dialog.dispose();
        });
        
        JPanel panel = new JPanel();
        panel.add(cancelButton);
        dialog.add(panel, BorderLayout.SOUTH);
        
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        return dialog;
    }

    private void showSuccess(File file) {
        JOptionPane.showMessageDialog(mainPanel,
            "PDF successfully saved to:\n" + file.getAbsolutePath(),
            "Export Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(mainPanel,
            "Failed to export PDF:\n" + e.getMessage(),
            "Export Failed",
            JOptionPane.ERROR_MESSAGE);
        callbacks.printError("PDF Export Error: " + e.getMessage());
    }
    
    
 // Replace your current zoom methods with these:
    private void zoomIn() {
        Font currentFont = textPane.getFont();
        int newSize = Math.min(currentFont.getSize() + FONT_ZOOM_INCREMENT, MAX_FONT_SIZE);
        textPane.setFont(new Font(
            currentFont.getFamily(),
            currentFont.getStyle(),
            newSize
        ));
        drawingPanel.setFont(new Font(
            currentFont.getFamily(),
            currentFont.getStyle(),
            newSize
        ));
    }

    private void zoomOut() {
        Font currentFont = textPane.getFont();
        int newSize = Math.max(currentFont.getSize() - FONT_ZOOM_INCREMENT, MIN_FONT_SIZE);
        textPane.setFont(new Font(
            currentFont.getFamily(),
            currentFont.getStyle(),
            newSize
        ));
        drawingPanel.setFont(new Font(
            currentFont.getFamily(),
            currentFont.getStyle(),
            newSize
        ));
    }

    private void zoomReset() {
        Font currentFont = textPane.getFont();
        textPane.setFont(new Font(
            currentFont.getFamily(),
            currentFont.getStyle(),
            DEFAULT_FONT_SIZE
        ));
        drawingPanel.setFont(new Font(
            currentFont.getFamily(),
            currentFont.getStyle(),
            DEFAULT_FONT_SIZE
        ));
    }


    
    
    private void showTableInsertDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Insert Table");
        dialog.setModal(true);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(350, 220);  // Increased height for width field

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Rows
        gbc.gridx = 0;
        gbc.gridy = 0;
        dialog.add(new JLabel("Rows:"), gbc);

        gbc.gridx = 1;
        rowsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 50, 1));
        dialog.add(rowsSpinner, gbc);

        // Columns
        gbc.gridx = 0;
        gbc.gridy = 1;
        dialog.add(new JLabel("Columns:"), gbc);

        gbc.gridx = 1;
        colsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 50, 1));
        dialog.add(colsSpinner, gbc);

        // Border Width
        gbc.gridx = 0;
        gbc.gridy = 2;
        dialog.add(new JLabel("Border Width:"), gbc);

        gbc.gridx = 1;
        JSpinner borderSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
        dialog.add(borderSpinner, gbc);

        // Table Width
        gbc.gridx = 0;
        gbc.gridy = 3;
        dialog.add(new JLabel("Width (px or %):"), gbc);

        gbc.gridx = 1;
        JTextField widthField = new JTextField("300px");
        dialog.add(widthField, gbc);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton insertButton = new JButton("Insert");
        insertButton.addActionListener(e -> {
            insertTable((int)rowsSpinner.getValue(), 
                      (int)colsSpinner.getValue(), 
                      (int)borderSpinner.getValue(),
                      widthField.getText().trim());
            dialog.dispose();
        });
        buttonPanel.add(insertButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);

        dialog.add(buttonPanel, gbc);
        dialog.setLocationRelativeTo(mainPanel);
        dialog.setVisible(true);
    }

    private void insertTable(int rows, int cols, int border, String width) {
        // Validate width input
        if (!width.matches("^\\d+(px|%)$")) {
            width = "300px";
            JOptionPane.showMessageDialog(mainPanel, 
                "Invalid width format. Using default 300px.\nUse format like '500px' or '50%'",
                "Invalid Width",
                JOptionPane.WARNING_MESSAGE);
        }

        StringBuilder html = new StringBuilder(
            "<table contenteditable='true' style='border-collapse: collapse; width: ")
            .append(width).append("; height: 200px; resize: both; overflow: auto; display: inline-block;' border='")
            .append(border).append("'>");
        
        for (int i = 0; i < rows; i++) {
            html.append("<tr>");
            for (int j = 0; j < cols; j++) {
                html.append("<td style='padding: 5px; border: 1px solid #ddd;'>&nbsp;</td>");
            }
            html.append("</tr>");
        }
        html.append("</table><br>");

        try {
            int pos = textPane.getCaretPosition();
            textPane.getDocument().insertString(pos, "\n", null);
            HTMLEditorKit kit = (HTMLEditorKit)textPane.getEditorKit();
            kit.insertHTML((HTMLDocument)textPane.getDocument(), pos, html.toString(), 0, 0, null);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainPanel, "Error inserting table: " + ex.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addTableContextMenu() {
        JPopupMenu tableMenu = new JPopupMenu();
        
        JMenuItem resizeItem = new JMenuItem("Resize Table");
        resizeItem.addActionListener(e -> resizeSelectedTable());
        tableMenu.add(resizeItem);
        
        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    Element tableElement = findTableElementAtPosition(e.getPoint());
                    if (tableElement != null) {
                        tableMenu.show(textPane, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void resizeSelectedTable() {
        Element tableElement = findTableElementAtPosition(textPane.getCaret().getMagicCaretPosition());
        if (tableElement != null) {
            AttributeSet attrs = tableElement.getAttributes();
            String currentWidth = (String)attrs.getAttribute(HTML.Attribute.STYLE);
            currentWidth = currentWidth.replaceAll(".*width:\\s*([^;]+);.*", "$1");
            
            String newWidth = JOptionPane.showInputDialog(mainPanel, 
                "Enter new width (e.g., 500px or 50%):", currentWidth);
            
            if (newWidth != null && newWidth.matches("^\\d+(px|%)$")) {
                try {
                    HTMLDocument doc = (HTMLDocument)textPane.getDocument();
                    SimpleAttributeSet newAttrs = new SimpleAttributeSet();
                    String newStyle = attrs.getAttribute(HTML.Attribute.STYLE).toString()
                        .replaceAll("width:\\s*[^;]+;", "width: " + newWidth + ";");
                    newAttrs.addAttribute(HTML.Attribute.STYLE, newStyle);
                    doc.setCharacterAttributes(tableElement.getStartOffset(), 
                        tableElement.getEndOffset() - tableElement.getStartOffset(), 
                        newAttrs, false);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mainPanel, "Error resizing table: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private Element findTableElementAtPosition(Point p) {
        if (p == null) return null;
        
        int pos = textPane.viewToModel(p);
        if (pos < 0) return null;
        
        HTMLDocument doc = (HTMLDocument)textPane.getDocument();
        Element elem = doc.getCharacterElement(pos);
        
        while (elem != null) {
            if (elem.getName().equals("table")) {
                return elem;
            }
            elem = elem.getParentElement();
        }
        return null;
    }
    
    private void setFontStyle(int style, boolean add) {
        StyledDocument doc = textPane.getStyledDocument();
        int start = textPane.getSelectionStart();
        int end = textPane.getSelectionEnd();

        if (start == end) {
            Font currentFont = textPane.getFont();
            int newStyle = currentFont.getStyle();

            if (style == Font.BOLD) {
                newStyle = add ? (newStyle | Font.BOLD) : (newStyle & ~Font.BOLD);
            } else if (style == Font.ITALIC) {
                newStyle = add ? (newStyle | Font.ITALIC) : (newStyle & ~Font.ITALIC);
            }

            textPane.setFont(new Font(currentFont.getFamily(), newStyle, currentFont.getSize()));
        } else {
            Style styleObj = textPane.addStyle("fontStyle", null);

            if (style == Font.BOLD) {
                StyleConstants.setBold(styleObj, add);
            } else if (style == Font.ITALIC) {
                StyleConstants.setItalic(styleObj, add);
            }

            doc.setCharacterAttributes(start, end - start, styleObj, false);
        }
    }

    private void setUnderline(boolean underline) {
        StyledDocument doc = textPane.getStyledDocument();
        int start = textPane.getSelectionStart();
        int end = textPane.getSelectionEnd();

        if (start == end) {
            MutableAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setUnderline(attrs, underline);
            textPane.setCharacterAttributes(attrs, false);
        } else {
            Style style = textPane.addStyle("underline", null);
            StyleConstants.setUnderline(style, underline);
            doc.setCharacterAttributes(start, end - start, style, false);
        }
    }

    private void changeTextColor() {
        Color color = JColorChooser.showDialog(mainPanel, "Choose Text Color", Color.BLACK);
        if (color != null && textPane.getSelectedText() != null) {
            String selectedText = textPane.getSelectedText();
            String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            String styledHtml = "<span style='color:" + hexColor + ";'>" + selectedText + "</span>";

            try {
                int start = textPane.getSelectionStart();
                int end = textPane.getSelectionEnd();

                // Remove selected text
                textPane.getDocument().remove(start, end - start);

                // Insert styled HTML in place
                HTMLEditorKit kit = (HTMLEditorKit) textPane.getEditorKit();
                kit.insertHTML((HTMLDocument) textPane.getDocument(), start, styledHtml, 0, 0, null);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainPanel, "Error applying color: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }



    private void clearCanvas() {
        int response = JOptionPane.showConfirmDialog(mainPanel, "Are you sure you want to clear the entire canvas?",
                "Confirm Clear", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            textPane.setText("");
            drawingPanel.clearLines();
            drawingPanel.removeAll();
            drawingPanel.revalidate();
            drawingPanel.repaint();
        }
    }

    private JPanel createToolbarSection(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(
                BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xdddddd)),
                        BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        panel.setBackground(new Color(0xf3f3f3));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        titleLabel.setForeground(new Color(0x666666));
        panel.add(titleLabel);
        panel.add(Box.createHorizontalStrut(10));
        return panel;
    }

    private JButton createToolbarButton(String text, Color bgColor, String iconName) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(bgColor.getRed() - 20, bgColor.getGreen() - 20, bgColor.getBlue() - 20), 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        ImageIcon icon = getScaledIcon(iconName, 16, 16);
        if (icon != null) {
            button.setIcon(icon);
            button.setText("");
        }

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(Math.max(0, bgColor.getRed() - 10), Math.max(0, bgColor.getGreen() - 10),
                        Math.max(0, bgColor.getBlue() - 10)));
            }

            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        return button;
    }

    private JButton createToolbarToggleButton(String text, String iconName) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xcccccc), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        button.setBackground(Color.WHITE);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        ImageIcon icon = getScaledIcon(iconName, 16, 16);
        if (icon != null) {
            button.setIcon(icon);
            button.setText("");
        }

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(0xe6e6e6));
            }

            public void mouseExited(MouseEvent e) {
                if (!button.isSelected()) {
                    button.setBackground(Color.WHITE);
                }
            }
        });
        return button;
    }

    private void updateFont() {
        try {
            String fontName = (String) fontFamilyCombo.getSelectedItem();
            int fontSize = Integer.parseInt((String) fontSizeCombo.getSelectedItem());
            int start = textPane.getSelectionStart();
            int end = textPane.getSelectionEnd();

            if (start == end) {
                // Apply to the whole pane only if no text is selected (optional)
                textPane.setFont(new Font(fontName, Font.PLAIN, fontSize));
            } else {
                String selectedText = textPane.getSelectedText();
                if (selectedText == null) return;

                String styledHtml = String.format(
                    "<span style='font-family:%s; font-size:%dpx;'>%s</span>",
                    fontName, fontSize, selectedText
                );

                // Replace selected text with styled span
                textPane.getDocument().remove(start, end - start);
                HTMLEditorKit kit = (HTMLEditorKit) textPane.getEditorKit();
                kit.insertHTML((HTMLDocument) textPane.getDocument(), start, styledHtml, 0, 0, null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainPanel, "Error updating font: " + ex.getMessage(), 
                "Font Error", JOptionPane.ERROR_MESSAGE);
        }
    }



    private JPanel createIconPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(
                BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xdddddd)),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JPanel iconGrid = new JPanel(new GridLayout(0, 3, 4, 4));
        iconGrid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        BufferedImage placeholder = createPlaceholderIcon();

        for (String iconName : iconNames) {
            try {
                String baseName = iconName.substring(0, iconName.lastIndexOf('.'));
                BufferedImage icon = ImageIO.read(getClass().getResourceAsStream("/icons/" + iconName));
                if (icon == null)
                    icon = placeholder;

                BufferedImage scaledIcon = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = scaledIcon.createGraphics();
                g.drawImage(icon, 0, 0, 48, 48, null);
                g.dispose();
                iconCache.put(baseName, scaledIcon);

                JButton button = createIconButton(iconName, baseName);
                iconGrid.add(button);
            } catch (Exception ex) {
                String baseName = iconName.substring(0, iconName.lastIndexOf('.'));
                JButton button = new JButton(baseName);
                iconGrid.add(button);
            }
        }

        panel.add(new JScrollPane(iconGrid), BorderLayout.CENTER);
        return panel;
    }

    private JButton createIconButton(String iconName, String baseName) {
        JButton button = new JButton(new ImageIcon(iconCache.get(baseName)));
        button.setToolTipText(baseName);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                DraggableIcon draggableIcon = new DraggableIcon(new ImageIcon(iconCache.get(baseName)), baseName);
                draggableIcon.setSize(48, 48);
                draggableIcon.setLocation(10, 10);
                drawingPanel.addIcon(draggableIcon);
            }
        });

        return button;
    }

    
    private BufferedImage createPlaceholderIcon() {
        BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, 24, 24);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, 23, 23);
        g.drawString("?", 8, 16);
        g.dispose();
        return img;
    }

    @Override
    public String getTabCaption() {
        return "Schematics";
    }

    @Override
    public Component getUiComponent() {
        return mainPanel;
    }

    private ImageIcon getScaledIcon(String iconName, int width, int height) {
        try {
            BufferedImage originalImage = ImageIO.read(getClass().getResource("/icons/" + iconName));
            if (originalImage != null) {
                Image scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static class ArrowIcon implements Icon {
        private final boolean expanded;

        public ArrowIcon(boolean expanded) {
            this.expanded = expanded;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.GRAY);
            if (expanded) {
                g2.drawLine(x, y + 5, x + 5, y + 10);
                g2.drawLine(x, y + 5, x + 5, y);
            } else {
                g2.drawLine(x + 5, y, x, y + 5);
                g2.drawLine(x + 5, y + 10, x, y + 5);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 10;
        }

        @Override
        public int getIconHeight() {
            return 10;
        }
    }
}