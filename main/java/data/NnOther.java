package main.java.data;

import javafx.application.Platform;
import main.java.layout.NnPopController;
import main.java.listener.NnOnDragDropListener;
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
        if (!sour.exists()) return;
        File des;
        if (dest != null) {
            des = new File(dest);
        } else {
            des = new File("nn_backup");
            if (!des.isDirectory() && des.exists() && !des.delete()) return;
            if (!des.mkdirs()) return;
            des = new File("nn_backup/" + sour.getName());
        }

        if (des.exists() && !des.delete()) return;

        Files.copy(sour.toPath(), des.toPath());
    }

    public static void nnBackup(String source) throws IOException {
        nnBackup(source, null);
    }

    // 去掉不必要的字符，将所有库存加起来
    public static double getQuality(char[] chars) {
        double value = 0;
        int len, index = 0;
        for (len = 0; len < chars.length; ++len) {
            char c = chars[len];
            if ((c < '0' || c > '9') && c != '.') {
                if (len > index)
                    value += Double.parseDouble(new String(chars, index, len - index));
                index = len + 1;
            }
        }
        return value + (len > index ? Double.parseDouble(new String(chars, index, len - index)) : 0);
    }

    // 得到字符串中的最大值
    public static double getMaxValue(char[] chars) {
        double value = -1;
        boolean flg = false;
        char[] cs = null;
        int i = 0;
        for (char c : chars) {
            if (!flg) {
                cs = new char[chars.length];
                flg = true;
            }
            if (c >= '0' && c <= '9' || c == '.') {
                cs[i++] = c;
                double d = Double.parseDouble(new String(cs));
                if (value < d) {
                    value = d;
                }
            } else {
                if (i > 0) {
                    flg = false;
                    i = 0;
                }
            }
        }
        return value;
    }

    private void toShowInfo(String s, String info) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main/resources/fxml/pop_layout.fxml"));
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

    // 弹出提示窗口
    public void showInfo(String s, String info) {
        Platform.runLater(() -> toShowInfo(s, info));
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

    public String getVersion() {
        return "v1.0.2 nnns 10.12";
    }
}
