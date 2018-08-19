package com.nn.data;

public class WeightingInfo {
    private String orderId;
    private String date;
    private double quality;
    private String packages;
    private String coordinate;
    private String group;
    private String cause;
    private String comments;// 备注
    private double a_purity;// 实际纯度
    private double a_mw;// 实际分子量
    private boolean isAvailable = true;// 这条数据是否有效
    private boolean canDelete = false;// 这条数据是否可以从主表中删除

    public WeightingInfo(String orderId) {
        if (orderId == null || orderId.equals("")) {
            isAvailable = false;
            return;
        }
        this.orderId = orderId;
        date = packages = coordinate = group = cause = comments = "";
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments == null ? "" : comments;
    }

    public double getA_purity() {
        return a_purity;
    }

    public void setA_purity(double a_purity) {
        this.a_purity = a_purity;
    }

    public double getA_mw() {
        return a_mw;
    }

    public void setA_mw(double a_mw) {
        this.a_mw = a_mw;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public boolean canDelete() {
        return canDelete;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }

    public void setPackages(String packages) {
        this.packages = packages == null ? "" : packages;
    }

    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate == null ? "" : coordinate;
    }

    public void setGroup(String group) {
        this.group = group == null ? "" : group;
    }

    public void setCause(String cause) {
        canDelete = !cause.equals("");
        this.cause = cause;
    }

    public void isCanDelete(boolean bol) {
        canDelete = bol;
    }

    public String getOrderId() {
        return orderId;
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

    public String getGroup() {
        return group;
    }

    public String getCause() {
        return cause;
    }
}
