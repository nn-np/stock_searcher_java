package com.nn.data;

/**
 * 这个类用来将库存坐标转换为位图
 */
public class NnCoordinate {
    private String plate;// 板
    private int count;// 空余坐标个数，这个数每次计算好还是从数据库读取好？
    private long[] coos;// 坐标int数组

    /**
     * 构造
     * @param plate 板
     * @param oCoo 原始坐标字符串
     */
    public NnCoordinate(String plate, String oCoo) {
        this.plate = plate;
        initCoos(oCoo);
        initCount();
    }

    public void addCoo(String coo) {
        char[] chars = coo.toCharArray();
        int index = (chars[0] >= 'G' && (chars[1] > '4' || chars.length > 2)) ? 1 : 0;
        long pointer = getCooIndex(chars);
        if (!isHave(pointer, index)) {
            coos[index] |= getCooIndex(chars);
            ++count;
        }
    }

    public void removeCoo(String coo) {
        char[] chars = coo.toCharArray();
        int index = (chars[0] >= 'G' && (chars[1] > '4' || chars.length > 2)) ? 1 : 0;
        long pointer = getCooIndex(chars);
        if (isHave(pointer, index)) {
            coos[index] &= (~pointer);
            --count;
        }
    }

    private long getCooIndex(char[] chars) {
        int i = chars.length > 2 ? 9 : 0;
        int flg = (chars[0] - 65) * 10 + (chars[1] - 49) + i;
        long value = 1;
        return value << flg;
    }

    private boolean isHave(long pointer,int index) {
        return (coos[index] & pointer) != 0;
    }

    public String getCoosString() {
        int index = 0;
        char head = 'A';
        boolean canMove = false;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 2; ++i) {
            long flg = 1;
            for (long j = 0; j < 64; ++j) {
                int f = ++index % 10;
                if (canMove) {
                    ++head;
                    builder.append('\n');
                }
                canMove = f == 0;
                if (((flg << j) & coos[i]) != 0) {// 有空位
                    builder.append(head).append(f == 0 ? 10 : f);
                }
                builder.append(',');
                if (i == 1 && j == 35) {
                    break;
                }
            }
        }
        return builder.toString();
    }

    public String getCoos() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < 2; ++i) {
            str.append(coos[i]).append(",");
        }
        return str.toString();
    }

    private void initCount() {
        count = 0;
        int index = 0;
        long flg = 1;
        for (int i = 0; i < 64; ++i) {
            if (((flg << i) & coos[index]) != 0) {
                ++count;
            }
            if (index == 0 && i == 63) {
                i = 0;
                index = 1;
            } else if (index == 1 && i == 35) {
                break;
            }
        }
    }

    private void initCoos(String str) {
        long[] coos = new long[2];
        int i = 0;
        int i_1, i_2 = 0;
        char[] chars = str.toCharArray();
        for (i_1 = 0; i_1 < chars.length; ++i_1) {
            if (chars[i_1] == ',') {
                coos[i++] = Long.parseLong(new String(chars, i_2, i_1 - i_2));
                i_2 = i_1 + 1;
            }
        }
        this.coos = coos;
    }

    public String getPlate() {
        return plate;
    }

    public int getCount() {
        return count < 0 ? 0 : count;
    }
}
