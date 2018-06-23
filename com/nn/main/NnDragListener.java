package com.nn.main;

import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * 窗体拖动监听类
 * 这里是大神的代码，值得借鉴
 * 果然是代码越简单粗暴越好用
 */

public class NnDragListener{
    private static double xOffset = 0, yOffset = 0;

    public static void addNnDragListener(Stage stage) {
        stage.addEventFilter(MouseEvent.MOUSE_PRESSED,event -> {
            xOffset = event.getScreenX() - stage.getX();
            yOffset = event.getScreenY() - stage.getY();
        });

        stage.addEventFilter(MouseEvent.MOUSE_DRAGGED,event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }
}
