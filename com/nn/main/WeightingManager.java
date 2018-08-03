package com.nn.main;

import com.nn.data.*;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WeightingManager {

    private NnExcelReader mExcel;
    private NnAccdbReader mAccedb;
    private TextArea mMessageBox;
    private NnOther mOther;

    public WeightingManager() {
        mOther = new NnOther();
        NnProperties mProperties;
        try {
            mProperties = new NnProperties("nn.xml");
        } catch (IOException | XMLStreamException e) {
            mOther.showInfo("提示！", "配置文件错误！");
            e.printStackTrace();
            return;
        }
        try {
            mAccedb = new NnAccdbReader(mProperties.getProperty("history", "polypeptideInfo.accdb"));
        } catch (ClassNotFoundException e) {
            mOther.showInfo("错误！", "数据库驱动发生错误！");
            e.printStackTrace();
        } catch (SQLException e) {
            mOther.showInfo("错误！","数据库路径错误！");
            e.printStackTrace();
        }
    }

    public NnAccdbReader getAccedb() {
        return mAccedb;
    }

    public void setMessageBox(TextArea textField) {
        mMessageBox = textField;
    }

    public void setExcel(String url) {
        try {
            mExcel = new NnExcelReader(url);
        } catch (IOException e) {
            mOther.showInfo("错误！","表格发生错误，请重试或！");
            e.printStackTrace();
        }
    }

    private void addMessage(String string) {
        Platform.runLater(() -> mMessageBox.appendText(string));
    }


    public void submit(){
        toSubmit();
    }

    private void toSubmit() {
        new Thread(()->{
            int rowSize = mExcel.getRowSize();
            int add = 0, delete = 0, error = 0;
            for (int i = 1; i < rowSize; ++i) {
                WeightingInfo info = getInfoFromExcel(i);
                if (!info.isAvailable()) {
                    // do something
                    return;
                }
                addMessage(info.getOrderId() + "\t");
                if (info.canDelete()) {
                    ++delete;
                    String str = "";
                    try {
                        updateStock(info);
                        str += "已移除";
                    } catch (SQLException e) {
                        str += "移除失败";
                        ++error;
                        e.printStackTrace();
                    }
                    addMessage(info.getCause() + "\t" + str);
                } else {
                    String str = "";
                    try {
                        insertStock(info);
                        str += info.getPackages() + "\t" + info.getCoordinate() + "\t添加成功";
                        ++add;
                    } catch (SQLException e) {
                        str += "添加失败（已存在）";
                        ++error;
                        e.printStackTrace();
                    }
                    addMessage(info.getQuality() + "mg\t" + str);
                }
                addMessage("\n");
            }
            addMessage("\n总计：---添加 " + add + " 条 ---移除 " + delete + " 条 ---出错 " + error + " 条");
        }).start();
    }


    private void insertStock(WeightingInfo info) throws SQLException {
        mAccedb.execute("insert into stock_new values('" + info.getDate() + "','" + info.getGroup() + "','" + info.getOrderId() + "','" + info.getQuality() + "','" + info.getPackages() + "','" + info.getCoordinate() + "')");
    }

    private void updateStock(WeightingInfo info) throws SQLException {
        mAccedb.execute("insert into stock_old ( _date, orderId,quality,[group] ) select stock_new.[_date],stock_new.orderId,stock_new.quality,stock_new.group from stock_new where orderId = '" + info.getOrderId() + "'");
        mAccedb.execute("update stock_old set cause = '" + info.getCause() + "' where orderId = '" + info.getOrderId() + "'");
        mAccedb.execute("delete from stock_new where orderId = '" + info.getOrderId() + "'");
    }



    private WeightingInfo getInfoFromExcel(int i) {
        String orderId = mExcel.getCellString(i, 0).replaceAll(" ","");
        WeightingInfo info = new WeightingInfo(orderId);
        String cause = mExcel.getCellString(i, 1).replaceAll(" ","");
        info.setCause(cause);
        if (info.canDelete()) {
            return info;
        }
        info.setDate(new SimpleDateFormat("yyyy.M.d").format(new Date()));
        info.setQuality(NnOther.getQuality(mExcel.getCellString(i,2).toCharArray()));
        info.setCoordinate(mExcel.getCellString(i, 3));
        info.setPackages(mExcel.getCellString(i, 4));
        info.setGroup(mExcel.getCellString(i, 5));

        System.out.println(info.getOrderId() + "\t" + info.getCause() + "\t"  + info.getQuality()+ "\t" + info.getCoordinate() + "\t" + info.getPackages() + "\t" + info.getGroup());

        return info;
    }
}
