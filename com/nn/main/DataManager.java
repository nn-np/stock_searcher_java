package com.nn.main;

import com.nn.layout.MainController;
import com.nn.layout.NnDragListener;
import com.nn.layout.WeighingController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Screen;
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
    private static boolean startFlg = false;// 开始方法，如果是称量打开，则flg为true
    public static void main(String[] args) {
        startFlg = args.length > 0 && args[0].equals("weighing");
        //startFlg = true;
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = null;
        Scene scene = null;
        Parent root = null;
        if (startFlg) {// 如果是称量
            fxmlLoader = new FXMLLoader(getClass().getResource("/com/nn/layout/weighing_layout.fxml"));
            root = fxmlLoader.load();
            root.setStyle("-fx-background-size: 360 127");
            scene = new Scene(root, 360, 360);
            WeighingController controller = fxmlLoader.getController();
            controller.setStage(primaryStage);
        } else {// 其他打开查库存
            fxmlLoader = new FXMLLoader(getClass().getResource("/com/nn/layout/main_layout.fxml"));
            root = fxmlLoader.load();
            root.setStyle("-fx-background-size: 360 127");
            scene = new Scene(root, 360, 127);
            MainController controller = fxmlLoader.getController();
            controller.setStage(primaryStage);
        }

        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                primaryStage.close();
            }
        });

        scene.setFill(null);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image("语文.png"));
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setAlwaysOnTop(true);

        NnDragListener.addNnDragListener(primaryStage,root);// 添加拖动监听

        primaryStage.show();

    }
}
