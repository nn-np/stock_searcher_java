package main.java.start;

import main.java.layout.MainController;
import main.java.listener.NnDragListener;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
/**
 * 测试库存查找
 * 创建日期 2018.5.4
 * 注意：文件命名格式：新单（当日需要查库存的新单），库存（当前的最新库存），history（从系统中找的以前做过的单子）
 * 先这样明命名，上班看了再改
 * @author 徐世宁
 */

public class NnMain extends Application {
    private static boolean startFlg = false;// 开始方法，如果是称量打开，则flg为true
    public static void main(String[] args) {
        startFlg = args.length > 0 && args[0].equals("weighting");
        //startFlg = true;
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader;
        Scene scene;
        Parent root;
        if (startFlg) {// 如果是称量
            fxmlLoader = new FXMLLoader(getClass().getResource("/main/resources/fxml/weighing_layout.fxml"));
            root = fxmlLoader.load();
            scene = new Scene(root, 730, 430);
            primaryStage.setTitle("Weighting Management System");
        } else {// 其他打开查库存
            fxmlLoader = new FXMLLoader(getClass().getResource("/main/resources/fxml/main_layout.fxml"));
            root = fxmlLoader.load();
            scene = new Scene(root, 360, 127);
            MainController controller = fxmlLoader.getController();
            controller.setStage(primaryStage);
            primaryStage.setAlwaysOnTop(true);
            NnDragListener.addNnDragListener(primaryStage, root);// 添加拖动监听
            primaryStage.initStyle(StageStyle.TRANSPARENT);
        }

        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                primaryStage.close();
            }
        });

        scene.setFill(null);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image("/main/resources/image/语文.png"));

        primaryStage.show();

    }
}
