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
import java.util.Vector;

import static com.nn.data.NnPolypeptide.getQuality;

public class Control {
    private boolean isContinue = true;
    private NnExcelReader mNew;// 新单（这个是excel）
    private NnAccdbReader mStock;// 库存（下面两个是Access数据库）
    private NnAccdbReader mHistory;// 历史订单

    private NnListener mNnListener;

    //private NnConfiguration mNnConfiguration;// 配置信息
    private NnProperties mNnProperties;

    // 已查到的库存数量：足够和量不足，搜索的条数，搜索的次数
    private int enoughCount = 0, otherCount = 0, searchedCount, searchedTimes;

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
        outPut();
        mNnListener.complete();
        mNnProperties.put("searchedCount", "" + searchedCount);
        mNnProperties.put("searchedTimes", "" + searchedTimes);
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
        if (mHistory == null) {
            System.out.println("空指针！！");
            return;
        }

        int count = 0;
        int newSize = mNew.getRowSize();
        for (int i = 0; i < newSize; ++i) {

            if (!isContinue) return;// 如果用户取消查找库存，就结束

            if (i > 0) {
                mNew.createCell(i, 11);
            }

            String orderId = mNew.getCellString(i, 10);
            String sequence = mNew.getCellString(i, 12);
            NnPolypeptide nnNewPolypeptide = new NnPolypeptide(orderId, sequence);
            nnNewPolypeptide.setPurity(mNew.getCellString(i, 15));
            nnNewPolypeptide.setQuality(mNew.getCellString(i, 14));
            nnNewPolypeptide.setMw(mNew.getCellString(i, 17));
            nnNewPolypeptide.setModification(mNew.getCellString(i, 16));
            if (nnNewPolypeptide.isAvailable()) {// 如果有效
                findHistory(nnNewPolypeptide, i);// 得到所有的历史订单
                ++count;
            }
            mNnListener.progress((double) i / newSize);// 进度监听者
        }
        searchedCount += count;
        mNew.setCellValue(0, 11, "库存信息：" + enoughCount + "+" + otherCount + "（绿色背景量不足）");
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
                writeBack(str.toString(), i, quality >= nnNewPolypeptide.getQuality());
            }
        } catch (SQLException e) {
            mNnListener.errorInfo("库存或历史订单数据库发生错误！");
            e.printStackTrace();
        }
    }

    /**
     * 将得到的库存信息写回新单excel表格中
     * @param info 需要写入的信息
     * @param x 需要写入的位置（行）
     * @param isEnough 库存是否足够
     */
    private void writeBack(String info, int x, boolean isEnough) {
        // TODO 注意，这里还没有写入文件，需要调用output一次性写入文件
        if (isEnough) {
            mNew.setCellValue(x, 11, info);
            ++enoughCount;
        } else {
            mNew.setCellValue(x, 27, info);
            ++otherCount;
            CellStyle cellStyle = mNew.createCellStyle();
            cellStyle.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Cell cell = mNew.getCell(x, 11);
            cell.setCellValue(info);
            cell.setCellStyle(cellStyle);
        }
    }

    private double findStock(String orderId, Vector<String> stockInfo) throws SQLException {
        ResultSet resultSet = mStock.getResultSet("select * from stock where orderId = '" + orderId + "'");
        double quality = 0;
        while (resultSet.next()) {
            String cause = resultSet.getString("cause");
            String other = resultSet.getString("other");
            if (cause == null || cause.equals("")) {
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
    private void nnInit(String news){
        try {
            mNnProperties = new NnProperties("nn.xml");
        } catch (IOException | XMLStreamException e) {
            mNnListener.errorInfo("配置文件读取错误！");
            e.printStackTrace();
        }

        searchedCount = Integer.parseInt(mNnProperties.getProperty("searchedCount", "0"));
        searchedTimes = Integer.parseInt(mNnProperties.getProperty("searchedTimes", "0"));
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