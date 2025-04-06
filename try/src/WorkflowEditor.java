import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WorkflowEditor extends JFrame {
    private CanvasPanel canvas;
    private ToolbarPanel toolbar;

    public WorkflowEditor() {
        setTitle("Workflow Design Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // 設定字體
        Font largeFont = new Font("SansSerif", Font.BOLD, 16);
        setUIFont(new javax.swing.plaf.FontUIResource(largeFont));

        // 設定選單
        setJMenuBar(createMenuBar(largeFont));

        // 初始化畫布與工具列
        canvas = new CanvasPanel();
        toolbar = new ToolbarPanel();

        // 設定工具列區域
        JPanel toolbarWrapper = new JPanel(new BorderLayout());
        toolbarWrapper.add(toolbar, BorderLayout.NORTH);
        toolbarWrapper.setBorder(BorderFactory.createLineBorder(Color.BLACK)); // 加邊框
        toolbarWrapper.setBackground(Color.WHITE);

        // 設定畫布區域
        JPanel canvasWrapper = new JPanel(new BorderLayout());
        canvasWrapper.add(canvas, BorderLayout.CENTER);
        canvasWrapper.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), 
                "Canvas", 
                TitledBorder.LEFT, 
                TitledBorder.TOP, 
                largeFont));
        canvasWrapper.setBackground(Color.WHITE);

        // 將畫布與工具列加入視窗
        add(toolbarWrapper, BorderLayout.WEST);
        add(canvasWrapper, BorderLayout.CENTER);

        // 設定工具列與畫布的互動
        toolbar.setCanvasPanel(canvas);

        setVisible(true);
    }

    // 設定所有UI元件的字體
    public static void setUIFont(javax.swing.plaf.FontUIResource f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }

    //選單列
    private JMenuBar createMenuBar(Font font) {
        JMenuBar menuBar = new JMenuBar();
        
        // File 選單 
        JMenu fileMenu = new JMenu("File");
        fileMenu.setFont(font);
        menuBar.add(fileMenu);
        
        // Edit 選單
        JMenu editMenu = new JMenu("Edit");
        editMenu.setFont(font);
        
        // Label
        JMenuItem labelMenuItem = new JMenuItem("Label");
        labelMenuItem.setFont(font);
        labelMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showLabelStyleDialog();
            }
        });
        
        // Group 
        JMenuItem groupMenuItem = new JMenuItem("Group");
        groupMenuItem.setFont(font);
        groupMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.groupSelectedShapes();
            }
        });
        
        // Ungroup 
        JMenuItem ungroupMenuItem = new JMenuItem("Ungroup");
        ungroupMenuItem.setFont(font);
        ungroupMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.ungroupSelectedShape();
            }
        });
        
        // 添加到編輯選單
        editMenu.add(labelMenuItem);  
        editMenu.addSeparator();
        editMenu.add(groupMenuItem);
        editMenu.add(ungroupMenuItem);
        menuBar.add(editMenu);
    
        return menuBar;
    }
    
    // 顯示標籤樣式對話框
    private void showLabelStyleDialog() {
        // 檢查是否有選中的形狀
        if (canvas.selectedShapes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請先選擇一個形狀", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 檢查選中的是否為組合形狀
        Object[] selectedShape = canvas.selectedShapes.get(0);
        if (selectedShape[0].equals("composite")) {
            JOptionPane.showMessageDialog(this, "無法為組合形狀設置標籤", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 獲取當前標籤設置（如果存在）
        String currentText = "Hi";
        String currentShape = "Oval";
        String currentColor = "yellow";
        String currentFontSize = "12";
        
        // 檢查選中形狀是否已有標籤設置
        if (selectedShape.length > 5 && selectedShape[5] != null) {
            Object[] labelSettings = (Object[]) selectedShape[5];
            currentText = (String) labelSettings[0];
            currentShape = (String) labelSettings[1];
            
            // 根據顏色對象反推顏色名稱
            Color labelColor = (Color) labelSettings[2];
            if (labelColor.getRed() == 255 && labelColor.getGreen() == 200 && labelColor.getBlue() == 200) {
                currentColor = "red";
            } else if (labelColor.getRed() == 255 && labelColor.getGreen() == 255 && labelColor.getBlue() == 200) {
                currentColor = "yellow";
            } else if (labelColor.getRed() == 200 && labelColor.getGreen() == 200 && labelColor.getBlue() == 255) {
                currentColor = "blue";
            } else if (labelColor.getRed() == 200 && labelColor.getGreen() == 255 && labelColor.getBlue() == 200) {
                currentColor = "green";
            }
            
            currentFontSize = String.valueOf(labelSettings[3]);
        }
        
        // 創建對話框
        JDialog labelDialog = new JDialog(this, "Custom Label Style", true);
        labelDialog.setSize(300, 330);
        labelDialog.setLocationRelativeTo(this);
        labelDialog.setResizable(false);
        
        // 使用灰色背景
        Color backgroundColor = new Color(204, 204, 204);
        
        // 創建主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(backgroundColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 創建標題標籤並設置樣式
        JLabel titleLabel = new JLabel("Custom Label Style", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBackground(new Color(240, 240, 240));
        titleLabel.setOpaque(true);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        // 設置標題面板
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.setMaximumSize(new Dimension(300, 30));
        
        // 創建輸入欄位面板
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBackground(backgroundColor);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        // 名稱欄位
        JLabel nameLabel = new JLabel("Name");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        JTextField nameField = new JTextField(currentText); // 使用當前設置
        nameField.setHorizontalAlignment(JTextField.CENTER); // 文字置中
        
        // 形狀欄位 - 使用下拉選單
        JLabel shapeLabel = new JLabel("Shape");
        shapeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        String[] shapes = {"Rect", "Oval"};
        JComboBox<String> shapeComboBox = new JComboBox<>(shapes);
        shapeComboBox.setSelectedItem(currentShape); // 使用當前設置
        ((JLabel)shapeComboBox.getRenderer()).setHorizontalAlignment(JLabel.CENTER); // 文字置中
        
        // 顏色欄位 - 使用下拉選單
        JLabel colorLabel = new JLabel("Color");
        colorLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        String[] colorNames = {"red", "yellow", "blue", "green"};
        JComboBox<String> colorComboBox = new JComboBox<>(colorNames);
        colorComboBox.setSelectedItem(currentColor); // 使用當前設置
        ((JLabel)colorComboBox.getRenderer()).setHorizontalAlignment(JLabel.CENTER); // 文字置中
        
        // 字體大小欄位 - 使用下拉選單
        JLabel fontSizeLabel = new JLabel("FontSize");
        fontSizeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        String[] fontSizes = {"10", "12", "14", "16", "18"};
        JComboBox<String> fontSizeComboBox = new JComboBox<>(fontSizes);
        fontSizeComboBox.setSelectedItem(currentFontSize); // 使用當前設置
        ((JLabel)fontSizeComboBox.getRenderer()).setHorizontalAlignment(JLabel.CENTER); // 文字置中
        
        // 添加組件到輸入面板
        inputPanel.add(nameLabel);
        inputPanel.add(nameField);
        inputPanel.add(shapeLabel);
        inputPanel.add(shapeComboBox);
        inputPanel.add(colorLabel);
        inputPanel.add(colorComboBox);
        inputPanel.add(fontSizeLabel);
        inputPanel.add(fontSizeComboBox);
        
        // 創建按鈕面板 - 修改為符合圖片的樣式
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10)); // 調整按鈕間距
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0)); // 增加上邊距
        
        // 創建按鈕 - 修改按鈕樣式以符合圖片
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cancelButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.setBackground(Color.WHITE); // 設置背景為白色
        cancelButton.setOpaque(true); // 確保背景色可見
        cancelButton.setBorderPainted(true); // 保留邊框
        
        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        okButton.setPreferredSize(new Dimension(100, 30));
        okButton.setBackground(Color.WHITE); // 設置背景為白色
        okButton.setOpaque(true); // 確保背景色可見
        okButton.setBorderPainted(true); // 保留邊框
        
        // 添加按鈕監聽器
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                labelDialog.dispose();
            }
        });
        
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 獲取用戶輸入
                String name = nameField.getText();
                String shape = (String) shapeComboBox.getSelectedItem();
                String colorName = (String) colorComboBox.getSelectedItem();
                String fontSizeStr = (String) fontSizeComboBox.getSelectedItem();
                
                // 將顏色名稱轉換為顏色對象
                Color color;
                switch (colorName.toLowerCase()) {
                    case "red":
                        color = new Color(255, 200, 200); // 淺紅色
                        break;
                    case "yellow":
                        color = new Color(255, 255, 200); // 淺黃色
                        break;
                    case "blue":
                        color = new Color(200, 200, 255); // 淺藍色
                        break;
                    case "green":
                        color = new Color(200, 255, 200); // 淺綠色
                        break;
                    default:
                        color = new Color(255, 255, 200); // 預設淺黃色
                }
                
                // 將字體大小轉換為整數
                int fontSize = Integer.parseInt(fontSizeStr);
                
                // 設置選中形狀的標籤
                canvas.setLabelToSelectedShape(name, shape, color, fontSize);
                labelDialog.dispose();
            }
        });
        
        // 添加按鈕到按鈕面板
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        
        // 將所有面板添加到主面板
        mainPanel.add(titlePanel);
        mainPanel.add(inputPanel);
        mainPanel.add(buttonPanel);
        
        // 將主面板添加到對話框
        labelDialog.add(mainPanel);
        
        // 顯示對話框
        labelDialog.setVisible(true);
    }
}
