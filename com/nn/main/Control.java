package com.nn.main;

import com.nn.data.NnAccdbReader;
import com.nn.data.NnConfiguration;
import com.nn.data.NnExcelReader;
import com.nn.data.NnPolypeptide;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import static com.nn.data.NnPolypeptide.getQuality;

public class Control {
    private boolean isContinue = true;
    private NnExcelReader mNew;// 新单（这个是excel）
    private NnAccdbReader mStock;// 库存（下面两个是Access数据库）
    private NnAccdbReader mHistory;// 历史订单

    private NnListener mNnListener;

    private NnConfiguration mNnConfiguration;// 配置信息

    public Control(String news, NnListener nnListener) {
        if (nnListener != null) {
            mNnListener = nnListener;
        }
        if (news == null || news.equals("")) {
            mNnListener.errorInfo("请先选择新单表格！");
            //mNnListener.complete();
            return;
        }
        new Thread(() -> {
            try {
                nnInit(news);// 初始化
                start();// 开始查找数据
                nnEnd();
            } catch (SQLException | ClassNotFoundException | ParserConfigurationException | IOException | SAXException e) {
                if (nnListener != null) {
                    nnListener.errorInfo("未知错误！");
                }
                e.printStackTrace();
            }
        }).start();
    }

    private void nnEnd(){
        mNnListener.complete();
        outPut();
    }

    private void outPut(){
        try {
            mNew.output();//将数据写回excel表格
        } catch (IOException e) {
            e.printStackTrace();
            mNnListener.errorInfo("表格被占用，请先关闭表格再尝试！");
            System.out.println("表格被占用，请先关闭文件后尝试！");
        } finally {
            try {
                mNew.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        isContinue = false;
    }

    private void start() {
        if (mHistory == null) {
            System.out.println("空指针！！");
            return;
        }
        int newSize = mNew.getRowSize();
        for (int i = 0; i < newSize; ++i) {

            if (!isContinue) return;// 如果用户取消查找库存，就结束

            String orderId = mNew.getCellString(i, 10);
            String sequence = mNew.getCellString(i, 12);
            NnPolypeptide nnNewPolypeptide = new NnPolypeptide(orderId, sequence);
            nnNewPolypeptide.setPurity(mNew.getCellString(i, 15));
            nnNewPolypeptide.setQuality(mNew.getCellString(i, 14));
            nnNewPolypeptide.setMw(mNew.getCellString(i, 17));
            nnNewPolypeptide.setModification(mNew.getCellString(i, 16));
            if (nnNewPolypeptide.isAvailable()) {// 如果有效
                findHistory(nnNewPolypeptide, i);// 得到所有的历史订单
            }
            mNnListener.progress((double) i / newSize);// 进度监听者
        }
    }

    // "orderId", "sequence", "purity", "mw"
    // 寻找历史订单，一个序列，会有很多的历史订单，注意它们的修饰和分子量是否相同（我的理解，分子量相同或者相差18，则修饰肯定相同）
    private void findHistory(NnPolypeptide nnNewPolypeptide, int i) {
        try {
            double quality = 0;
            Vector<String> stockInfo = new Vector<>();
            ResultSet resultHistory = mHistory.getResultSet("select * from history where sequence = '" + nnNewPolypeptide.getSequence() + "'");

            boolean isHave = false;

            while (resultHistory.next()) {
                NnPolypeptide nnHistoryPolypeptide = new NnPolypeptide(resultHistory.getString("orderId"), resultHistory.getString("sequence"));
                nnHistoryPolypeptide.setMw(resultHistory.getString("mw"));
                nnHistoryPolypeptide.setPurity(resultHistory.getString("purity"));
                nnHistoryPolypeptide.setModification(resultHistory.getString("modification"));

                // 判断历史订单里是否存在这条新单数据，如果没有就添加
                if (nnNewPolypeptide.getOrderId().equals(nnHistoryPolypeptide.getOrderId())) {
                    isHave = true;
                }

                int flag = nnHistoryPolypeptide.equalFlg(nnNewPolypeptide);
                if (flag > 0) {
                    quality += (findStock(nnHistoryPolypeptide.getOrderId(), stockInfo) / flag);// 返回获得的库存数量，并且把库存信息保存再stockInfo中
                }

            }

            if (!isHave) {
                mHistory.execute("insert into history values ('" + nnNewPolypeptide.getOrderId() + "','" +
                        nnNewPolypeptide.getSequence() + "','" + nnNewPolypeptide.getPurity() + "','" + nnNewPolypeptide.getModification()
                        + "','" + nnNewPolypeptide.getMw() + "')");
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
                    writeBack(str.toString(), i, 27);
                } else {// 库存不足
                    writeBack(str.toString(), i, 28);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将得到的库存信息写回新单excel表格中
     * @param info 需要写入的信息
     * @param x 需要写入的位置（行）
     * @param y 需要写入的位置（列）
     */
    private void writeBack(String info, int x, int y) {
        mNew.setCellValue(x, y, info);// TODO 注意，这里还没有写入文件，需要调用output一次性写入文件
    }

    private double findStock(String orderId, Vector<String> stockInfo) throws SQLException {
        ResultSet resultSet = mStock.getResultSet("select * from stock where orderId = '" + orderId + "'");
        double quality = 0;
        while (resultSet.next()) {
            String cause = resultSet.getString("cause");
            String other = resultSet.getString("other");
            if (cause == null || !cause.equals("")) {
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
        mNew = new NnExcelReader(news);
        //initHistory();
        mHistory = new NnAccdbReader(mNnConfiguration.getString("history"));
        mStock = new NnAccdbReader((mNnConfiguration.getString("stock")));
    }
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
