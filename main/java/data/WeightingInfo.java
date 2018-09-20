package main.java.data;

public class WeightingInfo extends NnPolypeptide {
    private String date;
    private String packages = "";
    private String coordinate = "";
    private String group = "";
    private String cause = "";
    private String other;// 其他一些信息
    private boolean canDelete = false;// 这条数据是否可以从主表中删除

    public WeightingInfo(String orderId) {
        super(orderId);
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }

    public boolean isAvailable() {
        return !getOrderId().equals("");
    }

    public boolean canDelete() {
        return canDelete;
    }

    public void setDate(String date) {
        this.date = date;
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

    public String getDate() {
        return date;
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
