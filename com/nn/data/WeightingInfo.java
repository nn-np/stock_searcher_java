package com.nn.data;

public class WeightingInfo {
    private String orderId;
    private String date;
    private double quality;
    private String packages;
    private String coordinate;
    private String group;
    private String cause;
    private boolean isAvailable = true;// 这条数据是否有效
    private boolean canDelete = false;// 这条数据是否可以从主表中删除

    public WeightingInfo(String orderId) {
        if (orderId == null || orderId.equals("")) {
            isAvailable = false;
            return;
        }
        this.orderId = orderId;
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
        this.packages = packages;
    }

    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setCause(String cause) {
        if (!cause.equals("")) {
            canDelete = true;
        }
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
