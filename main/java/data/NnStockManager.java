package main.java.data;

/**
 * 这个类用来专门管理库存的插入和删除以及库存坐标的维护工作
 */
public class NnStockManager {
    private NnAccdbReader mAccedb;

    public NnStockManager(NnAccdbReader accdb) {
        mAccedb = accdb;
    }

    // 向库存插入数据，返回受影响的条数，判断有没有坐标，插入到不同表中
    public int insertStock(WeightingInfo info) {
        return 0;
    }

    // 从库存删除数据，
    public int deleteStockNew(WeightingInfo info) {
        return 0;
    }

    // 从临时库存删除数据
    public int deleteStockTemporary(WeightingInfo info) {
        return 0;
    }
}
