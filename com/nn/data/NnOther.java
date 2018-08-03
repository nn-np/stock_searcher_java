package com.nn.data;

import com.nn.layout.NnPopController;
import com.nn.main.NnOnDragDropListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 提供其他功能，比如备份数据，弹出提示窗等方法
 * 为了复用代码，有的没的代码全丢到这里来了
 */
public class NnOther {
    // 备份数据
    public static void nnBackup(String source, String dest) throws IOException {
        File sour = new File(source);
        if(!sour.exists()) return;
        File des;
        if (dest != null) {
            des = new File(dest);
        } else {
            des = new File("nn_backup");
            if (!des.isDirectory() && des.exists() && !des.delete()) return;
            if(!des.mkdirs()) return;
            des = new File("nn_backup/" + sour.getName());
        }

        if(des.exists() && !des.delete()) return;

        Files.copy(sour.toPath(), des.toPath());
    }

    public static void nnBackup(String source) throws IOException {
        nnBackup(source, null);
    }

    // 去掉不必要的字符，将所有库存加起来
    public static double getQuality(char[] chars) {
        double value = 0;
        char[] chars1 = new char[chars.length];
        int len = 0;
        for (int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            if (c >= '0' && c <= '9' || c == '.') {
                chars1[len++] = c;
            } else {
                int len2 = chars.length - i - 1;
                char[] chars2 = new char[len2];
                for (int j = 0; j < len2; ++j) {
                    ++i;
                    chars2[j] = chars[i];
                }
                value += getQuality(chars2);
            }
        }
        return value + (len > 0 ? Double.parseDouble(new String(chars1)) : 0);
    }

    // 弹出提示窗口
    public void showInfo(String s, String info) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nn/layout/pop_layout.fxml"));
        Parent root;
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        NnPopController popController = fxmlLoader.getController();

        Scene scene = new Scene(root, 320, 100);
        scene.setFill(null);

        Stage stage = new Stage();
        stage.setScene(scene);
        popController.setStage(stage);
        popController.setText(info);
        popController.setTitle(s);
        popController.setFadeTransition(root);

        stage.show();
    }

    // 支持文件拖拽
    public void initDragDrop(Node root, NnOnDragDropListener listener) {
        root.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasUrl()) {
                event.acceptTransferModes(TransferMode.LINK);
            }
            event.consume();
        });
        root.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasUrl()) {
                String url = db.getUrl().replaceAll("file:/", "");
                System.out.println(url);
                listener.onDragDropped(url);
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }
}
