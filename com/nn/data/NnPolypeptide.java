package com.nn.data;

import java.util.regex.Pattern;

import static java.lang.Math.abs;

/**
 * 多肽
 */
public class NnPolypeptide {
    private String orderId;
    private double purity;// 纯度
    //private String workNo;// work号// TODO 这东西没用好像
    private String sequence;// 氨基酸序列
    private double mw;// 分子量
    private double quality;// 需要的质量
    private String modification;// 修饰

    public NnPolypeptide(String orderId, String sequence) {
        if (orderId == null || sequence == null) {
            return;
        }
        this.orderId = orderId;
        this.sequence = sequence.trim();
        purity = mw = quality = 0;
        //this.workNo = workNo;
    }

    // 这条订单是否有效（注：如果这里任何一个值无效，这条订单就无效）
    public boolean isAvailable() {
        return orderId != null && !orderId.equals("") && sequence != null && purity > -1 && mw > -1 && quality > -1;
    }

    public void setModification(String modifications) {
        modifications = getTrimString(modifications);
        this.modification = modifications;
    }

    private String getTrimString(String modification) {
        if (modification == null) {
            return "";
        }
        char[] buffer = new char[120];
        int len = modification.length();
        int t = 0;
        for (int i = 0; i < len; ++i) {
            char c = modification.charAt(i);
            if (c != ' ') {
                buffer[t] = c;
                ++t;
            }
        }
        return new String(buffer);
    }

    public String getModification() {
        return modification;
    }

    public void setMw(String str) {
        if (str == null || str.equals("")) {
            mw = -1;
            return;
        }
        str = str.substring(str.lastIndexOf("/") + 1);
        //System.out.println(str);
        mw = Pattern.compile("[0-9.]+").matcher(str).matches() ? Double.parseDouble(str) : -1;
    }

    public void setMw(double mw) {
        this.mw = mw;
    }

    public void setQuality(String str) {
        if (str == null || str.equals("")) {
            quality = -1;
            return;
        }
        str = str.replaceAll("mg", "").replaceAll("g", "")
                .replaceAll("无", "0").replaceAll(".*-", "").replaceAll(" ", "");
        //System.out.println(str);
        quality = (Pattern.compile("[0-9.]*").matcher(str).matches() && !str.equals("")) ? Double.parseDouble(str) : -1;
    }

    // 公司库存写的乱七八糟，醉了,只能把不符合规则的库存忽略
    // 如果格式没问题返回质量，有问题返回-1
    public static double getQuality(String str) {
        if (str == null || str.equals("")) {
            return -1;
        }
        str = str.replaceAll("mg", "").replaceAll("g", "")
                .replaceAll("无", "0").replaceAll(".*-", "").replaceAll(" ", "");
        return (Pattern.compile("[0-9.mg]*").matcher(str).matches() && !str.equals("")) ? Double.parseDouble(str) : -1;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }

    public void setPurity(String str) {
        if (str == null || str.equals("")) {
            purity = -1;
            return;
        }
        str = str.replaceAll(">", "").replaceAll("%", "")
                .replaceAll("crude", "1").replaceAll("Crude", "1");
        //System.out.println(str);
        purity = (Pattern.compile("[0-9.]+").matcher(str).matches() && !str.equals("")) ? Double.parseDouble(str) : -1;
    }

    public void setPurity(double purity) {
        this.purity = purity;
    }

    public double getQuality() {
        return quality;
    }

    public double getMw() {
        return mw;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /*public void setWorkNo(String workNo) {
        this.workNo = workNo;
    }*/

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getPurity() {
        return purity;
    }

    /*public String getWorkNo() {
        return workNo;
    }*/

    public String getSequence() {
        return sequence;
    }

    /**
     * 返回这条订单和参数中的订单纯度以及分子量的相似程度
     * @param nnPolypeptide 需要比较的多肽订单
     * @return 3 表示相同但是纯度低，需要除以3，1 表示相同并且纯度高，-1 表示分子量或修饰不同
     */
    public int equalFlg(NnPolypeptide nnPolypeptide) {
        double d_value = abs(this.mw - nnPolypeptide.mw);
        if (( d_value < 0.8) || (abs(d_value - 18) < 0.8)) {
            if (this.purity < nnPolypeptide.purity) {
                return 3;
            }
            return 1;
        }
        return -1;
    }
}
