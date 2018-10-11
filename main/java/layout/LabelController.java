package main.java.layout;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import main.java.data.NnExcelReader;
import main.java.data.NnOther;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class LabelController {
    private NnOther nnOther;
    private Stage mStage;
    private Scene parent;

    private String[] urls;
    private int flg = 0;
    private Map<String,String> map;
    private NnExcelReader mReader;

    public AnchorPane root;
    public Button bt_start;
    public Label label;
    public Label label2;
    public Label nnns;

    public LabelController() {
        nnOther = new NnOther();
        urls = new String[2];

        Platform.runLater(() -> {
            nnns.setText(nnOther.getVersion());
            nnOther.initDragDrop(root, url -> {
                String str = url.substring(url.lastIndexOf('.'));
                if (str.equals(".xls") || str.equals("xlsx")) {
                    urls[0] = url;
                    ++flg;
                } else if (str.equals(".txt")) {
                    urls[1] = url;
                    ++flg;
                } else {
                    nnOther.showInfo("提示！", "文件无效!");
                }
                label2.setText(label2.getText() + url + "\n");
                doIt();
            });
        });
    }

    private void doIt() {
        if (urls[0] == null) {
            label.setText("Ⅱ、选择excel文件");
        } else if (urls[1] == null) {
            label.setText("Ⅱ、选择标签文件（txt）");
        }else{
            label.setText("");
        }
        if (flg > 1) {
            new Thread(this::merge).start();
            flg = 1;
        }
    }

    // 合并标签
    private void merge() {
        map = new HashMap<>();
        try {
            setMess("正在匹配...");
            initLabel();
            readExcel();
            outPut();
            nnOther.showInfo("提示！", "结果已写入表格！");
            urls[1] = null;
            setMess("已完成，选择其他标签文件");
        } catch (IOException e) {
            nnOther.showInfo("错误！", "文件读取错误！");
            e.printStackTrace();
        }
    }

    private void setMess(String mess) {
        Platform.runLater(()->{
            label.setText(mess);
        });
    }

    private void outPut() {
        try {
            mReader.output();
        } catch (IOException e) {
            nnOther.showInfo("提示！", "文件被占用！");
            e.printStackTrace();
        }
    }

    private void initLabel() throws IOException {
        InputStreamReader in = new InputStreamReader(new FileInputStream(urls[1]));
        BufferedReader read = new BufferedReader(in);
        String str;
        while ((str = read.readLine()) != null) {
            if (str.length() > 5) {
                String orderId = getStr(str, "ID: ");
                map.put(orderId, str);
                System.out.println(orderId);
            }
        }
    }

    private void readExcel() {
        try {
            mReader = new NnExcelReader(urls[0]);
            int size = mReader.getRowSize(1);
            copyRow(0,0, 0, 10);
            mReader.setCellValue(1, 0, 12, "标签", null);
            for (int i = 1; i < size; ++i) {
                String wono = mReader.getCellString(1, i, 0);
                int row = getRowIndex(wono);
                String lab = map.get(mReader.getCellString(row, 1));
                if (row > -1 && lab != null) {
                    copyRow(i, row, 0, 15);
                    mReader.setCellValue(1, i, 12, lab, null);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getRowIndex(String wono) {
        int size = mReader.getRowSize();
        for (int i = 0; i < size; ++i) {
            if (wono.equals(mReader.getCellString(i, 0))) {
                return i;
            }
        }
        return -1;
    }

    // 拷贝excel的行，这里默认从sheet0拷贝到sheet1
    private void copyRow(int row_0, int row, int i1, int i2) {
        for (int i = i1; i <= i2; ++i) {
            mReader.setCellValue(1, row_0, i, mReader.getCellString(row, i), null);
        }
    }

    private String getStr(String strs,String o_str) {
        char[] chars = strs.toCharArray();
        String str = "";
        int i_1 = strs.indexOf(o_str);
        if (i_1 > 0) {
            i_1 += o_str.length();
            int i_2 = getNextIndex(i_1, chars);
            str = new String(chars, i_1, i_2 - i_1);
        }
        return str;
    }

    private int getNextIndex(int i_1, char[] chars) {
        int i;
        for (i = i_1; i < chars.length; ++i) {
            if (chars[i] == '|') {
                break;
            }
        }
        return i;
    }

    public void setStage(Stage stage) {
        mStage = stage;
        parent = stage.getScene();
    }

    public void back() {
        mStage.setScene(parent);
    }

    public void start() {
        try {
            Runtime.getRuntime().exec("cmd /c " + urls[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
