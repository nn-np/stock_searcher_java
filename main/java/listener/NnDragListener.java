package main.java.listener;

import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * 窗体拖动
 * 这里是大神的代码，值得借鉴
 * 果然是代码越简单粗暴越好用
 */

public class NnDragListener {
    private static double xOffset = 0, yOffset = 0;
    private static boolean dragging = false;

    // 把这个方法写成static方法也是我的主张，和大神无关...
    public static void addNnDragListener(Stage stage, Node node) {
        stage.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            xOffset = event.getScreenX() - stage.getX();
            yOffset = event.getScreenY() - stage.getY();
        });

        stage.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!dragging) return;
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
        // 后面是我加的，用于检测是否是拖动
        node.setOnMouseDragged(event -> dragging = event.getButton() == MouseButton.PRIMARY);
        node.setOnMouseReleased(event -> dragging = false);
    }
}
