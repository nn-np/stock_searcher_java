package com.nn.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
/**
 * 测试库存查找
 * 创建日期 2018.5.4
 * 注意：文件命名格式：新单（当日需要查库存的新单），库存（当前的最新库存），history（从系统中找的以前做过的单子）
 * 先这样明命名，上班看了再改
 * @author 徐世宁
 */

public class DataManager extends Application {
    public static Stage mStage;
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        mStage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("/com/nn/layout/main_layout.fxml"));
        Scene scene = new Scene(root, 360, 127);
        scene.setFill(null);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image("语文.png"));
        primaryStage.initStyle(StageStyle.TRANSPARENT);

        NnDragListener.addNnDragListener(primaryStage);// 添加拖动监听

        primaryStage.show();

    }
}
