package com.nn.data;

import java.util.Vector;

/**
 * 库存信息
 * @author nn_np
 * 创建日期 2018.7.20
 */
public class NnStockInfo {
    private NnPolypeptide nnNewPolypeptide;// 新单多肽信息

    private int rowIndex;// 在新单中的行数
    private Vector<StockInfo> mStocks;
    private double abs_quality;
    private double error_abs_quality;

    public NnStockInfo(NnPolypeptide nnPolypeptide, int rowIndex) {
        this.nnNewPolypeptide = nnPolypeptide;
        this.rowIndex = rowIndex;
        mStocks = new Vector<>();
        abs_quality = error_abs_quality = 0;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public boolean isAvailable() {
        return abs_quality + error_abs_quality > 0;
    }

    public NnPolypeptide getNnNewPolypeptide() {
        return nnNewPolypeptide;
    }

    public void addStockInfo(StockInfo stockInfo) {
        if (!isHave(stockInfo)) {// 用于排除重复的库存信息，别问我，我也不知道为什么就有重复的
            if (stockInfo.modificationFlg) {
                abs_quality += stockInfo.abs_quality;
            } else {
                error_abs_quality += stockInfo.abs_quality;
            }
            mStocks.add(stockInfo);
        }
    }

    private boolean isHave(StockInfo stockInfo) {
        for (StockInfo sto : mStocks) {
            if (sto.getOrderId().equals(stockInfo.getOrderId())) {
                return true;
            }
        }
        return false;
    }

    public String getInfo() {
        int flg = getFlg();
        boolean isError = flg == 2;// 这个值为true表示有错误
        StringBuilder str = new StringBuilder();
        int lens = mStocks.size();
        for (int is = 0; is < lens; ++is) {
            StockInfo stockInfo = mStocks.get(is);
            if (is > 0) {
                str.append(" ||  ");
            }
            if (!isError && stockInfo.modificationFlg) {// 无措，就把有有错的排除
                str.append(stockInfo.getOrderId()).append(" ").append(stockInfo.getDate()).append(" ").append(stockInfo.getQuality()).append("mg");
            } else if (isError) {// 有错，就把有错的加上
                str.append(stockInfo.getOrderId()).append(" ").append(stockInfo.getDate()).append(" ").append(stockInfo.getQuality()).append("mg");
                str.append(" ").append(stockInfo.getModification());
            }
        }
        return str.toString();
    }

    public int stockSize() {
        return mStocks.size();
    }

    public StockInfo getStockInfo(int index) {
        return mStocks.get(index);
    }

    public StockInfo makeStockInfo(NnPolypeptide nnPolypeptide, String date, double quality, String packages, String coordinate) {
        return new StockInfo(nnPolypeptide, date, quality, packages, coordinate);
    }

    public int getFlg() {
        if (abs_quality >= nnNewPolypeptide.getQuality()) {// 正确的质量够，就排除可能不正确的
            return 0;
        } else if (error_abs_quality == 0) {
            return 1;
        }
        return 2;// 这种情况质量有可能是够的，有可能不够，但结果里肯定包含有可能有错误的库存
    }

    // 库存临时类
    public class StockInfo {
        private NnPolypeptide nnHistoryPolypeptide;// 历史订单多肽信息
        private String date;// 库存日期
        private double quality;// 质量
        private String packages;// 袋子
        private String coordinate;// 坐标
        boolean modificationFlg;// 修饰是否相同

        // 注意，绝对质量一定要设置
        private double abs_quality;

        public StockInfo(NnPolypeptide nnPolypeptide, String date, double quality, String packages, String coordinate) {
            this.nnHistoryPolypeptide = nnPolypeptide;
            this.date = date;
            this.quality = quality;
            this.packages = packages;
            this.coordinate = coordinate;
            modificationFlg = nnHistoryPolypeptide.getModification().equals(nnNewPolypeptide.getModification());
        }

        public String getModification() {
            return nnHistoryPolypeptide.getModification();
        }

        public String getOrderId() {
            return nnHistoryPolypeptide.getOrderId();
        }

        public String getDate() {
            return date;
        }

        public double getQuality() {
            return quality;
        }

        public String getPackages() {
            return packages;
        }

        public String getCoordinate() {
            return coordinate;
        }

        public void setAbs_quality(double abs_quality) {
            this.abs_quality = abs_quality;
        }

        public double getAbs_quality() {
            return abs_quality;
        }
    }
}


