package com.nn.main;

import com.nn.data.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import static com.nn.data.NnOther.getQuality;

public class Control {
    private boolean isContinue = true;
    private NnExcelReader mNew;// 新单（这个是excel）
    private NnAccdbReader mStock;// 库存（下面两个是Access数据库）
    private NnAccdbReader mHistory;// 历史订单

    private NnListener mNnListener;

    private Vector<NnStockInfo> mStockInfos;
    private NnStockInfo mNnStockInfo;// 临时库存信息，用于复用
    private NnProperties mNnProperties;

    // 已查到的库存数量：足够和量不足，搜索的条数，搜索的次数
    private int otherCount = 0, searchedCount, searchedTimes;

    public Control(String news, NnListener nnListener) {
        if (nnListener != null) {
            mNnListener = nnListener;
        }
        if (news == null || news.equals("")) {
            mNnListener.errorInfo("请先选择新单表格！");
            return;
        }
        new Thread(() -> {
            nnInit(news);// 初始化
            start();// 开始查找数据
            nnEnd();
        }).start();
    }

    private void nnEnd(){
        if (!isContinue) {// 如果用户在查找结束之前取消查找，说明用户不希望改变表格，这里return
            mNnListener.errorInfo("已取消!");
            return;
        }
        writeBack();
        outPut();
        mNnListener.complete();
        mNnProperties.put("searched_count", "" + searchedCount);
        mNnProperties.put("searched_times", "" + searchedTimes);
        try {
            mNnProperties.submit();
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(this::historyBackup).start();
    }

    private void historyBackup() {// 定时对历史数据进行备份，以免出现意外毁坏珍贵数据
        File history = new File(mHistory.getUrl());
        long hisTime = history.lastModified();
        File backup = new File("nn_backup/" + history.getName());
        long backTime = backup.lastModified();
        if ((backTime + 259200000) < hisTime) {// 如果3天没备份数据
            try {
                NnOther.nnBackup(mHistory.getUrl());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        int count = 0;
        int newSize = mNew.getRowSize();
        for (int i = 0; i < newSize; ++i) {

            if (!isContinue) return;// 如果用户取消查找库存，就结束

            if (i > 0) {
                mNew.createCell(i, 11);
            }
            NnPolypeptide nnNewPolypeptide = getNnPolypeptideFromExcel(i);
            if (nnNewPolypeptide.isAvailable()) {// 如果有效
                mNnStockInfo = new NnStockInfo(nnNewPolypeptide, i);
                findHistory();// 得到所有的历史订单
                ++count;
            }
            mNnListener.progress((double) i / newSize);// 进度监听者
        }
        searchedCount += count;
        mNew.setCellValue(0, 11, "库存信息：" + mStockInfos.size() + "+" + otherCount);
    }

    // 从excel表获取多肽（NnPolypeptide）对象
    private NnPolypeptide getNnPolypeptideFromExcel(int i) {
        String orderId = mNew.getCellString(i, 10);
        String sequence = mNew.getCellString(i, 12);
        NnPolypeptide nnNewPolypeptide = new NnPolypeptide(orderId, sequence);
        nnNewPolypeptide.setPurity(mNew.getCellString(i, 15));
        nnNewPolypeptide.setQuality(mNew.getCellString(i, 14));
        nnNewPolypeptide.setMw(mNew.getCellString(i, 17));
        nnNewPolypeptide.setModification(mNew.getCellString(i, 16));
        nnNewPolypeptide.setWorkNo(mNew.getCellString(i,0));
        nnNewPolypeptide.setComments(mNew.getCellString(i, 22));
        return nnNewPolypeptide;
    }

    // "orderId", "sequence", "purity", "mw"
    // 寻找历史订单，一个序列，会有很多的历史订单，注意它们的修饰和分子量是否相同（我的理解，分子量相同或者相差18，则修饰肯定相同）
    private void findHistory() {
        try {
            NnPolypeptide nnNewPolypeptide = mNnStockInfo.getNnNewPolypeptide();
            ResultSet resultHistory = mHistory.getResultSet("select * from history where sequence = '" + nnNewPolypeptide.getSequence() + "'");

            boolean isHave = false;

            while (resultHistory.next()) {
                NnPolypeptide nnHistoryPolypeptide = getNnPolypeptideFromAccdb(resultHistory);

                // 判断历史订单里是否存在这条新单数据，如果有，isHave会为true，将不再添加到数据库
                if (nnNewPolypeptide.getOrderId().equals(nnHistoryPolypeptide.getOrderId())) {
                    isHave = true;
                }

                int flag = nnHistoryPolypeptide.equalFlg(nnNewPolypeptide);
                if (flag > 0) {
                    findStock(nnHistoryPolypeptide, flag);
                }

            }

            if (!isHave) {
                String date = new SimpleDateFormat("yyyy/M/d").format(new Date());
                mHistory.execute("insert into history values ('" + date + "','" + nnNewPolypeptide.getOrderId() + "','" +
                        nnNewPolypeptide.getSequence() + "','" + nnNewPolypeptide.getPurity() + "','" + nnNewPolypeptide.getModification()
                        + "','" + nnNewPolypeptide.getMw() + "','" + nnNewPolypeptide.getComments() + "')");
            }

            if (mNnStockInfo.isAvailable()) {
                mStockInfos.add(mNnStockInfo);
            }
        } catch (SQLException e) {
            mNnListener.errorInfo("库存或历史订单数据库发生错误！");
            e.printStackTrace();
        }
    }

    // 从Access数据库获取多肽对象
    private NnPolypeptide getNnPolypeptideFromAccdb(ResultSet resultHistory) throws SQLException {
        NnPolypeptide nnHistoryPolypeptide = new NnPolypeptide(resultHistory.getString("orderId"), resultHistory.getString("sequence"));
        nnHistoryPolypeptide.setMw(resultHistory.getString("mw"));
        nnHistoryPolypeptide.setPurity(resultHistory.getString("purity"));
        nnHistoryPolypeptide.setModification(resultHistory.getString("modification"));
        return nnHistoryPolypeptide;
    }

    // todo 修饰不同的区别对待  对比的时候把标点符号去掉
    // flg是用来计算绝对质量用的，哈哈，发现我创造了很多新词，什么历史订单，绝对质量，希望这个项目没有第二个人维护（逃）
    // 那么解释一下绝对质量，免得以后懵逼，绝对质量就是如果历史订单纯度不足，除去3之后得到的质量
    private void findStock(NnPolypeptide nnHistoryPolypeptide, int flg) throws SQLException {
        String orderId = nnHistoryPolypeptide.getOrderId();
        ResultSet resultSet = mStock.getResultSet("select * from stock where orderId = '" + orderId + "'");
        while (resultSet.next()) {
            String cause = resultSet.getString("cause");
            if (cause == null || cause.equals("")) {
                String date = resultSet.getString("_date");
                if (date == null) {
                    continue;
                }
                double q = getQuality(resultSet.getString("quality").toCharArray());
                if (q > 0) {// 这里有质量才调用addStockInfo，所以通过这一步肯定会有库存
                    // TODO 注意这里没有读取packages和coordinate
                    NnStockInfo.StockInfo stockInfo = mNnStockInfo.makeStockInfo(nnHistoryPolypeptide, date, q, "", "");
                    stockInfo.setAbs_quality(q / flg);
                    mNnStockInfo.addStockInfo(stockInfo);
                }
            }
        }
    }

    /**
     * 将得到的库存信息写回新单excel表格中
     * 注意，这里还没有写入文件，需要调用output一次性写入文件
     */
    private void writeBack() {
        // flg = 0，库存足够，修饰没问题，flg = 1，库存不够，修饰没问题，flg = 2，修饰有可能有问题，库存有可能不够
        for (NnStockInfo info : mStockInfos) {
            int flg = info.getFlg();
            if (flg == 0) {
                mNew.setCellValue(info.getRowIndex(), 11, info.getInfo());
            } else {
                CellStyle cellStyle = mNew.createCellStyle();
                if (flg == 1) {
                    cellStyle.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
                } else {
                    cellStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
                }
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                int x = info.getRowIndex();
                mNew.setCellValue(x, 27, info.getInfo());
                Cell cell = mNew.getCell(x, 11);
                cell.setCellValue(info.getInfo());
                cell.setCellStyle(cellStyle);
                ++otherCount;
            }
        }
    }

    // 初始化，将历史订单存入数据库中，以及其他一些初始化工作
    private void nnInit(String news){
        mStockInfos = new Vector<>();
        try {
            mNnProperties = new NnProperties("nn.xml");
        } catch (IOException | XMLStreamException e) {
            mNnListener.errorInfo("配置文件读取错误！");
            e.printStackTrace();
        }

        searchedCount = Integer.parseInt(mNnProperties.getProperty("searched_count", "0"));
        searchedTimes = Integer.parseInt(mNnProperties.getProperty("searched_times", "0"));
        searchedTimes += 1;

        try {
            mNew = new NnExcelReader(news);
        } catch (IOException e) {
            mNnListener.errorInfo("新单文件读取错误！");
            e.printStackTrace();
        }
        try {
            mHistory = new NnAccdbReader(mNnProperties.getProperty("history","history(new).accdb"));
        } catch (ClassNotFoundException e) {
            mNnListener.errorInfo("Access数据库驱动错误！");
            e.printStackTrace();
        } catch (SQLException e) {
            mNnListener.errorInfo("历史订单数据库读取错误！");
            e.printStackTrace();
        }
        try {
            mStock = new NnAccdbReader((mNnProperties.getProperty("stock","stock.accdb")));
        } catch (ClassNotFoundException e) {
            mNnListener.errorInfo("Access数据库驱动错误！");
            e.printStackTrace();
        } catch (SQLException e) {
            mNnListener.errorInfo("库存数据库读取错误！");
            e.printStackTrace();
        }
    }
}