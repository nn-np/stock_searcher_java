package com.nn.layout;


import com.nn.data.NnAccdbReader;
import com.nn.main.Control;
import com.nn.main.NnListener;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;


public class MainController {
    private NnAccdbReader mConfigurationReader;
    private String news = null;
    private Control mControl;
    private int isStart = 0;

    private Stage mStage;


    public AnchorPane root;
    public Button bt_start;
    public ProgressBar progressBar;
    public Label label;
    public Button bt_select;
    public TextField tf_main;

    public MainController() {
        try {
            mConfigurationReader = new NnAccdbReader("nnns.accdb");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        Platform.runLater(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tf_main.setFocusTraversable(false);
            initDragDrop();
        });
    }

    public void setStage(Stage stage) {
        mStage = stage;
    }

    // 支持文件拖拽
    private void initDragDrop() {
        root.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasUrl()) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });
        root.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasUrl()) {
                String url = db.getUrl().replaceAll("file:/", "");
                System.out.println(url);
                String str = url.substring(url.lastIndexOf('.') + 1);
                if (str.equals("xls") || str.equals("xlsx")) {
                    tf_main.setText(url);
                    news = url;
                } else {
                    tf_main.setText("无效！");
                    news = null;
                }
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    public void select(ActionEvent event) {// 选择按钮事件

        toStop();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("打开新单表格");
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Excel 文件(.xls .xlsx)", "*.xls", "*.xlsx");
        fileChooser.getExtensionFilters().add(extensionFilter);

        String paths = null;
        try {
            ResultSet resultSet = mConfigurationReader.getResultSet("select * from nn where key = 'history'");
            if (resultSet.next()) {
                paths = resultSet.getString("_value");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (paths != null) {
            fileChooser.setInitialDirectory(new File(paths));
        }
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            news = file.getPath();
            tf_main.setText(news);
        } else {
            return;
        }

        try {
            if (paths == null) {
                mConfigurationReader.execute("insert into nn values ('history','" + file.getParent() + "')");
            } else {
                mConfigurationReader.execute("update nn set key = 'history',_value='" + file.getParent() + "' where key = 'history'");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void start(ActionEvent event) {// 开始按钮事件
        if (isStart == 0) {
            toStart();
            mControl = new Control(news, new OnNnListener());
        } else if (isStart == 1) {
            toStop();
            mControl.stop();
        } else if (isStart == 2) {
            toOpenTable();
        }
    }

    private void toOpenTable() {// 打开excel表格
        //System.out.println("cmd /c start " + news);
        try {
            Runtime.getRuntime().exec("cmd /c start " + news);
        } catch (IOException e) {
            e.printStackTrace();
        }
        toStop();
    }

    private void toStart() {
        label.setText("正在搜索...");
        label.setVisible(true);
        progressBar.setVisible(true);
        bt_select.setVisible(false);
        tf_main.setVisible(false);
        bt_start.setText("取消");
        isStart = 1;
    }

    private void toStop() {
        tf_main.setText(null);
        news = null;
        label.setVisible(false);
        progressBar.setVisible(false);
        bt_select.setVisible(true);
        tf_main.setVisible(true);
        bt_start.setText("开始");
        isStart = 0;
    }

    private void toComplete() {
        label.setVisible(false);
        progressBar.setVisible(false);
        bt_select.setVisible(true);
        tf_main.setVisible(true);
        tf_main.setText("搜索完成，结果已写入表格最后两列！");
        bt_start.setText("打开表格");
        isStart = 2;
    }

    public void close(ActionEvent event) {
        if (mControl != null)
            mControl.stop();
        mStage.close();
    }

    /**
     * 用于监听工作线程的状态
     */
    class OnNnListener implements NnListener {// 这里的方法都是再工作线程中被调用，注意！！

        @Override
        public void progress(double progress) {
            System.out.println(progress);

            Platform.runLater(() -> progressBar.setProgress(progress));

        }

        @Override
        public void errorInfo(String info) {
            System.out.println(info);
            Platform.runLater(()->{
                //tf_main.setText(info);
                /*Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误!");
                alert.setHeaderText(info);
                alert.show();*/
                toStop();
                try {
                    showInfo("错误！！", info);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        public void complete() {
            Platform.runLater(MainController.this::toComplete);
        }
    }

    private void showInfo(String s, String info) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nn/layout/pop_layout.fxml"));
        Parent root = fxmlLoader.load();

        NnPopController popController = fxmlLoader.getController();

        Scene scene = new Scene(root, 320, 100);

        Stage stage = new Stage();
        popController.setStage(stage);
        popController.setText(info);
        popController.setTitle(s);

        stage.initOwner(mStage);
        stage.setScene(scene);
        stage.sizeToScene();
        //stage.getIcons().add(null);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        stage.showAndWait();
    }

}
