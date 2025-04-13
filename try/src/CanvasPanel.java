import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class CanvasPanel extends JPanel {
    // 常數
    private static final Color SHAPE_COLOR = new Color(200, 200, 200);
    private static final Color SELECTION_COLOR = Color.BLUE;
    private static final Color SELECTION_FILL_COLOR = new Color(0, 0, 255, 50);
    private static final int CONTROL_POINT_SIZE = 10;
    private static final int ARROW_SIZE = 14;
    private static final int SELECTION_THRESHOLD = 5;
    
    // 模式和形狀相關
    private String currentMode = "select";
    private final ArrayList<Object[]> shapes = new ArrayList<>();
    private final ArrayList<Object[]> links = new ArrayList<>();
    public ArrayList<Object[]> selectedShapes = new ArrayList<>();
    
    // 連線和拖動相關
    private Object[] startShape = null, endShape = null, draggingObject = null;
    private int[] startControlPoint = null, currentMousePos = null;
    private boolean isDraggingLink = false, isDraggingSelection = false, isDraggingObject = false;
    private int dragStartX = -1, dragStartY = -1, dragCurrentX = -1, dragCurrentY = -1;
    private int lastDragX = -1, lastDragY = -1;

    public CanvasPanel() {
        setBackground(Color.WHITE);
        setupMouseListeners();
    }
    
    private void setupMouseListeners() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { handleMousePressed(e); }
            @Override
            public void mouseReleased(MouseEvent e) { handleMouseReleased(e); }
            @Override
            public void mouseMoved(MouseEvent e) {
                currentMousePos = new int[]{e.getX(), e.getY()};
                repaint();
            }
            @Override
            public void mouseDragged(MouseEvent e) { handleMouseDragged(e); }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }
    
    // 處理滑鼠事件
    private void handleMousePressed(MouseEvent e) {
        int x = e.getX(), y = e.getY();
        
        if (currentMode.equals("rect") || currentMode.equals("oval")) {
            // 創建新形狀
            int width = currentMode.equals("oval") ? 120 : 90;
            int height = currentMode.equals("oval") ? 80 : 120;
            shapes.add(new Object[]{currentMode, x, y, width, height, null});
        } else if (currentMode.equals("select")) {
            Object[] clickedShape = findShapeAt(x, y);
            
            if (clickedShape != null && !isShapeCoveredByOthers(clickedShape)) {
                // 選擇並準備拖動形狀
                selectedShapes.clear();
                selectedShapes.add(clickedShape);
                shapes.remove(clickedShape);
                shapes.add(clickedShape);
                
                isDraggingObject = true;
                draggingObject = clickedShape;
                lastDragX = x;
                lastDragY = y;
            } else {
                // 開始框選
                isDraggingSelection = true;
                dragStartX = dragCurrentX = x;
                dragStartY = dragCurrentY = y;
            }
        } else if (isLinkMode()) {
            // 開始拖曳連線
            startDraggingLink(x, y);
        }
        repaint();
    }
    
    private void handleMouseReleased(MouseEvent e) {
        int x = e.getX(), y = e.getY();
        
        if (currentMode.equals("select")) {
            if (isDraggingSelection) {
                isDraggingSelection = false;
                
                int x1 = Math.min(dragStartX, dragCurrentX);
                int y1 = Math.min(dragStartY, dragCurrentY);
                int x2 = Math.max(dragStartX, dragCurrentX);
                int y2 = Math.max(dragStartY, dragCurrentY);
                
                if (x2 - x1 > SELECTION_THRESHOLD && y2 - y1 > SELECTION_THRESHOLD) {
                    selectShapesInArea(new Rectangle(x1, y1, x2-x1, y2-y1));
                }
                
                dragStartX = dragStartY = dragCurrentX = dragCurrentY = -1;
            } else if (isDraggingObject) {
                isDraggingObject = false;
                draggingObject = null;
                lastDragX = lastDragY = -1;
            }
        } else if (isLinkMode() && isDraggingLink) {
            // 嘗試完成連線或取消
            if (startControlPoint != null) {
                completeDraggingLink(x, y);
            }
            // 無論是否成功連線，都重置拖曳狀態
            isDraggingLink = false;
            startControlPoint = null;
            startShape = null;
        }
        repaint();
    }
    
    private void handleMouseDragged(MouseEvent e) {
        int x = e.getX(), y = e.getY();
        
        if (isDraggingLink) {
            currentMousePos = new int[]{x, y};
        } else if (currentMode.equals("select")) {
            if (isDraggingSelection) {
                dragCurrentX = x;
                dragCurrentY = y;
            } else if (isDraggingObject && draggingObject != null) {
                int deltaX = x - lastDragX;
                int deltaY = y - lastDragY;
                
                moveShape(draggingObject, deltaX, deltaY);
                updateConnectedLinks();
                lastDragX = x;
                lastDragY = y;
            }
        }
        repaint();
    }
    
    // 連線相關方法
    private void startDraggingLink(int x, int y) {
        // 重置任何先前的拖曳狀態
        startShape = null;
        startControlPoint = null;
        isDraggingLink = false;
        
        // 檢查所有選中的物件，不僅是最上層的
        for (Object[] selectedShape : selectedShapes) {
            // 確保物件沒有被其他物件覆蓋
            if (!isShapeCoveredByOthers(selectedShape)) {
                if (isNearControlPoint(x, y, (int)selectedShape[1], 
                        (int)selectedShape[2], (int)selectedShape[3], 
                        (int)selectedShape[4], (String)selectedShape[0])) {
                    startShape = selectedShape;
                    startControlPoint = findControlPointAt(x, y);
                    if (startControlPoint != null) {
                        isDraggingLink = true;
                        return; // 找到起點後，立即返回
                    }
                }
            }
        }
    }
    
    private void completeDraggingLink(int x, int y) {
        int[] endControlPoint = findControlPointAt(x, y);
        if (endControlPoint != null) {
            endShape = findShapeAtControlPoint(x, y);
            if (endShape != null && endShape != startShape) {
                links.add(new Object[]{
                    currentMode, startShape, endShape,
                    startControlPoint[0], startControlPoint[1],
                    endControlPoint[0], endControlPoint[1]
                });
            }
        }
        
        // 無論成功與否，都清除狀態
        isDraggingLink = false;
        startControlPoint = null;
        startShape = null;
        endShape = null;
    }
    
    // 形狀查找和檢查方法
    private Object[] findShapeAt(int x, int y) {
        List<Object[]> shapesAtPoint = new ArrayList<>();
        
        for (Object[] shape : shapes) {
            if (isPointInShape(shape, x, y)) {
                shapesAtPoint.add(shape);
            }
        }
        
        if (shapesAtPoint.isEmpty()) return null;
        if (shapesAtPoint.size() == 1) return shapesAtPoint.get(0);
        
        // 返回最上層的形狀
        return shapesAtPoint.stream()
            .max((s1, s2) -> Integer.compare(shapes.indexOf(s1), shapes.indexOf(s2)))
            .orElse(null);
    }
    
    private boolean isShapeCoveredByOthers(Object[] shape) {
        int shapeIndex = shapes.indexOf(shape);
        if (shapeIndex == -1 || shapeIndex == shapes.size() - 1) return false;
        
        Rectangle shapeBounds = getBounds(shape);
        
        // 檢查是否被更高層級的形狀覆蓋
        for (int i = shapeIndex + 1; i < shapes.size(); i++) {
            if (shapeBounds.intersects(getBounds(shapes.get(i)))) {
                return true;
            }
        }
        return false;
    }
    
    private Rectangle getBounds(Object[] shape) {
        return new Rectangle((int)shape[1], (int)shape[2], (int)shape[3], (int)shape[4]);
    }
    
    private boolean isPointInShape(Object[] shape, int x, int y) {
        String type = (String) shape[0];
        int sx = (int) shape[1], sy = (int) shape[2];
        int sw = (int) shape[3], sh = (int) shape[4];
        
        if (type.equals("rect")) {
            return x >= sx && x <= sx + sw && y >= sy && y <= sy + sh;
        } else if (type.equals("oval")) {
            double centerX = sx + sw / 2.0, centerY = sy + sh / 2.0;
            double a = sw / 2.0, b = sh / 2.0;
            double normalizedX = (x - centerX) / a;
            double normalizedY = (y - centerY) / b;
            return normalizedX * normalizedX + normalizedY * normalizedY <= 1.0;
        } else if (type.equals("composite")) {
            ArrayList<Object[]> children = (ArrayList<Object[]>) shape[5];
            for (Object[] child : children) {
                if (isPointInShape(child, x, y)) return true;
            }
        }
        return false;
    }
    
    
    private void selectShapesInArea(Rectangle selectionArea) {
        // 收集區域內未被覆蓋的形狀
        ArrayList<Object[]> topShapes = new ArrayList<>();
        
        for (Object[] shape : shapes) {
            Rectangle bounds = getBounds(shape);
            if (selectionArea.contains(bounds) && !isShapeCoveredByOthers(shape)) {
                topShapes.add(shape);
            }
        }
        
        if (!topShapes.isEmpty()) {
            selectedShapes.clear();
            selectedShapes.addAll(topShapes);
        }
    }
    
    // 控制點相關方法
    private boolean isNearControlPoint(int x, int y, int shapeX, int shapeY, 
                                      int shapeWidth, int shapeHeight, String shapeType) {
        for (int[] p : getControlPoints(shapeX, shapeY, shapeWidth, shapeHeight, shapeType)) {
            if (distance(p[0], p[1], x, y) <= 10) return true;
        }
        return false;
    }
    
    private double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }
    
    private int[] findControlPointAt(int x, int y) {
        // 首先檢查所有選中形狀的控制點，而不只是最上層的
        for (Object[] shape : selectedShapes) {
            if (!isShapeCoveredByOthers(shape)) {
                int[] point = findControlPointInShape(shape, x, y);
                if (point != null) return point;
            }
        }
        
        // 再檢查其它未被覆蓋的形狀
        for (int i = shapes.size() - 1; i >= 0; i--) {
            Object[] shape = shapes.get(i);
            if (!selectedShapes.contains(shape) && !isShapeCoveredByOthers(shape)) {
                String type = (String) shape[0];
                int sx = (int) shape[1], sy = (int) shape[2];
                int sw = (int) shape[3], sh = (int) shape[4];
                
                if (isNearControlPoint(x, y, sx, sy, sw, sh, type)) {
                    int[] point = findControlPointInShape(shape, x, y);
                    if (point != null) return point;
                }
            }
        }
        
        return null;
    }
    
    private int[] findControlPointInShape(Object[] shape, int x, int y) {
        if (shape[0].equals("composite")) {
            for (Object[] child : (ArrayList<Object[]>) shape[5]) {
                int[] point = findControlPointInShape(child, x, y);
                if (point != null) return point;
            }
            return null;
        }
        
        String type = (String) shape[0];
        int sx = (int) shape[1], sy = (int) shape[2];
        int sw = (int) shape[3], sh = (int) shape[4];
        
        for (int[] p : getControlPoints(sx, sy, sw, sh, type)) {
            if (distance(p[0], p[1], x, y) <= 10) return p;
        }
        return null;
    }
    
    private Object[] findShapeAtControlPoint(int x, int y) {
        
        for (int i = shapes.size() - 1; i >= 0; i--) {
            Object[] shape = shapes.get(i);
            String type = (String) shape[0];
            int sx = (int) shape[1], sy = (int) shape[2];
            int sw = (int) shape[3], sh = (int) shape[4];
            
            if (isNearControlPoint(x, y, sx, sy, sw, sh, type) && !isShapeCoveredByOthers(shape)) {
                return shape;  // 返回第一個找到的未被覆蓋的形狀
            }
        }
        
        return null;
    }
    
    private int[][] getControlPoints(int x, int y, int w, int h, String shapeType) {
        if (shapeType.equals("oval")) {
            return new int[][]{
                {x + w / 2, y},    // 上
                {x + w / 2, y + h},// 下
                {x, y + h / 2},    // 左
                {x + w, y + h / 2} // 右
            };
        } else {
            return new int[][]{
                {x, y}, {x + w, y}, {x, y + h}, {x + w, y + h},      // 四角
                {x + w / 2, y}, {x + w / 2, y + h}, {x, y + h / 2}, {x + w, y + h / 2} // 中點
            };
        }
    }
    
    // 移動和更新相關方法
    private void moveShape(Object[] shape, int deltaX, int deltaY) {
        shape[1] = (int)shape[1] + deltaX;
        shape[2] = (int)shape[2] + deltaY;
        
        if (shape[0].equals("composite")) {
            for (Object[] child : (ArrayList<Object[]>) shape[5]) {
                moveShapeRecursively(child, deltaX, deltaY);
            }
        }
    }
    
    private void moveShapeRecursively(Object[] shape, int deltaX, int deltaY) {
        shape[1] = (int)shape[1] + deltaX;
        shape[2] = (int)shape[2] + deltaY;
        
        if (shape[0].equals("composite")) {
            for (Object[] child : (ArrayList<Object[]>) shape[5]) {
                moveShapeRecursively(child, deltaX, deltaY);
            }
        }
    }
    
    private void updateConnectedLinks() {
        Set<Object[]> affectedShapes = collectAffectedShapes();
        
        for (Object[] link : links) {
            Object[] linkStartShape = (Object[]) link[1];
            Object[] linkEndShape = (Object[]) link[2];
            
            if (affectedShapes.contains(linkStartShape)) {
                updateLinkControlPoint(link, linkStartShape, 3, 4);
            }
            
            if (affectedShapes.contains(linkEndShape)) {
                updateLinkControlPoint(link, linkEndShape, 5, 6);
            }
        }
    }
    
    private Set<Object[]> collectAffectedShapes() {
        Set<Object[]> affected = new HashSet<>();
        
        if (isDraggingObject && draggingObject != null) {
            affected.add(draggingObject);
            
            if (draggingObject[0].equals("composite")) {
                collectCompositeChildren(draggingObject, affected);
            }
        }
        
        return affected;
    }
    
    private void collectCompositeChildren(Object[] composite, Set<Object[]> collection) {
        if (!composite[0].equals("composite")) return;
        
        for (Object[] child : (ArrayList<Object[]>) composite[5]) {
            collection.add(child);
            if (child[0].equals("composite")) {
                collectCompositeChildren(child, collection);
            }
        }
    }
    
    private void updateLinkControlPoint(Object[] link, Object[] shape, int xIndex, int yIndex) {
        int origX = (int) link[xIndex], origY = (int) link[yIndex];
        String type = (String) shape[0];
        int sx = (int) shape[1], sy = (int) shape[2];
        int sw = (int) shape[3], sh = (int) shape[4];
        
        int[][] controlPoints = getControlPoints(sx, sy, sw, sh, type);
        int[] newPoint = findNearestControlPoint(controlPoints, origX, origY);
        
        link[xIndex] = newPoint[0];
        link[yIndex] = newPoint[1];
    }
    
    private int[] findNearestControlPoint(int[][] points, int origX, int origY) {
        if (points.length == 0) return new int[]{origX, origY};
        
        int[] nearest = points[0];
        double minDistance = Double.MAX_VALUE;
        
        for (int[] p : points) {
            double dist = distance(p[0], p[1], origX, origY);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = p;
            }
        }
        
        return nearest;
    }
    
    // 組合相關方法
    public void groupSelectedShapes() {
        if (selectedShapes.size() < 2) return;
        
        // 計算組合邊界
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        
        for (Object[] shape : selectedShapes) {
            int x = (int) shape[1], y = (int) shape[2];
            int w = (int) shape[3], h = (int) shape[4];
            
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + w);
            maxY = Math.max(maxY, y + h);
        }
        
        // 創建組合物件
        ArrayList<Object[]> children = new ArrayList<>(selectedShapes);
        Object[] composite = new Object[] {
            "composite", minX, minY, maxX - minX, maxY - minY, children
        };
        
        shapes.removeAll(selectedShapes);
        shapes.add(composite);
        
        selectedShapes.clear();
        selectedShapes.add(composite);
        
        repaint();
    }
    
    public void ungroupSelectedShape() {
        if (selectedShapes.size() != 1) return;
        
        Object[] selected = selectedShapes.get(0);
        if (!selected[0].equals("composite")) return;
        
        ArrayList<Object[]> children = (ArrayList<Object[]>) selected[5];
        
        shapes.remove(selected);
        shapes.addAll(children);
        
        selectedShapes.clear();
        selectedShapes.addAll(children);
        
        repaint();
    }
    
    // 設定和狀態方法
    public void setMode(String mode) {
        this.currentMode = mode;
        resetDragStates();
        repaint();
    }
    
    private void resetDragStates() {
        isDraggingLink = isDraggingSelection = isDraggingObject = false;
        startControlPoint = null;
        dragStartX = dragStartY = dragCurrentX = dragCurrentY = -1;
        draggingObject = null;
        lastDragX = lastDragY = -1;
    }
    
    private boolean isLinkMode() {
        return currentMode.equals("association") || 
               currentMode.equals("generalization") || 
               currentMode.equals("composition");
    }
    
    // 繪圖方法
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        
        drawShapes(g);
        drawSelectionHighlights(g);
        drawConnections(g);
        drawLabels(g);
        drawControlPoints(g);
        drawDraggingLink(g);
        drawSelectionBox(g);
    }
    
    private void drawShapes(Graphics g) {
        for (Object[] shape : shapes) {
            drawShape(g, shape);
        }
    }
    
    private void drawShape(Graphics g, Object[] shape) {
        String type = (String) shape[0];
        int x = (int) shape[1], y = (int) shape[2];
        int w = (int) shape[3], h = (int) shape[4];
        
        g.setColor(SHAPE_COLOR);
        if (type.equals("rect")) {
            g.fillRect(x, y, w, h);
        } else if (type.equals("oval")) {
            g.fillOval(x, y, w, h);
        } else if (type.equals("composite")) {
            for (Object[] child : (ArrayList<Object[]>) shape[5]) {
                drawShape(g, child);
            }
        }
    }
    
    private void drawSelectionHighlights(Graphics g) {
        if (selectedShapes.isEmpty()) return;
        
        g.setColor(SELECTION_COLOR);
        for (Object[] shape : selectedShapes) {
            drawSelectionHighlight(g, shape);
        }
    }
    
    private void drawSelectionHighlight(Graphics g, Object[] shape) {
        String type = (String) shape[0];
        int x = (int) shape[1], y = (int) shape[2];
        int w = (int) shape[3], h = (int) shape[4];
        
        if (type.equals("rect")) {
            g.drawRect(x-2, y-2, w+4, h+4);
        } else if (type.equals("oval")) {
            g.drawOval(x-2, y-2, w+4, h+4);
        } else if (type.equals("composite")) {
            for (Object[] child : (ArrayList<Object[]>) shape[5]) {
                drawSelectionHighlight(g, child);
            }
        }
    }
    
    private void drawConnections(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.BLACK);
        Stroke original = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        for (Object[] link : links) {
            drawLink(g, link);
        }
        
        g2d.setStroke(original);
    }
    
    private void drawLink(Graphics g, Object[] link) {
        String type = (String) link[0];
        int x1 = (int) link[3], y1 = (int) link[4];
        int x2 = (int) link[5], y2 = (int) link[6];
        
        // 繪製直線
        g.drawLine(x1, y1, x2, y2);
        drawArrow(g, x1, y1, x2, y2, type);
        
        // 繪製控制點
        drawControlPoint(g, x1, y1);
        drawControlPoint(g, x2, y2);
    }
    
    private void drawArrow(Graphics g, int x1, int y1, int x2, int y2, String type) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        
        if (type.equals("generalization")) {
            // 繪製繼承關係箭頭（空心三角形）
            int[] xPoints = {
                x2, 
                x2 - (int) (ARROW_SIZE * Math.cos(angle - Math.PI / 6)), 
                x2 - (int) (ARROW_SIZE * Math.cos(angle + Math.PI / 6))
            };
            int[] yPoints = {
                y2, 
                y2 - (int) (ARROW_SIZE * Math.sin(angle - Math.PI / 6)), 
                y2 - (int) (ARROW_SIZE * Math.sin(angle + Math.PI / 6))
            };
            
            Color orig = g.getColor();
            g.setColor(Color.WHITE);
            g.fillPolygon(xPoints, yPoints, 3);
            g.setColor(orig);
            g.drawPolygon(xPoints, yPoints, 3);
        } else if (type.equals("composition")) {
            // 繪製組合關係箭頭（實心菱形）
            int size = ARROW_SIZE;
            int[] xPoints = {
                x2,
                x2 - (int)(size * Math.cos(angle - Math.PI/4)),
                x2 - (int)(size * Math.sqrt(2) * Math.cos(angle)),
                x2 - (int)(size * Math.cos(angle + Math.PI/4))
            };
            int[] yPoints = {
                y2,
                y2 - (int)(size * Math.sin(angle - Math.PI/4)),
                y2 - (int)(size * Math.sqrt(2) * Math.sin(angle)),
                y2 - (int)(size * Math.sin(angle + Math.PI/4))
            };
            
            g.fillPolygon(xPoints, yPoints, 4);
        } else if (type.equals("association")) {
            // 繪製關聯關係箭頭（開放箭頭）
            g.drawLine(x2, y2, 
                      x2 - (int) (ARROW_SIZE * Math.cos(angle - Math.PI / 6)), 
                      y2 - (int) (ARROW_SIZE * Math.sin(angle - Math.PI / 6)));
            g.drawLine(x2, y2, 
                      x2 - (int) (ARROW_SIZE * Math.cos(angle + Math.PI / 6)), 
                      y2 - (int) (ARROW_SIZE * Math.sin(angle + Math.PI / 6)));
        }
    }
    
    private void drawControlPoints(Graphics g) {
        if (selectedShapes.isEmpty()) return;
        
        g.setColor(Color.BLACK);
        for (Object[] shape : selectedShapes) {
            if (!isShapeCoveredByOthers(shape)) {
                drawShapeControlPoints(g, shape);
            }
        }
    }
    
    private void drawShapeControlPoints(Graphics g, Object[] shape) {
        String type = (String) shape[0];
        int x = (int) shape[1], y = (int) shape[2];
        int w = (int) shape[3], h = (int) shape[4];
        
        if (type.equals("composite")) {
            for (Object[] child : (ArrayList<Object[]>) shape[5]) {
                if (!isShapeCoveredByOthers(child)) {
                    drawShapeControlPoints(g, child);
                }
            }
        } else {
            for (int[] p : getControlPoints(x, y, w, h, type)) {
                drawControlPoint(g, p[0], p[1]);
            }
        }
    }
    
    private void drawControlPoint(Graphics g, int x, int y) {
        int size = CONTROL_POINT_SIZE;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(x - size/2, y - size/2, size, size);
    }
    
    private void drawDraggingLink(Graphics g) {
        if (!isDraggingLink || startControlPoint == null || currentMousePos == null) return;
        
        g.setColor(Color.BLACK);
        
        // 直接從起點到當前滑鼠位置繪製線段
        g.drawLine(startControlPoint[0], startControlPoint[1], currentMousePos[0], currentMousePos[1]);
        drawArrow(g, startControlPoint[0], startControlPoint[1], 
                 currentMousePos[0], currentMousePos[1], currentMode);
    }
    
    private void drawSelectionBox(Graphics g) {
        if (!isDraggingSelection || dragStartX == -1 || dragCurrentX == -1) return;
        
        int x = Math.min(dragStartX, dragCurrentX);
        int y = Math.min(dragStartY, dragCurrentY);
        int width = Math.abs(dragCurrentX - dragStartX);
        int height = Math.abs(dragCurrentY - dragStartY);
        
        g.setColor(SELECTION_FILL_COLOR);
        g.fillRect(x, y, width, height);
        
        g.setColor(SELECTION_COLOR);
        g.drawRect(x, y, width, height);
    }

    // 標籤相關方法
    public void setLabelToSelectedShape(String labelText, String labelShape, Color labelColor, int fontSize) {
        if (selectedShapes.isEmpty()) return;
        
        Object[] shape = selectedShapes.get(0);
        if (shape[0].equals("composite")) return; // 不支持為組合形狀設置標籤
        
        // 創建標籤設置並存儲在形狀的第6個元素
        Object[] labelSettings = {labelText, labelShape, labelColor, fontSize};
        shape[5] = labelSettings;
        
        repaint();
    }

    private void drawLabels(Graphics g) {
        for (Object[] shape : shapes) {
            if (shape.length > 5 && shape[5] != null && !shape[0].equals("composite")) {
                drawLabel(g, shape);
            }
        }
    }
    
    private void drawLabel(Graphics g, Object[] shape) {
        int x = (int) shape[1], y = (int) shape[2];
        int w = (int) shape[3], h = (int) shape[4];
        Object[] labelSettings = (Object[]) shape[5];
        
        if (labelSettings == null) return;
        
        String text = (String) labelSettings[0];
        String labelShape = (String) labelSettings[1];
        Color color = (Color) labelSettings[2];
        int fontSize = (int) labelSettings[3];
        
        // 保存原始字體和顏色
        Font originalFont = g.getFont();
        Color originalColor = g.getColor();
        
        // 設置字體
        g.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        FontMetrics fm = g.getFontMetrics();
        
        // 計算文字位置
        int textWidth = fm.stringWidth(text);
        int labelWidth = 80;  
        int labelHeight = 60;  
        
        // 計算標籤位置和文字位置
        int labelX = x + (w - labelWidth) / 2;
        int labelY = y + (h - labelHeight) / 2;
        int textX = labelX + (labelWidth - textWidth) / 2;
        int textY = labelY + (labelHeight + fm.getAscent() - fm.getDescent()) / 2;
        
        // 根據標籤形狀繪製標籤背景
        g.setColor(color);
        if (labelShape.equalsIgnoreCase("rect")) {
            g.fillRect(labelX, labelY, labelWidth, labelHeight);
        } else if (labelShape.equalsIgnoreCase("oval")) {
            g.fillOval(labelX, labelY, labelWidth, labelHeight);
        }

        g.setColor(Color.BLACK);
        g.drawString(text, textX, textY);
        
        // 恢復原始設置
        g.setFont(originalFont);
        g.setColor(originalColor);
    }
}
