package com.nn.layout;

import com.nn.data.NnOther;
import com.nn.data.NnProperties;
import com.nn.main.Control;
import com.nn.main.NnListener;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

public class MainController {
    private NnProperties mNnProperties;
    private NnOther mNnOther;
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
        mNnOther = new NnOther();
        try {
            mNnProperties = new NnProperties("nn.xml");
        } catch (IOException | XMLStreamException e) {
            mNnOther.showInfo("错误！！", "配置文件读取错误!");
            e.printStackTrace();
        }

        Platform.runLater(() -> {
            tf_main.setFocusTraversable(false);
            mNnOther.initDragDrop(root, url -> {
                toStop();
                String str = url.substring(url.lastIndexOf('.'));
                if (str.equals(".xls") || str.equals(".xlsx")) {
                    tf_main.setText(url);
                    news = url;
                } else {
                    tf_main.setText("文件无效！");
                    news = null;
                }
            });
        });
    }

    public void setStage(Stage stage) {
        mStage = stage;
    }

    public void select(ActionEvent event) {// 选择按钮事件

        toStop();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择新单表格");
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Excel 文件(.xls .xlsx)", "*.xls", "*.xlsx");
        fileChooser.getExtensionFilters().add(extensionFilter);

        String paths = mNnProperties.getProperty("old_path", null);

        if (paths != null) {
            fileChooser.setInitialDirectory(new File(paths));
        }
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            news = file.getPath();
            tf_main.setText(news);
            mNnProperties.put("old_path", file.getParent());
            try {
                mNnProperties.submit();
            } catch (IOException e) {
                e.printStackTrace();
            }
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

        mNnOther.showInfo("提示", "搜索完成，结果已写入表格！");

        label.setVisible(false);
        progressBar.setVisible(false);
        bt_select.setVisible(true);
        tf_main.setVisible(true);
        tf_main.setText("搜索完成，结果已写入表格！");
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

            double pr = progress * 100;
            String str = String.format("%.2f", pr);

            Platform.runLater(() -> {
                progressBar.setProgress(progress);
                label.setText("正在搜索..." + str + "%");
            });

        }

        @Override
        public void errorInfo(String info) {
            System.out.println(info);
            Platform.runLater(() -> {
                toStop();
                tf_main.setText(info);
                mNnOther.showInfo("错误！！", info);
            });
        }

        @Override
        public void complete() {
            Platform.runLater(MainController.this::toComplete);
        }
    }

}
