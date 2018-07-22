package com.nn.layout;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.*;

public class NnPopController {
    public Label lb_text;
    public Label lb_title;

    private Stage mStage;

    public void setStage(Stage stage) {
        mStage = stage;

        stage.setAlwaysOnTop(true);
        stage.sizeToScene();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        stage.setY(dim.height - 280);
        stage.setX(dim.width / 2 - 150);
    }

    public void setFadeTransition(Parent root) {
        new Thread(()->{
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 动画
            FadeTransition ft = new FadeTransition(Duration.millis(500), root);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.play();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(()->mStage.close());
        }).start();
    }

    public void setTitle(String title) {
        lb_title.setText(title);
    }

    public void setText(String text) {
        lb_text.setText(text);
    }
}
