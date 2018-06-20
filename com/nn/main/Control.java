package com.nn.main;

import com.nn.data.NnAccdbReader;
import com.nn.data.NnConfiguration;
import com.nn.data.NnExcelReader;
import com.nn.data.NnPolypeptide;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import static com.nn.data.NnPolypeptide.getQuality;

public class Control {
    private NnExcelReader mNew;// 新单（这个是excel）
    private NnAccdbReader mStock;// 库存（下面两个是Access数据库）
    private NnAccdbReader mHistory;// 历史订单

    private NnConfiguration mNnConfiguration;// 配置信息

    public Control(String news) throws IOException, SQLException, SAXException, ParserConfigurationException, ClassNotFoundException {
        nnInit(news);// 初始化
        start();// 开始查找数据
        try {
            mNew.output();//将数据写回excel表格
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("表格被占用，请先关闭文件后尝试！");
        }finally {
            mNew.close();
        }
        //openExcel();
    }

    private void openExcel() {// 打开excel表格
        try {
            Runtime.getRuntime().exec("cmd /c start " + mNew.getUrl());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void start() {
        if (mHistory == null) {
            System.out.println("空指针！！");
            return;
        }
        int newSize = mNew.getRowSize();
        for (int i = 0; i < newSize; ++i) {
            String orderId = mNew.getCellString(i, 10);
            String sequence = mNew.getCellString(i, 12);
            NnPolypeptide nnNewPolypeptide = new NnPolypeptide(orderId, sequence);
            nnNewPolypeptide.setPurity(mNew.getCellString(i, 15));
            nnNewPolypeptide.setQuality(mNew.getCellString(i, 14));
            nnNewPolypeptide.setMw(mNew.getCellString(i, 17));
            if (nnNewPolypeptide.isAvailable()) {// 如果有效
                findHistory(nnNewPolypeptide, i);// 得到所有的历史订单
            }
        }
    }

    // "orderId", "sequence", "purity", "mw"
    // 寻找历史订单，一个序列，会有很多的历史订单，注意它们的修饰和分子量是否相同（我的理解，分子量相同或者相差18，则修饰肯定相同）
    private void findHistory(NnPolypeptide nnNewPolypeptide, int i) {
        try {
            double quality = 0;
            Vector<String> stockInfo = new Vector<>();
            ResultSet resultHistory = mHistory.getResultSet("select * from history where sequence = '" + nnNewPolypeptide.getSequence()+"'");
            while (resultHistory.next()) {
                NnPolypeptide nnHistoryPolypeptide = new NnPolypeptide(resultHistory.getString("orderId"), resultHistory.getString("sequence"));
                nnHistoryPolypeptide.setMw(resultHistory.getDouble("mw"));
                nnHistoryPolypeptide.setPurity(resultHistory.getDouble("purity"));

                int flag = nnHistoryPolypeptide.equalFlg(nnNewPolypeptide);
                if (flag > 0) {
                    quality += (findStock(nnHistoryPolypeptide.getOrderId(), stockInfo) / flag);// 返回获得的库存数量，并且把库存信息保存再stockInfo中
                }

            }
            if (quality > 0) {
                StringBuilder str = new StringBuilder();
                int lens = stockInfo.size();
                for (int is = 0; is < lens; ++is) {
                    str.append(stockInfo.get(is));
                    if (lens > 1 && is < lens - 1) {
                        str.append(" ||  ");
                    }
                }
                System.out.println(str.toString());
                if (quality >= nnNewPolypeptide.getQuality()) {// 有库存
                    writeBack(str.toString(), i, true);
                } else {// 库存不足
                    writeBack(str.toString(), i, false);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将得到的库存信息写回新单excel表格中
     * @param info     库存信息
     * @param i        需要写入的行数（第几行）
     * @param isEnough （库存量是否足够）
     */
    private void writeBack(String info, int i, boolean isEnough) {
        int row = isEnough ? 27 : 28;
        mNew.setCellValue(i, row, info);// TODO 注意，这里还没有写入文件，需要调用output一次性写入文件
    }

    private double findStock(String orderId, Vector<String> stockInfo) throws SQLException {
        ResultSet resultSet = mStock.getResultSet("select * from stock where orderId = '" + orderId + "'");
        double quality = 0;
        while (resultSet.next()) {
            String cause = resultSet.getString("cause");
            String other = resultSet.getString("other");
            if (cause == null ) {
                String date = resultSet.getString("_date");
                if (date == null && (other == null || other.equals("已销毁"))) {
                    continue;
                }
                double q = getQuality(resultSet.getString("quality"));
                if (q > 0) {
                    quality += q;
                    String stock = orderId + " " + date + " " + q + "mg";
                    if (!isHave(stockInfo, stock)) {
                        stockInfo.add(stock);
                    }
                }
            }
        }
        return quality;
    }

    private boolean isHave(Vector<String> stockInfo, String stock) {
        for (String str : stockInfo) {
            if (str.equals(stock)) {
                return true;
            }
        }
        return false;
    }

    // 初始化，将历史订单存入数据库中，以及其他一些初始化工作
    private void nnInit(String news) throws SQLException, ClassNotFoundException, IOException, ParserConfigurationException, SAXException {
        mNnConfiguration = new NnConfiguration("path.xml");
        String str = news == null ? mNnConfiguration.getString("new") : news;
        mNew = new NnExcelReader(news == null ? mNnConfiguration.getString("new") : news);
        //initHistory();
        mHistory = new NnAccdbReader(mNnConfiguration.getString("history"));
        mStock = new NnAccdbReader((mNnConfiguration.getString("stock")));
    }

    /*private void initHistory() throws IOException, SQLException, ClassNotFoundException {
        NnExcelReader history = new NnExcelReader(mNnConfiguration.getString("history"));
        mHistory = new NnAccdbReader("nnns.accdb");
        mHistory.execute("delete from nn");
        //mHistory.createTable("nn", new String[]{"orderId", "sequence", "purity", "mw"});

        int historySize = history.getRowSize();
        for (int i = 0; i < historySize; ++i) {
            String orderId = history.getCellString(i, 15);
            String sequence = history.getCellString(i, 6);
            NnPolypeptide nnPolypeptide = new NnPolypeptide(orderId, sequence);
            nnPolypeptide.setPurity(history.getCellString(i, 3));
            nnPolypeptide.setMw(history.getCellString(i, 9));
            if (nnPolypeptide.isAvailable()) {
                System.out.println(nnPolypeptide.isAvailable());
                String sql = "insert into nn values ('" + nnPolypeptide.getOrderId() + "','" + nnPolypeptide.getSequence() +
                        "','" + nnPolypeptide.getPurity() + "','" + nnPolypeptide.getMw() + "')";
                mHistory.execute(sql);
            }
        }
        history.close();
    }*/
}
