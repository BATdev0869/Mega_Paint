import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.Stack;

public class MegaPaintApp extends JFrame {

    private final Settings settings = new Settings();
    private final File settingsFile = new File("paint_settings.txt");
    // Settings class at the top
    static class Settings {
        boolean showWelcome = true;
        public void save(File file) {
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                writer.write("showWelcome=" + showWelcome + "\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        public void load(File file) {
            try (java.util.Scanner scanner = new java.util.Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String[] parts = scanner.nextLine().split("=");
                    if (parts[0].equals("showWelcome")) {
                        showWelcome = Boolean.parseBoolean(parts[1]);
                    }
                }
            } catch (Exception e) {
                // ignore missing file errors
            }
        }
    }

    enum Tool { BRUSH, ERASER, LINE, RECT, CIRCLE, FILL }

    private BufferedImage canvas;
    private Graphics2D g2d;
    private Point lastPoint = null;
    private Point startPoint = null;
    private Color currentColor = Color.BLACK;
    private int brushSize = 4;
    private int eraserSize = 20; // separate variable for eraser
    private Tool currentTool = Tool.BRUSH;
    private final Stack<BufferedImage> undoStack = new Stack<>();

    private final Color[] colorOptions = {
        Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE,
        Color.MAGENTA, Color.CYAN, Color.GRAY, Color.PINK, Color.YELLOW
    };
    private final Rectangle[] colorBoxes = new Rectangle[colorOptions.length];
    private final Rectangle clearBtn      = new Rectangle(10, 60, 80, 30);
    private final Rectangle saveBtn       = new Rectangle(100, 60, 80, 30);
    private final Rectangle openBtn       = new Rectangle(190, 60, 80, 30);
    private final Rectangle brushBtn      = new Rectangle(280, 60, 80, 30);
    private final Rectangle eraserBtn     = new Rectangle(370, 60, 80, 30);
    private final Rectangle rectBtn       = new Rectangle(460, 60, 80, 30);
    private final Rectangle circleBtn     = new Rectangle(550, 60, 80, 30);
    private final Rectangle lineBtn       = new Rectangle(640, 60, 80, 30);
    private final Rectangle undoBtn       = new Rectangle(730, 60, 80, 30);
    private final Rectangle brushSizeBtn  = new Rectangle(820, 60, 100, 30);
    private final Rectangle eraserSizeBtn = new Rectangle(930, 60, 100, 30);

    private JPanel canvasPanel;

    public MegaPaintApp() {
        setTitle("Mega Paint App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        // Initialize canvasPanel first, so that its size is available.
        canvasPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(canvas, 0, 0, null);
                drawUI(g);
            }
        };
        // Set the preferred size of the canvas before using it
        canvasPanel.setPreferredSize(new Dimension(1000, 600));

        // Create the canvas based on the panel's preferred size.
        Dimension panelSize = canvasPanel.getPreferredSize();
        canvas = new BufferedImage(panelSize.width, panelSize.height, BufferedImage.TYPE_INT_ARGB);
        g2d = canvas.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Setup the color boxes.
        for (int i = 0; i < colorBoxes.length; i++) {
            colorBoxes[i] = new Rectangle(10 + i * 40, 10, 30, 30);
        }

        // Add a component listener for resizing
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension newSize = canvasPanel.getSize();
                if (newSize.width > 0 && newSize.height > 0) {
                    resizeCanvas(newSize.width, newSize.height);
                    canvasPanel.repaint();
                }
            }
        });

        // Mouse listeners to handle drawing and clicks.
        canvasPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleClick(e);
                if (e.getY() > 100) {
                    pushUndo();
                    if (currentTool == Tool.FILL) {
                        floodFill(e.getX(), e.getY(), new Color(canvas.getRGB(e.getX(), e.getY())), currentColor);
                        canvasPanel.repaint();
                    } else {
                        startPoint = e.getPoint();
                        lastPoint = e.getPoint();
                    }
                    if (currentTool == Tool.ERASER) {
                        g2d.setStroke(new BasicStroke(eraserSize));
                        g2d.setColor(Color.RED); // typically eraser uses white—adjust as needed
                    } else {
                        g2d.setStroke(new BasicStroke(brushSize));
                        g2d.setColor(currentColor);
                    }
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (startPoint != null && currentTool != Tool.BRUSH 
                    && currentTool != Tool.ERASER && currentTool != Tool.FILL) {
                    drawShape(startPoint, e.getPoint());
                    canvasPanel.repaint();
                }
                startPoint = null;
                lastPoint = null;
            }
        });

        canvasPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastPoint != null && currentTool != Tool.FILL && currentTool != Tool.LINE &&
                        currentTool != Tool.RECT && currentTool != Tool.CIRCLE) {
                    g2d.setColor(currentTool == Tool.ERASER ? Color.WHITE : currentColor);
                    g2d.setStroke(new BasicStroke(currentTool == Tool.ERASER ? eraserSize : brushSize));
                    g2d.drawLine(lastPoint.x, lastPoint.y, e.getX(), e.getY());
                    lastPoint = e.getPoint();
                    canvasPanel.repaint();
                }
            }
        });

        // Add canvasPanel to the frame, pack, and show.
        add(canvasPanel);
        pack();
        setVisible(true);

        // Load settings and display welcome message if necessary.
        settings.load(settingsFile);
        if (settings.showWelcome) {
            JCheckBox dontShowAgain = new JCheckBox("Don't show this again");
            JTextArea message = new JTextArea("""
                🎨 Welcome to Mega Paint App!

                - Use the toolbar to select tools and colors
                - Click 'Open' to load an image
                - Click 'Save' to store your masterpiece
                - Use 'Undo' to go back one step

                Have fun creating!
                """);
            message.setEditable(false);
            message.setFocusable(false);
            message.setBackground(new JLabel().getBackground());

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.add(message, BorderLayout.CENTER);
            panel.add(dontShowAgain, BorderLayout.SOUTH);

            JOptionPane.showMessageDialog(this, panel, "Welcome", JOptionPane.INFORMATION_MESSAGE);

            if (dontShowAgain.isSelected()) {
                settings.showWelcome = false;
                settings.save(settingsFile);
            }
        }
    }

    private void pushUndo() {
        BufferedImage copy = new BufferedImage(canvas.getWidth(), canvas.getHeight(), canvas.getType());
        Graphics g = copy.getGraphics();
        g.drawImage(canvas, 0, 0, null);
        g.dispose();
        undoStack.push(copy);
    }

    private void drawShape(Point start, Point end) {
        g2d.setColor(currentColor);
        g2d.setStroke(new BasicStroke(brushSize));
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int w = Math.abs(end.x - start.x);
        int h = Math.abs(end.y - start.y);
        switch (currentTool) {
            case LINE -> g2d.drawLine(start.x, start.y, end.x, end.y);
            case RECT -> g2d.drawRect(x, y, w, h);
            case CIRCLE -> g2d.drawOval(x, y, w, h);
        }
    }

    private void handleClick(MouseEvent e) {
        Point p = e.getPoint();
        for (int i = 0; i < colorBoxes.length; i++) {
            if (colorBoxes[i].contains(p)) {
                currentColor = colorOptions[i];
                canvasPanel.repaint();
                return;
            }
        }
        if (clearBtn.contains(p)) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        } else if (saveBtn.contains(p)) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Image");
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                try {
                    ImageIO.write(canvas, "PNG", fileToSave);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else if (brushBtn.contains(p))
            currentTool = Tool.BRUSH;
        else if (eraserBtn.contains(p))
            currentTool = Tool.ERASER;
        else if (rectBtn.contains(p))
            currentTool = Tool.RECT;
        else if (circleBtn.contains(p))
            currentTool = Tool.CIRCLE;
        else if (lineBtn.contains(p))
            currentTool = Tool.LINE;
        else if (undoBtn.contains(p)) {
            if (!undoStack.isEmpty()) {
                canvas = undoStack.pop();
                g2d = canvas.createGraphics();
            }
        } else if (openBtn.contains(p)) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Open Image");
            int userSelection = fileChooser.showOpenDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToOpen = fileChooser.getSelectedFile();
                try {
                    BufferedImage img = ImageIO.read(fileToOpen);
                    g2d.drawImage(img, 0, 0, null);
                    canvasPanel.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else if (brushSizeBtn.contains(p)) {
            String input = JOptionPane.showInputDialog(this, "Enter brush size:", brushSize);
            if (input != null) {
                try {
                    brushSize = Integer.parseInt(input);
                } catch (NumberFormatException ignored) {}
            }
        } else if (eraserSizeBtn.contains(p)) {
            String input = JOptionPane.showInputDialog(this, "Enter eraser size:", eraserSize);
            if (input != null) {
                try {
                    eraserSize = Integer.parseInt(input);
                } catch (NumberFormatException ignored) {}
            }
        }
        canvasPanel.repaint();
    }

    private void floodFill(int x, int y, Color target, Color replacement) {
        if (target.equals(replacement)) return;
        if (x < 0 || y < 0 || x >= canvas.getWidth() || y >= canvas.getHeight()) return;
        if (!new Color(canvas.getRGB(x, y)).equals(target)) return;
        canvas.setRGB(x, y, replacement.getRGB());
        floodFill(x + 1, y, target, replacement);
        floodFill(x - 1, y, target, replacement);
        floodFill(x, y + 1, target, replacement);
        floodFill(x, y - 1, target, replacement);
    }

    private void drawUI(Graphics g) {
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        for (int i = 0; i < colorBoxes.length; i++) {
            g.setColor(colorOptions[i]);
            g.fillRect(colorBoxes[i].x, colorBoxes[i].y, colorBoxes[i].width, colorBoxes[i].height);
            g.setColor(Color.BLACK);
            g.drawRect(colorBoxes[i].x, colorBoxes[i].y, colorBoxes[i].width, colorBoxes[i].height);
        }
        drawButton(g, clearBtn, "Clear");
        drawButton(g, saveBtn, "Save");
        drawButton(g, brushBtn, "Brush");
        drawButton(g, eraserBtn, "Eraser");
        drawButton(g, rectBtn, "Rect");
        drawButton(g, circleBtn, "Circle");
        drawButton(g, lineBtn, "Line");
        drawButton(g, undoBtn, "Undo");
        drawButton(g, openBtn, "Open");
        drawButton(g, brushSizeBtn, "Brush Size");
        drawButton(g, eraserSizeBtn, "Eraser Size");

        g.setColor(Color.BLACK);
        g.drawString("Tool: " + currentTool + " | Color: " + getColorName(currentColor), 10, 580);
    }

    private void drawButton(Graphics g, Rectangle r, String label) {
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(Color.BLACK);
        g.drawRect(r.x, r.y, r.width, r.height);
        g.drawString(label, r.x + 10, r.y + 20);
    }

    private String getColorName(Color color) {
        for (int i = 0; i < colorOptions.length; i++) {
            if (colorOptions[i].equals(color)) {
                return colorOptions[i].toString();
            }
        }
        return "Custom";
    }

    private void resizeCanvas(int width, int height) {
        BufferedImage newCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newCanvas.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.drawImage(canvas, 0, 0, null);
        g.dispose();
        canvas = newCanvas;
        g2d = canvas.createGraphics();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MegaPaintApp::new);
    }
}