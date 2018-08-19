package com.nn.main;

import com.nn.data.*;
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

public class Control {
    private boolean isContinue = true;
    private NnExcelReader mNew;// 新单（这个是excel）
    private NnAccdbReader mHistory;// 历史订单

    private NnListener mNnListener;

    private Vector<NnStockInfo> mStockInfos;
    private NnStockInfo mNnStockInfo;// 临时库存信息，用于复用
    private NnProperties mNnProperties;

    // 已查到的库存数量：足够和量不足，搜索的条数，搜索的次数
    private int searchedCount, searchedTimes;

    public Control(String news, NnListener nnListener) {
        mNnListener = nnListener;
        if (news == null || news.equals("")) {
            mNnListener.errorInfo("请先选择新单表格！");
            return;
        }
        new Thread(() -> {
            if (nnInit(news)) {// 初始化
                start();// 开始查找数据
                nnEnd();
            }
        }).start();
    }

    private void nnEnd(){
        mNnListener.complete();
        if (!isContinue) {// 如果用户在查找结束之前取消查找，说明用户不希望改变表格，这里return
            mNnListener.errorInfo("已取消!");
            return;
        }
        writeBack();
        writeStockBack();
        outPut();
        mNnProperties.put("searched_count", "" + searchedCount);
        mNnProperties.put("searched_times", "" + searchedTimes);
        try {
            mNnProperties.submit();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mHistory.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        new Thread(this::historyBackup).start();
    }

    private void writeStockBack() {
        mNew.createSheet("库存汇总");// TODO 有时间这里优化下，主要是格式
        mNew.setCellValue(1, 0, 0, "新单Work No", null);
        mNew.setCellValue(1, 0, 1, "orderId", null);
        mNew.setCellValue(1, 0, 2, "质量", null);
        mNew.setCellValue(1, 0, 3, "分子量（实际）", null);
        mNew.setCellValue(1, 0, 4, "纯度（实际）", null);
        mNew.setCellValue(1, 0, 5, "库存日期", null);
        mNew.setCellValue(1, 0, 6, "袋", null);
        mNew.setCellValue(1, 0, 7, "坐标", null);
        mNew.setCellValue(1, 0, 8, "备注", null);
        int i = 1;
        for (NnStockInfo info : mStockInfos) {
            Vector<NnStockInfo.StockInfo> ifs = info.getStocks();
            mNew.setCellValue(1, i, 0, info.getNnNewPolypeptide().getWorkNo(), null);
            for (NnStockInfo.StockInfo if2 : ifs) {
                mNew.setCellValue(1, i, 1, if2.getOrderId(), null);
                mNew.setCellValue(1, i, 2, if2.getQuality() + "mg", null);
                mNew.setCellValue(1, i, 3, ""+if2.getMw(), null);
                mNew.setCellValue(1, i, 4, ("" + if2.getPurity()).replaceAll("\\.0", "%"), null);
                mNew.setCellValue(1, i, 5, if2.getDate(), null);
                mNew.setCellValue(1, i, 6, if2.getPackages(), null);
                mNew.setCellValue(1, i, 7, if2.getCoordinate(), null);
                mNew.setCellValue(1, i, 8, if2.getComments(), null);
                ++i;
            }
        }
    }

    private void historyBackup() {// 定时对历史数据进行备份，以免出现意外毁坏珍贵数据
        File history = new File(mHistory.getUrl());
        long hisTime = history.lastModified();
        File backup = new File("nn_backup/" + history.getName());
        long backTime = backup.lastModified();
        if ((backTime + 172800000) < hisTime) {// 如果3天没备份数据
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
            ResultSet resultHistory = mHistory.getResultSet("select * from history inner join stock_new on history.orderId = stock_new.orderId" +
                    " and sequence = '" + nnNewPolypeptide.getSequence() + "' order by quality desc");

            while (resultHistory.next()) {
                NnPolypeptide nnHistoryPolypeptide = getNnPolypeptideFromAccdb(resultHistory);

                NnStockInfo.StockInfo stockInfo = getStockInfo(resultHistory, nnHistoryPolypeptide);

                int flag = nnHistoryPolypeptide.equalFlg(nnNewPolypeptide);
                if (flag > 0 && stockInfo.getQuality() > 0) {// flg大于0，这条库存有效
                    stockInfo.setAbs_quality(flag);
                    mNnStockInfo.addStockInfo(stockInfo);
                }
            }

            try {
                String date = new SimpleDateFormat("yyyy/M/d").format(new Date());
                mHistory.execute("insert into history values ('" + date + "','" + nnNewPolypeptide.getOrderId() + "','" +
                        nnNewPolypeptide.getSequence() + "','" + nnNewPolypeptide.getPurity() + "','" + nnNewPolypeptide.getModification()
                        + "','" + nnNewPolypeptide.getMw() + "','" + nnNewPolypeptide.getComments() + "')");
            } catch (Exception ignored) {
            }

            if (mNnStockInfo.isAvailable()) {
                mStockInfos.add(mNnStockInfo);
            }
        } catch (SQLException e) {
            mNnListener.errorInfo("库存或历史订单数据库发生错误！");
            e.printStackTrace();
        }
    }

    // 从access获取库存信息
    private NnStockInfo.StockInfo getStockInfo(ResultSet resultHistory, NnPolypeptide nnHistoryPolypeptide) throws SQLException {
        NnStockInfo.StockInfo stockInfo = mNnStockInfo.makeStockInfo(nnHistoryPolypeptide);
        String date = resultHistory.getString("_date");
        stockInfo.setMw(resultHistory.getString("a_mw"));
        stockInfo.setDate(date);
        String packages = resultHistory.getString("package");
        stockInfo.setPackages(packages);
        String coordinate = resultHistory.getString("coordinate");
        stockInfo.setCoordinate(coordinate);
        return stockInfo;
    }

    // 从Access数据库获取多肽对象
    private NnPolypeptide getNnPolypeptideFromAccdb(ResultSet resultHistory) throws SQLException {
        NnPolypeptide nnHistoryPolypeptide = new NnPolypeptide(resultHistory.getString("orderId"), resultHistory.getString("sequence"));
        nnHistoryPolypeptide.setQuality(resultHistory.getString("quality"));
        nnHistoryPolypeptide.setMw(resultHistory.getString("mw"));
        nnHistoryPolypeptide.setPurity(resultHistory.getString("a_purity"));
        nnHistoryPolypeptide.setModification(resultHistory.getString("modification"));
        nnHistoryPolypeptide.setComments(resultHistory.getString("comments"));
        return nnHistoryPolypeptide;
    }

    /**
     * 将得到的库存信息写回新单excel表格中
     * 注意，这里还没有写入文件，需要调用output一次性写入文件
     */
    private void writeBack() {
        int enoughCount = 0, otherCount = 0;
        // 这里简直是向生活低下狗头的典型案例啊，别问我为什么写这么繁琐（解决.xls文件有部分颜色设置不正确问题，这问题很迷）
        CellStyle cellStyle = mNew.createCellStyle();
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
        CellStyle cellStyle1 = mNew.createCellStyle();
        cellStyle1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle1.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        // flg = 0，库存足够，修饰没问题，flg = 1，库存不够，修饰没问题，flg = 2，修饰有可能有问题，库存有可能不够
        for (NnStockInfo info : mStockInfos) {
            int flg = info.getFlg();
            if (flg == 0) {
                mNew.setCellValue(info.getRowIndex(), 11, info.getInfo());
                ++enoughCount;
            } else {
                int x = info.getRowIndex();
                String str = info.getInfo();
                if (flg == 1) {
                    mNew.setCellValue(x, 11, str, cellStyle);
                } else {
                    mNew.setCellValue(x, 11, str, cellStyle1);
                }
                mNew.setCellValue(x, 27, str);
                ++otherCount;
            }
        }
        mNew.setCellValue(0, 11, "库存信息：" + enoughCount + "+" + otherCount);
    }

    // 初始化，将历史订单存入数据库中，以及其他一些初始化工作
    private boolean nnInit(String news){
        mStockInfos = new Vector<>();
        try {
            mNnProperties = new NnProperties("nn.xml");
        } catch (IOException | XMLStreamException e) {
            mNnListener.errorInfo("配置文件读取错误！");
            e.printStackTrace();
            return false;
        }

        searchedCount = Integer.parseInt(mNnProperties.getProperty("searched_count", "0"));
        searchedTimes = Integer.parseInt(mNnProperties.getProperty("searched_times", "0"));
        searchedTimes += 1;

        try {
            mNew = new NnExcelReader(news);
        } catch (IOException e) {
            mNnListener.errorInfo("新单文件读取错误！");
            e.printStackTrace();
            return false;
        }
        try {
            mHistory = new NnAccdbReader(mNnProperties.getProperty("history","polypeptideInfo.accdb"));
        } catch (ClassNotFoundException e) {
            mNnListener.errorInfo("Access数据库驱动错误！");
            e.printStackTrace();
            return false;
        } catch (SQLException e) {
            mNnListener.errorInfo("历史订单数据库读取错误！");
            e.printStackTrace();
            return false;
        }
        return true;
    }
}