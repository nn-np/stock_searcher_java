package com.nn.data;

import java.util.regex.Pattern;

import static java.lang.Math.abs;

/**
 * 多肽
 */
public class NnPolypeptide {
    private String orderId;
    private double purity;// 纯度
    private String workNo;// work号
    private String sequence;// 氨基酸序列
    private double mw;// 分子量
    private double quality;// 需要的质量
    private String modification;// 修饰
    private String comments;// 备注

    public NnPolypeptide(String orderId, String sequence) {
        this.orderId = orderId == null ? "" : orderId;
        this.sequence = sequence == null ? "" : sequence;
        purity = mw = quality = 0;
    }

    // 这个方法用来格式化序列
    private String getSequence(String sequence) {
        char[] chars = new char[sequence.length()];
        int i = 0;
        for (char c : sequence.toCharArray()) {
            if (c > 64 && c < 126) {
                if (c > 96 && c < 124) {
                    c -= 32;
                }
                chars[i++] = c;
            }
        }
        return new String(chars, 0, i);
    }

    // TODO 所有的空白字符返回""，不要返回null
    // 这条订单是否有效（注：如果这里任何一个值无效，这条订单就无效）
    public boolean isAvailable() {
        return !orderId.equals("") && !sequence.equals("") && purity > -1 && mw > -1 && quality > -1;
    }

    public void setComments(String comments) {
        this.comments = comments == null ? "" : comments;
    }

    public String getComments() {
        return comments;
    }

    public void setModification(String modifications) {
        this.modification = modifications == null ? "" : modifications.trim();
    }

    public String getModification() {
        return modification;
    }

    public void setMw(String str) {
        if (str == null || str.equals("")) {
            mw = -1;
            return;
        }
        mw = getMaxValue(str.toCharArray());
    }

    public void setMw(double mw) {
        this.mw = mw;
    }

    public void setQuality(String str) {
        if (str == null || str.equals("")) {
            quality = -1;
            return;
        }
        quality = getMaxValue(str.toCharArray());
    }

    // 得到字符串中的最大值
    private double getMaxValue(char[] chars) {
        double value = -1;

        boolean flg = false;
        char[] cs = null;
        int i = 0;
        for (char c : chars) {
            if (!flg) {
                cs = new char[chars.length];
                flg = true;
            }
            if (c >= '0' && c <= '9' || c == '.') {
                cs[i++] = c;
                double d = Double.parseDouble(new String(cs));
                if (value < d) {
                    value = d;
                }
            } else {
                if (i > 0) {
                    flg = false;
                    i = 0;
                }
            }
        }
        return value;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }

    // TODO 以后把这里全格式化，就不用占用资源处理了
    public void setPurity(String str) {
        if (str == null || str.equals("")) {
            purity = -1;
            return;
        }
        char[] chars = new char[str.length()];
        int i = 0;
        for (char c : str.toCharArray()) {
            if (c >= '0' && c <= '9') {// 纯度没有小数点
                chars[i++] = c;
            }
        }
        if (i > 0) {
            purity = Double.parseDouble(new String(chars));
        } else {
            purity = 1;
        }
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

    public void setWorkNo(String workNo) {
        this.workNo = (workNo == null || !Pattern.compile("[0-9.]+").matcher(workNo).matches()) ? "" : workNo.substring(0, workNo.lastIndexOf('.'));
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getPurity() {
        return purity;
    }

    public String getWorkNo() {
        return workNo;
    }

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
        if ((d_value < 0.8) || (abs(d_value - 18) < 0.8)) {
            if (this.purity < nnPolypeptide.purity) {
                return 3;
            }
            return 1;
        }
        return -1;
    }
}
