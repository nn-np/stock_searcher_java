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

    private String mDate;

    private TextArea mSearchBox;
    private Vector<WeightingInfo> mVector;

    private Vector<WeightingInfo> vSelect;// 库存提交时如果一个orderId有两条库存就添加到这个数组里再处理

    public WeightingManager() {
        mOther = new NnOther();
        mDate = new SimpleDateFormat("yyyy.M.d").format(new Date());
        // 配置文件
        NnProperties mNnProperties;
        try {
            mNnProperties = new NnProperties("nn.xml");
        } catch (IOException | XMLStreamException e) {
            mOther.showInfo("错误！", "配置文件读取错误！");
            e.printStackTrace();
            return;
        }
        try {
            mAccedb = new NnAccdbReader(mNnProperties.getProperty("history", "polypeptideInfo.accdb"));
        } catch (ClassNotFoundException e) {
            mOther.showInfo("错误！", "数据库驱动错误！");
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
            mOther.showInfo("错误！", "表格错误，请重试！");
            e.printStackTrace();
        }
    }

    private void addMessage(String string) {
        Platform.runLater(() -> mMessageBox.appendText(string));
    }


    public void submit(boolean isUpdate) {
        new Thread(() -> {
            toSubmit(isUpdate);
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
                setWeightingInfo(info, result,4);
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
                setWeightingInfo(info, result,2);
                addMessage(info.getDate() + " \t" + info.getOrderId() + " \t" + info.getCause() + " \t" + info.getQuality() + "mg");
                mVector.add(info);
                mData.add(info);
                ++count;
            }
            result = mAccedb.getResultSet("select * from stock_temporary left join history on stock_temporary.orderId = history.orderId and orderId = '" + str + "'");
            if (result.next()) {
                WeightingInfo info = new WeightingInfo(str);
                info.isCanDelete(false);
                setWeightingInfo(info, result, 3);
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

    public static void setWeightingInfo(WeightingInfo info, ResultSet result,int flg) throws SQLException {
        info.setDate(result.getString("_date"));
        String quality = result.getString("quality");// TODO 有时间这里优化下
        info.setQuality((quality == null || quality.equals("")) ? 0 : NnOther.getQuality(quality.toCharArray()));
        if (info.canDelete()) {
            info.setCause(result.getString("cause"));
        } else if (flg == 1 || flg == 4) {
            info.setCoordinate(result.getString("coordinate"));
        }
        if (flg == 2 || flg == 4 || flg == 3) {
            info.setComments(result.getString("comments"));
        }
        if (flg == 0 || flg == 3) {
            info.setPackages(result.getString("package"));
        }
        String string = result.getString("a_purity");
        double p = Double.parseDouble((string == null || string.equals("")) ? "0" : string);
        p *= p < 1 ? 100 : 1;
        info.setA_purity(p);
        string = result.getString("a_mw");
        info.setA_mw(Double.parseDouble((string == null || string.equals("")) ? "0" : string));
        info.setGroup(result.getString("group"));
    }

    private int add, delete, update;

    private void toSubmit(boolean isUpdate) {// 有无坐标
        add = 0;
        delete = 0;
        update = 0;
        vSelect = new Vector<>();
        int rowSize = mExcel.getRowSize();
        for (int i = 1; i < rowSize; ++i) {
            WeightingInfo info = getInfoFromExcel(i);
            if (!info.isAvailable()) {// do something or not
                continue;
            }
            if (info.canDelete()) {
                _toDelete(info);
            } else {
                _toUpdate(info, isUpdate);
            }
        }
        addMessage("\n总计：---添加 " + add + " 条 ---移除 " + delete + " 条 ---修改 " + update + " 条");
    }

    private void _toUpdate(WeightingInfo info, boolean isUpdate) {
        String str = info.getOrderId() + "\t";
        if (isUpdate) {// 如果需要更新坐标，一种是已经有坐标，更换坐标，一种是没有坐标，添加坐标（直接向sock_new 添加，删除stock_temporary里相关数据）
            try {
                str = updateCoordinate(info);
            } catch (SQLException e) {
                str += info.getCoordinate() + "坐标已存在或其他原因，更新失败";
                e.printStackTrace();
            }
        } else {
            try {
                if (insertStock(info) > 0) {
                    str += info.getPackages() + "\t" + info.getCoordinate() + "\t添加成功";
                    ++add;
                } else {
                    str += "更新或添加失败";
                }
            } catch (SQLException e) {
                str += "更新或添加失败";
                e.printStackTrace();
            }
        }
        addMessage(info.getQuality() + "mg\t" + info.getA_purity() + "\t" + info.getA_mw() + "\t" + info.getCoordinate() + "\t" + str + "\n");
    }

    private int insertStock(WeightingInfo info) throws SQLException {
        if (info.getCoordinate() != null && !info.getCoordinate().equals("")) {// 如果有坐标信息，添加到stock_new
            return insertStockNew(info);
        } else {
            return insertStockTemporary(info);
        }
    }

    // 更新坐标
    private String updateCoordinate(WeightingInfo info) throws SQLException {
        String str = "";
        if (info.getOther() != null && !info.getOther().equals("")) {// 如果库存信息含有旧坐标
            ResultSet res = mAccedb.getResultSet("select * from stock_new where coordinate = '" + info.getOther() + "'");
            if (res.next()) {
                WeightingInfo l_info = new WeightingInfo(res.getString("orderId"));// 这里需要从数据库里读取数据，不能用excel里的，因为不全
                setWeightingInfo(l_info, res,1);
                l_info.setCoordinate(info.getCoordinate());
                if (insertStockNew(l_info) > 0) {
                    if (_deleteFromStockNew(info.getOther()) > 0) {
                        str += info.getOther() + " -> " + info.getCoordinate() + " 成功";
                    }
                } else {
                    str += info.getOther() + "更新失败";
                }
            } else {
                str += "坐标 " + info.getOther() + " 不存在";
            }
        } else {// 如果没有旧坐标
            /*if (info.getCoordinate() == null || info.getCoordinate().equals("")) {
                if (info.isAvailable()) {
                    insertStockTemporary(info);
                }
                str += "已经添加";
                return str;
            }*/
            ResultSet res = mAccedb.getResultSet("select * from stock_temporary where orderId = '" + info.getOrderId() + "'");
            if (res.next()) {// 如果有临时的库存
                WeightingInfo l_info = new WeightingInfo(res.getString("orderId"));
                l_info.setCoordinate(info.getCoordinate());
                setWeightingInfo(l_info, res,0);
                if (insertStockNew(l_info) > 0) {
                    mAccedb.execute("delete from stock_temporary where orderId = '" + l_info.getOrderId() + "'");
                    str += "已添加 " + info.getOrderId() + "  " + info.getCoordinate();
                }
            } else {// 如果没有，直接添加
                if (insertStockNew(info) > 0) {
                    str += "已添加 " + info.getOrderId() + "  " + info.getCoordinate();
                }
            }
        }
        return str;
    }

    private int insertStockTemporary(WeightingInfo info) throws SQLException {
        return mAccedb.executeUpdate("insert into stock_temporary values('" + mDate + "','" + info.getGroup() + "','" + info.getOrderId() + "','" + info.getQuality() + "','" + info.getA_purity() + "','"
                + info.getA_mw() + "','" + info.getPackages() + "')");
    }

    private void _toDelete(WeightingInfo info) {
        String str = info.getOrderId() + "\t";
        try {
            int flg = deleteFormStock(info);
            if (flg == 1) {
                str += "已移除";
                ++delete;
            } else if (flg > 1) {
                str += "共 " + flg + " 条库存，另处理";
            } else if (flg == 0) {
                str += "无记录，移除失败";
            }
        } catch (SQLException e) {
            str += "未知原因，移除失败";
        }
        addMessage(info.getCause() + "\t" + str + "\n");
    }

    /*private void update(WeightingInfo info) throws SQLException {
        String date = new SimpleDateFormat("yyyy.M.d").format(new Date());
        mAccedb.execute("update stock_new set [_date] = '" + date + "',[group] = '" + info.getGroup() + "',quality = '" + info.getQuality() + "',a_purity = " +
                info.getA_purity() + ",a_mw = " + info.getA_mw() + ",package = '" + info.getPackages() + "',coordinate ='" + info.getCoordinate() + "' where orderId = '" + info.getOrderId() + "'");
    }*/

    // 向数据库中添加数据，这个要不写一个类？我看行

    // 向stock_new中添加数据
    private int insertStockNew(WeightingInfo info) throws SQLException {
        int i = mAccedb.executeUpdate("insert into stock_new values('" + info.getDate() + "','" + info.getGroup() + "','" + info.getOrderId() + "','" + info.getQuality() + "','" + info.getA_purity() + "','"
                + info.getA_mw() + "','" + info.getCoordinate() + "')");
        if (i > 0) {// TODO 对坐标数据库进行维护

        }
        return i;
    }

    // 对stock_new的操作通过主键 coordinate 进行,对临时表 stock_temporary，通过orderId，这样每次要移除也要输入坐标
    private int deleteFormStock(WeightingInfo info) throws SQLException {
        if (info.getCoordinate().equals("") || info.getCoordinate() == null) {// 如果没有坐标，则在临时表里找，
            return deleteFromStockTemporary(info);
        }
        return deleteFromStockNew(info);
    }

    private int deleteFromStockNew(WeightingInfo info) throws SQLException {// 如果有原因就是移除这条，没有就是在更新坐标
        ResultSet res = mAccedb.getResultSet("select * from stock_new where coordinate = '" + info.getCoordinate() + "'");
        if (res.next()) {
            if (updateStockOld(res, info.getCause()) > 0) {
                return _deleteFromStockNew(info.getCoordinate());
            }
        }
        return 0;
    }

    // 向sock_old 添加数据
    private int updateStockOld(ResultSet resSet, String cause) throws SQLException {
        return mAccedb.executeUpdate("insert into stock_old values('" + resSet.getString("_date") + "','" + mDate + "','" + resSet.getString("group") + "','" + resSet.getString("orderId") +
                "','" + cause + "','" + resSet.getString("quality") + "','" + resSet.getString("a_purity") + "','" + resSet.getString("a_mw") + "')");
    }

    private int _deleteFromStockNew(String coordinate) throws SQLException {
        int i = mAccedb.executeUpdate("delete from stock_new where coordinate = '" + coordinate + "'");
        if (i > 0) {// TODO 注意，这里还要对坐标数据库进行维护

        }
        return i;
    }

    private int deleteFromStockTemporary(WeightingInfo info) throws SQLException {
        // 没有坐标的情况是这个数据在临时表里，也有可能是用户忘记添加坐标（着一点要避免，这是错误操作）
        ResultSet resSet = mAccedb.getResultSet("select * from stock_temporary where orderId = '" + info.getOrderId() + "'");
        if (resSet.next()) {
            /*int count = resSet.getInt("count");
            if (count > 1) {// 如果有多条
                return mAccedb.executeUpdate("update stock_temporary set count = " + (--count) + " where orderId = '" + info.getOrderId() + "'");
            } else if (count == 1) {// 如果只有一条*/
            updateStockOld(resSet, info.getCause());
             /*String comment = resSet.getString("comment");
               if (comment != null && !comment.equals("")) {// TOD 删除其他信息，搜索的时候如果库存大于1，也要注意
                }*/
            return mAccedb.executeUpdate("delete from stock_temporary where orderId = '" + info.getOrderId() + "'");
            //}
        }
        return 0;// 如果临时表里没有，就去有坐标的表里找？还是不要，不然会乱套
    }

    private void toChoose() {
        Platform.runLater(() -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nn/layout/weighing_popwindow.fxml"));
            try {
                Parent root = fxmlLoader.load();
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private WeightingInfo getInfoFromExcel(int i) {
        String orderId = mExcel.getCellString(i, 0).replaceAll(" ", "").replaceAll("\\.0+", "");
        WeightingInfo info = new WeightingInfo(orderId);
        if (!info.isAvailable()) return info;// 无效
        String cause = mExcel.getCellString(i, 6).replaceAll(" ", "");
        info.setCause(cause);
        info.setCoordinate(mExcel.getCellString(i, 4));
        if (info.canDelete()) { // 有原因，从表中删除，注意区分
            return info;
        }
        info.setDate(mDate);
        info.setQuality(NnOther.getQuality(mExcel.getCellString(i, 1).toCharArray()));
        info.setA_purity(NnOther.getMaxValue(mExcel.getCellString(i, 2).toCharArray()));
        info.setA_mw(Double.parseDouble(mExcel.getCellString(i, 3)));
        info.setGroup(mExcel.getCellString(i, 5));
        info.setOther(mExcel.getCellString(i, 7));

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
