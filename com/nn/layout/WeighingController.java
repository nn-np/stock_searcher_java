package com.nn.layout;

import com.nn.data.*;
import com.nn.main.WeightingManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

public class WeighingController {
    private NnOther mNnOther;
    private NnProperties mNnProperties;// 配置文件
    private WeightingManager mManager;// 数据提交控制类
    private String stockUrl = null;// 拖动的excel表格url
    private int submitFlg = 0;// 0打开文件  1提交数据

    private ObservableList<WeightingInfo> mData;

    public HBox hb_bottom;
    public TextArea ta_weight_search;
    public TextArea tArea_weight;
    public AnchorPane root;
    public Button bt_submit;
    public TableView<WeightingInfo> w_tv;
    private CheckBox checkBox;

    public WeighingController() {
        mNnOther = new NnOther();
        mData = FXCollections.observableArrayList();
        try {
            mNnProperties = new NnProperties("nn.xml");
        } catch (IOException | XMLStreamException e) {
            mNnOther.showInfo("错误！！", "配置文件读取发生错误！");
            e.printStackTrace();
        }
        mManager = new WeightingManager();
        Platform.runLater(() -> {
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

            initTableView(w_tv, mData);
            //ta_weight_search.setFocusTraversable(false);
            tArea_weight.setEditable(false);
            mManager.setMessageBox(tArea_weight);
            mManager.setSearchBox(ta_weight_search);
        });
    }

    public static void initTableView(TableView<WeightingInfo> w_tv,ObservableList<WeightingInfo> data) {
        w_tv.getVisibleLeafColumn(0).setCellValueFactory(new PropertyValueFactory<>("date"));
        w_tv.getVisibleLeafColumn(1).setCellValueFactory(new PropertyValueFactory<>("orderId"));
        w_tv.getVisibleLeafColumn(2).setCellValueFactory(new PropertyValueFactory<>("quality"));
        w_tv.getVisibleLeafColumn(3).setCellValueFactory(new PropertyValueFactory<>("a_purity"));
        w_tv.getVisibleLeafColumn(4).setCellValueFactory(new PropertyValueFactory<>("a_mw"));
        w_tv.getVisibleLeafColumn(5).setCellValueFactory(new PropertyValueFactory<>("coordinate"));
        w_tv.getVisibleLeafColumn(6).setCellValueFactory(new PropertyValueFactory<>("packages"));
        w_tv.getVisibleLeafColumn(7).setCellValueFactory(new PropertyValueFactory<>("cause"));
        w_tv.getVisibleLeafColumn(8).setCellValueFactory(new PropertyValueFactory<>("comments"));
        w_tv.setItems(data);
    }

    // 准备提交
    private void toSubmit() {
        bt_submit.setText("提交");
        mManager.setExcel(stockUrl);
        tArea_weight.setVisible(true);
        w_tv.setVisible(false);
        tArea_weight.setText(stockUrl + "\n");
        checkBox = new CheckBox("更新坐标");
        checkBox.setStyle("-fx-text-fill: white");
        hb_bottom.getChildren().add(0, checkBox);
        submitFlg = 1;
    }

    public void submit(ActionEvent event) {
        if (submitFlg == 0) {// 选择表格
            toSelectTable();
        } else if (submitFlg == 1) {// 提交信息
            mManager.submit(checkBox.isSelected());
            toBegin();
        } else if (submitFlg == 2) {// 导出库存信息
            outPut();
        }
    }

    private void toBegin() {
        bt_submit.setText("选择");
        hb_bottom.getChildren().remove(0);
        submitFlg = 0;
    }

    private void outPut() {
        new Thread(()->mManager.outPut(()->{
            Platform.runLater(() -> bt_submit.setText("选择"));
            submitFlg = 0;
        })).start();
    }

    public void search(ActionEvent event) {
        //tArea_weight.clear();
        mData.clear();
        tArea_weight.setVisible(false);
        w_tv.setVisible(true);
        mManager.search(() -> {
            Platform.runLater(() -> bt_submit.setText("导出Excel表"));
            submitFlg = 2;
        }, mData);
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
}
