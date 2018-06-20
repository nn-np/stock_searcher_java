package com.nn.layout;


import com.nn.data.NnAccdbReader;
import com.nn.main.Control;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.nn.main.DataManager.mStage;

public class MainController {

    private NnAccdbReader mConfigurationReader;
    private String news = null;

    public TextField tf_main;

    public MainController() {
        try {
            mConfigurationReader = new NnAccdbReader("nnns.accdb");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        Platform.runLater(()->{
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tf_main.setFocusTraversable(false);
        });
    }

    public void select(ActionEvent event) {// 选择按钮事件
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("打开新单文件");
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
        try {
            new Control(news);
            new Thread(()->{
                for (int i = 9; i > 0; --i) {
                    int finalI = i;
                    Platform.runLater(() -> tf_main.setText("库存查找结束，即将退出... " + finalI + ""));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Platform.runLater(() -> mStage.close());
            }).start();
        } catch (IOException | SQLException | ParserConfigurationException | SAXException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void close(ActionEvent event) {
        mStage.close();
    }
}
