package com.nn.layout;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class NnPopController {
    public Label lb_text;
    public Label lb_title;

    private Stage mStage;

    public void setStage(Stage stage) {
        mStage = stage;
    }

    private void close() {
        mStage.close();
    }

    public void setTitle(String title) {
        lb_title.setText(title);
    }

    public void setText(String text) {
        lb_text.setText(text);
    }

    public void bt_ok(ActionEvent event) {
        close();
    }

    public void bt_close(ActionEvent event) {
        close();
    }
}
