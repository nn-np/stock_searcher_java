package main.java.data;

import static java.lang.Math.abs;

/**
 * 多肽
 * 这里可以接受数据库格式优良的数据，也可以接受excel格式垃圾的数据
 */
public class NnPolypeptide {
    private String orderId;
    private long workNo;// work号
    private double purity;// 纯度（如果是历史订单，这里是实际纯度）
    private double mw;// 分子量
    private double quality;// 需要的质量（如果是历史订单，这里是库存质量）
    private String sequence;// 氨基酸序列
    private String modification;// 修饰
    private String comments;// 备注

    public NnPolypeptide(String orderId, String sequence) {
        this.orderId = orderId == null ? "" : orderId;
        this.sequence = sequence == null ? "" : getSequence(sequence);
        purity = mw = quality = 0;
    }

    public NnPolypeptide(String orderId) {
        this(orderId, null);
    }

    // 这个方法用来格式化序列
    public String getSequence(String sequence) {
        char[] chars = new char[sequence.length()];
        int i = 0;
        for (char c : sequence.toCharArray()) {
            if (c > 64 && c < 126) {
                if (c > 96 && c < 123) {
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
        return !orderId.equals("") && !sequence.equals("") && purity != 0 && mw > -1 && quality > -1;
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
        mw = NnOther.getMaxValue(str.toCharArray());
    }

    public void setMw(double mw) {
        this.mw = mw;
    }

    public void setQuality(String str) {
        if (str == null || str.equals("")) {
            quality = -1;
            return;
        }
        quality = NnOther.getQuality(str.toCharArray());
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }

    // 这里主要是excel表格读出的数据用
    public void setPurity(String str) {
        if (str == null || str.equals("")) {
            purity = -1;
            return;
        }
        str = str.toLowerCase();
        if (str.equals("crude")) {
            purity = -1;
            return;
        }
        if (str.equals("desalt")) {
            purity = -2;
            return;
        }
        purity = NnOther.getMaxValue(str.toCharArray());
        if (purity > 0 && purity < 1) {
            purity *= 100;
        }
    }

    public void setPurity(double purity) {
        this.purity = (purity > 0 && purity < 1) ? purity * 100 : purity;
    }

    public double getQuality() {
        return quality;
    }

    public String getQualityStr() {
        return quality + "mg";
    }

    public double getMw() {
        return mw;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setWorkNo(long workNo) {
        this.workNo = workNo;
    }

    public void setWorkNo(String string) {
        if (string.equals("")) {
            return;
        }
        workNo = (long) NnOther.getMaxValue(string.toCharArray());
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPurityStr() {
        if (purity == 0) {
            return "Desalt";
        }
        if (purity == -1) {
            return "Crude";
        }
        return ("" + purity).replaceAll("\\.0", "") + "%";
    }

    public double getPurity() {
        return purity;
    }

    public String getWorkNo() {
        return "" + workNo;
    }

    public String getSequence() {
        return sequence;
    }

    /**
     * 返回这条订单和参数中的订单纯度以及分子量的相似程度
     *
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
