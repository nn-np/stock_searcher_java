package com.nn.main;

import com.nn.data.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import com.nn.layout.WeighingPopController;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class WeightingManager {

    private NnExcelReader mExcel;
    private NnAccdbReader mAccedb;
    private TextArea mMessageBox;
    private NnOther mOther;

    private TextArea mSearchBox;
    private Vector<WeightingInfo> mVector;

    private Vector<WeightingInfo> vSelect;// 库存提交时如果一个orderId有两条库存就添加到这个数组里再处理

    public WeightingManager() {
        mOther = new NnOther();
        // 配置文件
        NnProperties mNnProperties;
        try {
            mNnProperties = new NnProperties("nn.xml");
        } catch (IOException | XMLStreamException e) {
            mOther.showInfo("错误！！", "配置文件读取发生错误！");
            e.printStackTrace();
            return;
        }
        try {
            mAccedb = new NnAccdbReader(mNnProperties.getProperty("history", "polypeptideInfo.accdb"));
        } catch (ClassNotFoundException e) {
            mOther.showInfo("错误！", "数据库驱动发生错误！");
            e.printStackTrace();
        } catch (SQLException e) {
            mOther.showInfo("错误！", "数据库路径错误！");
            e.printStackTrace();
        }
    }

    public void setSearchBox(TextArea mSearchBox) {
        this.mSearchBox = mSearchBox;
    }

    public void setMessageBox(TextArea textField) {
        mMessageBox = textField;
    }

    public void setExcel(String url) {
        try {
            mExcel = new NnExcelReader(url);
        } catch (IOException e) {
            mOther.showInfo("错误！", "表格发生错误，请重试或！");
            e.printStackTrace();
        }
    }

    private void addMessage(String string) {
        Platform.runLater(() -> mMessageBox.appendText(string));
    }


    public void submit() {
        new Thread(() -> {
            toSubmit();
            if (vSelect.size() > 0) {
                addMessage(" ---其他 " + vSelect.size() + " 条");
                toChoose();
            }
        }).start();
    }

    public void search(NnMessageListener m, ObservableList<WeightingInfo> mData) {
        new Thread(() -> {
            try {
                toSearch(m, mData);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void toSearch(NnMessageListener m, ObservableList<WeightingInfo> mData) throws SQLException {
        Vector<String> vector = getSearchStr();
        if (vector.size() < 1) {
            Platform.runLater(() -> mOther.showInfo("提示！", "orderId不能为空白！"));
            return;
        }
        mVector = new Vector<>();
        for (String str : vector) {
            ResultSet result = mAccedb.getResultSet("select * from stock_new left join history on stock_new.orderId = history.orderId and orderId = '" + str + "'");
            int count = 0;
            while (result.next()) {
                WeightingInfo info = new WeightingInfo(str);
                info.isCanDelete(false);
                setWeightingInfo(info, result);
                String message = (info.getDate() + " \t" + info.getOrderId() + " \t" + ("" + info.getA_purity()).replaceAll("\\.0", "") + "% \t" + info.getA_mw() + " \t" + info.getQuality() + "mg \t" +
                        info.getPackages() + " \t" + info.getCoordinate());
                addMessage(message);
                mVector.add(info);
                mData.add(info);
                ++count;
            }
            result = mAccedb.getResultSet("select * from stock_old left join history on stock_old.orderId = history.orderId and orderId = '" + str + "'");
            while (result.next()) {
                WeightingInfo info = new WeightingInfo(str);
                info.isCanDelete(true);
                setWeightingInfo(info, result);
                addMessage(info.getDate() + " \t" + info.getOrderId() + " \t" + info.getCause() + " \t" + info.getQuality() + "mg");
                mVector.add(info);
                mData.add(info);
                ++count;
            }
            if (count < 0) {
                WeightingInfo info = new WeightingInfo(str);
                addMessage(str + "  无记录！");
                info.setCause("无记录！");
                mData.add(info);
            }
        }
        addMessage("\n");
        if (mVector.size() > 0) {
            m.send();
        }

    }

    private Vector<String> getSearchStr() {
        Vector<String> vector = new Vector<>();
        char[] chars = new char[240];
        char[] strs = mSearchBox.getText().toCharArray();
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

    public static void setWeightingInfo(WeightingInfo info, ResultSet result) throws SQLException {
        info.setDate(result.getString("_date"));
        String quality = result.getString("quality");// TODO 有时间这里优化下
        info.setQuality((quality == null || quality.equals("")) ? 0 : NnOther.getQuality(quality.toCharArray()));
        if (info.canDelete()) {
            info.setCause(result.getString("cause"));
        } else {
            info.setPackages(result.getString("package"));
            info.setCoordinate(result.getString("coordinate"));

        }
        String string = result.getString("a_purity");
        info.setA_purity(Double.parseDouble((string == null || string.equals("")) ? "0" : string));
        string = result.getString("a_mw");
        info.setA_mw(Double.parseDouble((string == null || string.equals("")) ? "0" : string));
        info.setGroup(result.getString("group"));
        info.setComments(result.getString("comments"));
    }


    private int add, delete, update;
    private void toSubmit() {
        add = 0;
        delete = 0;
        update = 0;
        vSelect = new Vector<>();
        int rowSize = mExcel.getRowSize();
        for (int i = 1; i < rowSize; ++i) {
            WeightingInfo info = getInfoFromExcel(i);
            if (!info.isAvailable()) {// do something or not
                return;
            }
            addMessage(info.getOrderId() + "\t");
            if (info.canDelete()) {
                _toDelete(info);
            } else {
                _toUpdate(info);
            }
        }
        addMessage("\n总计：---添加 " + add + " 条 ---移除 " + delete + " 条 ---修改 " + update + " 条");
    }

    private void _toUpdate(WeightingInfo info) {
        String str = "";
        try {
            insertStock(info);
            str += info.getPackages() + "\t" + info.getCoordinate() + "\t添加成功";
            ++add;
        } catch (SQLException e) {
            try {
                update(info);
                str += "已修改";
            } catch (SQLException e1) {
                str += "更新失败";
            }
            ++update;
        }
        addMessage(info.getQuality() + "mg\t" + info.getA_purity() + "\t" + info.getA_mw() + "\t" + info.getCoordinate() + "\t" + str + "\n");
    }

    private void _toDelete(WeightingInfo info) {
        String str = "";
        try {
            if (updateStock(info)) {
                str += "已移除";
                ++delete;
            } else {
                str += "多条库存，另处理";
            }
        } catch (SQLException e) {
            str += "移除失败";
        }
        addMessage(info.getCause() + "\t" + str + "\n");
    }

    private void update(WeightingInfo info) throws SQLException {
        String date = new SimpleDateFormat("yyyy.M.d").format(new Date());
        mAccedb.execute("update stock_new set [_date] = '" + date + "',[group] = '" + info.getGroup() + "',quality = '" + info.getQuality() + "',a_purity = " +
                info.getA_purity() + ",a_mw = " + info.getA_mw() + ",package = '" + info.getPackages() + "',coordinate='" + info.getCoordinate() + "' where orderId = '" + info.getOrderId() + "'");
    }


    private void insertStock(WeightingInfo info) throws SQLException {
        mAccedb.execute("insert into stock_new values('" + info.getDate() + "','" + info.getGroup() + "','" + info.getOrderId() + "','" + info.getQuality() + "'," + info.getA_purity() + ","
                + info.getA_mw() + ",'" + info.getPackages() + "','" + info.getCoordinate() + "')");
    }

    private boolean updateStock(WeightingInfo info) throws SQLException {
        ResultSet res = mAccedb.getResultSet("select count(1) nn from stock_new where orderId = '" + info.getOrderId() + "'");
        res.next();
        if (res.getInt("nn") > 1) {
            vSelect.add(info);
            return false;
        }
        mAccedb.execute("insert into stock_old ( _date, orderId,quality,[group],a_purity,a_mw ) select stock_new.[_date],stock_new.orderId,stock_new.quality,stock_new.group,stock_new.a_purity," +
                "stock_new.a_mw from stock_new where orderId = '" + info.getOrderId() + "'");
        mAccedb.execute("update stock_old set cause = '" + info.getCause() + "' where orderId = '" + info.getOrderId() + "'");
        mAccedb.execute("delete from stock_new where orderId = '" + info.getOrderId() + "'");
        return true;
    }

    private void toChoose() {
        Platform.runLater(() -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nn/layout/weighing_popwindow.fxml"));
            Parent root = null;
            try {
                root = fxmlLoader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            WeighingPopController controller = fxmlLoader.getController();
            controller.setVector(vSelect);
            controller.setNnAccdb(mAccedb);

            Scene scene = new Scene(root, 430, 230);

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("选择要移除的库存");
            stage.getIcons().add(new Image("语文.png"));

            controller.setStage(stage);

            stage.show();
        });
    }

    private WeightingInfo getInfoFromExcel(int i) {
        String orderId = mExcel.getCellString(i, 0).replaceAll(" ", "").replaceAll("\\.0+", "");
        WeightingInfo info = new WeightingInfo(orderId);
        if (!info.isAvailable()) return info;
        String cause = mExcel.getCellString(i, 6).replaceAll(" ", "");
        info.setCause(cause);
        if (info.canDelete()) {
            return info;
        }
        info.setDate(new SimpleDateFormat("yyyy.M.d").format(new Date()));
        info.setQuality(NnOther.getQuality(mExcel.getCellString(i, 1).toCharArray()));
        info.setA_purity(NnOther.getMaxValue(mExcel.getCellString(i, 2).toCharArray()));
        info.setA_mw(Double.parseDouble(mExcel.getCellString(i, 3)));
        info.setCoordinate(mExcel.getCellString(i, 4));
        info.setGroup(mExcel.getCellString(i, 5));

        System.out.println(info.getOrderId() + "\t" + info.getCause() + "\t" + info.getA_purity() + "\t" + info.getA_mw() + "\t" + info.getQuality() + "\t" +
                info.getCoordinate() + "\t" + info.getPackages() + "\t" + info.getGroup());

        return info;
    }

    public void outPut(NnMessageListener m) {
        toOutPut(m);
    }

    private void toOutPut(NnMessageListener m) {
        NnExcelReader reader = new NnExcelReader();
        reader.createSheet(new SimpleDateFormat("yyyy.M.d").format(new Date()) + "库存查找");
        reader.setUrl(System.getProperty("java.io.tmpdir") + "weightingInfo.xlsx");
        reader.setCellValue(0, 0, "日期").setCellValue(0, 1, "order ID").setCellValue(0, 2, "质量").setCellValue(0, 3, "实际纯度")
                .setCellValue(0, 4, "实际分子量").setCellValue(0, 5, "坐标").setCellValue(0, 6, "袋")
                .setCellValue(0, 9, "备注").setCellValue(0, 8, "原因").setCellValue(0, 7, "组别");
        int i = 1;
        for (WeightingInfo info : mVector) {
            reader.setCellValue(i, 0, info.getDate());
            reader.setCellValue(i, 1, info.getOrderId());
            reader.setCellValue(i, 2, info.getQuality() + "mg");
            reader.setCellValue(i, 3, "" + info.getA_purity());
            reader.setCellValue(i, 4, "" + info.getA_mw());
            reader.setCellValue(i, 9, "" + info.getComments());
            reader.setCellValue(i, 8, "" + info.getCause());
            reader.setCellValue(i, 7, "" + info.getGroup());
            if (!info.canDelete()) {
                reader.setCellValue(i, 5, "" + info.getCoordinate());
                reader.setCellValue(i, 6, "" + info.getPackages());
            }
            ++i;
        }
        try {
            reader.output();
            reader.close();
            m.send();
        } catch (IOException e) {
            Platform.runLater(() -> mOther.showInfo("提示！", "文件已被其他应用占用，请先关闭该应用！"));
            e.printStackTrace();
        }
        try {
            Runtime.getRuntime().exec("cmd /c start " + reader.getUrl());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
