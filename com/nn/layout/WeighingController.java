package com.nn.layout;

import com.nn.data.*;
import com.nn.main.WeightingManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class WeighingController {
    private NnOther mNnOther;
    private NnProperties mNnProperties;// 配置文件
    private WeightingManager mManager;// 数据提交控制类
    private String stockUrl = null;// 拖动的excel表格url
    private int submitFlg = 0;// 0打开文件  1提交数据
    private Vector<WeightingInfo> mVector;

    private Stage mStage;

    public TextArea ta_weight_search;
    public TextArea tArea_weight;
    public AnchorPane root;
    public Button bt_submit;

    public WeighingController() {
        mNnOther = new NnOther();
        try {
            mNnProperties = new NnProperties("nn.xml");
        } catch (IOException | XMLStreamException e) {
            mNnOther.showInfo("错误！！", "配置文件读取发生错误！");
            e.printStackTrace();
        }

        mManager = new WeightingManager();

        Platform.runLater(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ta_weight_search.setFocusTraversable(false);
            tArea_weight.setEditable(false);
            mManager.setMessageBox(tArea_weight);
            mNnOther.initDragDrop(root, url -> {
                String str = url.substring(url.lastIndexOf('.'));
                if (str.equals(".xls") || str.equals(".xlsx")) {
                    tArea_weight.setText(url + "\n");
                    stockUrl = url;
                    toSubmit();
                } else {
                    mNnOther.showInfo("提示！", "文件无效！");
                }
            });
        });
    }

    private void toSubmit() {
        bt_submit.setText("提交");
        mManager.setExcel(stockUrl);
        tArea_weight.setText("咳咳，一定要确保数据准确啊！^^");
        submitFlg = 1;
    }

    public void setStage(Stage primaryStage) {
        mStage = primaryStage;
    }

    public void close(ActionEvent event) {
        mStage.close();
    }

    public void submit(ActionEvent event) {
        if (submitFlg == 0) {// 选择表格
            toSelectTable();
        } else if (submitFlg == 1) {// 提交信息
            mManager.submit();
            bt_submit.setText("选择");
            submitFlg = 0;
        } else if (submitFlg == 2) {// 导出库存信息
            outPut();
        }
    }

    private void outPut() {
        bt_submit.setText("选择");
        submitFlg = 0;
        new Thread(()->toOutPut()).start();
    }

    private void toOutPut() {
        NnExcelReader reader = new NnExcelReader();
        reader.createSheet(new SimpleDateFormat("M.d.yy").format(new Date()) + "库存查找");
        reader.setUrl("weightingInfo.xlsx");
        reader.setCellValue(0, 0, "日期").setCellValue(0,1,"order ID").setCellValue(0, 2, "原因").setCellValue(0, 3, "质量")
                .setCellValue(0, 4, "坐标").setCellValue(0, 5, "袋");
        int i = 1;
        for (WeightingInfo info : mVector) {
            reader.setCellValue(i,0,info.getDate());
            reader.setCellValue(i,1,info.getOrderId());
            reader.setCellValue(i,2,info.getCause());
            reader.setCellValue(i, 3, "" + info.getQuality() + "mg");
            if (!info.canDelete()) {
                reader.setCellValue(i, 4, info.getCoordinate());
                reader.setCellValue(i, 5, info.getPackages());
            }
            ++i;
        }
        try {
            reader.output();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Runtime.getRuntime().exec("cmd /c start weightingInfo.xlsx");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void toSelectTable() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择表格");
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Excel 文件(.xls .xlsx)", "*.xls", "*.xlsx");
        fileChooser.getExtensionFilters().add(extensionFilter);

        String path = mNnProperties.getProperty("weight_path", null);

        if (path != null) {
            fileChooser.setInitialDirectory(new File(path));
        }

        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            stockUrl = file.getPath();
            toSubmit();
            tArea_weight.setText(stockUrl + "\n");
            mNnProperties.put("weight_path", file.getParent());
            try {
                mNnProperties.submit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void search(ActionEvent event) {
        tArea_weight.clear();
        new Thread(() -> {
            try {
                toSearch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void toSearch() throws SQLException {
        Vector<String> vector = getSearchStr();
        if (vector.size() < 1) {
            Platform.runLater(() -> mNnOther.showInfo("提示！", "orderId不能为空白！"));
            return;
        }
        mVector = new Vector<>();
        NnAccdbReader access = mManager.getAccedb();
        for (String str : vector) {
            ResultSet result = access.getResultSet("select * from stock_new where orderId = '" + str + "'");
            WeightingInfo info = new WeightingInfo(str);
            if (result.next()) {
                info.isCanDelete(false);
                info = setWeightingInfo(info, result);
                addMessage(info.getDate() + "\t" + info.getOrderId() + "\t" + info.getQuality() + "mg\t" +
                        info.getPackages() + "\t" + info.getCoordinate());
                mVector.add(info);
            } else {
                result = access.getResultSet("select * from stock_old where orderId = '" + str + "'");
                if (result.next()) {
                    info.isCanDelete(true);
                    info = setWeightingInfo(info, result);
                    addMessage(info.getDate() + "\t" + info.getOrderId() + "\t" + info.getCause() + "\t" + info.getQuality());
                    mVector.add(info);
                } else {
                    addMessage(str + "  无记录！");
                }
            }
            addMessage("\n");
        }
        if (mVector.size() > 0) {
            Platform.runLater(() -> bt_submit.setText("导出Excel表"));
            submitFlg = 2;
        }
    }

    private WeightingInfo setWeightingInfo(WeightingInfo info, ResultSet result) throws SQLException {
        info.setDate(result.getString("_date"));
        String quality = result.getString("quality");// TODO 有时间这里优化下
        info.setQuality((quality == null || quality.equals("")) ? 0 : NnOther.getQuality(quality.toCharArray()));
        if (info.canDelete()) {
            info.setCause(result.getString("cause"));
        } else {
            info.setPackages(result.getString("package"));
            info.setCoordinate(result.getString("coordinate"));
        }
        return info;
    }

    private void addMessage(String str) {
        Platform.runLater(() -> tArea_weight.appendText(str));
    }

    private Vector<String> getSearchStr() {
        Vector<String> vector = new Vector<>();
        char[] chars = new char[240];
        char[] strs = ta_weight_search.getText().toCharArray();
        int i = 0;
        for (char c : strs) {
            if (c != '\n' && c != '\t' && c != ' ') {
                chars[i++] = c;
            } else {
                if (i > 0)
                    vector.add(new String(chars, 0, i));
                i = 0;
            }
        }
        if (i > 0) {
            vector.add(new String(chars, 0, i));
        }
        return vector;
    }
}
