package com.nn.layout;

import com.nn.data.NnAccdbReader;
import com.nn.data.NnOther;
import com.nn.data.WeightingInfo;
import com.nn.main.WeightingManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;

public class WeighingPopController {
    private ObservableList<WeightingInfo> mData;

    private Stage mStage;
    private NnAccdbReader mAccdb;
    private NnOther mNnOther;

    public TableView<WeightingInfo> we_pop_tv;

    public void setStage(Stage mStage) {
        this.mStage = mStage;
    }

    public void setNnAccdb(NnAccdbReader mAccess) {
        this.mAccdb = mAccess;
    }

    public void setVector(Vector<WeightingInfo> vector) {
        mData = FXCollections.observableArrayList();
        mNnOther = new NnOther();
        new Thread(() ->
                Platform.runLater(() -> {
                    for (WeightingInfo info : vector) {
                        try {
                            ResultSet res = mAccdb.getResultSet("select * from stock_new left join history on stock_new.orderId = history.orderId and orderId = '" + info.getOrderId() + "'");
                            while (res.next()) {
                                WeightingInfo info1 = new WeightingInfo(info.getOrderId());
                                WeightingManager.setWeightingInfo(info1, res);
                                mData.add(info1);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    WeighingController.initTableView(we_pop_tv, mData);
                    we_pop_tv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                })
        ).start();
    }

    public void bt_ok(ActionEvent event) {
        ObservableList<Integer> infos = we_pop_tv.getSelectionModel().getSelectedIndices();
        Iterator<Integer> it = infos.iterator();
        while (it.hasNext()) {
            WeightingInfo info = mData.get(it.next());
            try {
                if (info.getCoordinate() != null && !info.getCoordinate().equals("")) {
                    updateStock(info, "coordinate = '" + info.getCoordinate() + "'");
                } else {
                    updateStock(info, "_date = '" + info.getDate() + "'");
                }
            } catch (SQLException e) {
                mNnOther.showInfo("提示！", "发生未知错误，数据未提交！");
                e.printStackTrace();
            }
        }
        bt_cancel(null);
    }

    private void updateStock(WeightingInfo info, String conditions) throws SQLException {
        mAccdb.execute("insert into stock_old ( _date, orderId,quality,[group],a_purity,a_mw ) select stock_new.[_date],stock_new.orderId,stock_new.quality,stock_new.group,stock_new.a_purity," +
                "stock_new.a_mw from stock_new where orderId = '" + info.getOrderId() + "' and " + conditions);
        mAccdb.execute("update stock_old set cause = '" + info.getCause() + "' where orderId = '" + info.getOrderId() + "'");
        mAccdb.execute("delete from stock_new where orderId = '" + info.getOrderId() + "' and " + conditions);
    }

    public void bt_cancel(ActionEvent event) {
        mStage.close();
    }

}
