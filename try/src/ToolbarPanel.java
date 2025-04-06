import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ToolbarPanel extends JPanel {
    private JButton selectedButton = null; // 記錄當前被選中的按鈕
    private CanvasPanel canvasPanel;

    public ToolbarPanel() {
        setLayout(new GridLayout(6, 1));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 按鈕名稱與對應的圖標
        String[][] buttons = {
            {"select", "try/icons/select.png"},
            {"association", "try/icons/association.png"},
            {"generalization", "try/icons/generalization.png"},
            {"composition", "try/icons/composition.png"},
            {"rect", "try/icons/rect.png"},
            {"oval", "try/icons/oval.png"}
        };

        for (String[] btn : buttons) {
            String name = btn[0];
            String iconPath = btn[1];

            // 創建圖片圖標
            ImageIcon icon = new ImageIcon(iconPath);
            Image scaledImg = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH); // 調整圖片大小

            // 創建按鈕並設置大小
            JButton button = new JButton(new ImageIcon(scaledImg));
            button.setPreferredSize(new Dimension(70, 70)); // 設置按鈕尺寸，使其變大
            button.setBackground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createLineBorder(Color.BLACK));

            // 按鈕的提示文本
            button.setToolTipText(name);

            // 設置按鈕的顏色和模式
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (selectedButton != null) {
                        selectedButton.setBackground(Color.WHITE); // 還原之前的按鈕
                    }
                    button.setBackground(Color.BLACK); // 按鈕變黑
                    selectedButton = button;
                    if (canvasPanel != null) {
                        canvasPanel.setMode(name); // 使用 CanvasPanel 更新模式
                    }
                }
            });

            add(button);
        }
    }

    // 設定畫布面板，透過這個方法來傳遞 canvasPanel
    public void setCanvasPanel(CanvasPanel canvasPanel) {
        this.canvasPanel = canvasPanel;
    }
}
