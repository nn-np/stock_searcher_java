package main.java.data;

/**
 * 这个类用来将库存坐标转换为位图
 */
public class NnCoordinate {
    private String plate;// 板
    private int count;// 空余坐标个数，这个数每次计算好还是从数据库读取好？
    private long[] coos;// 坐标int数组
    private int size;// plate的大小，大板有220个坐标，需要4个long类型
    private int f;// flg，小板A1-A10，大板A1-A22，所以小板f=10，大板f=22

    /**
     * 构造
     * @param plate 板
     * @param oCoo 原始坐标字符串
     */
    public NnCoordinate(String plate, String oCoo) {
        this.plate = plate;
        size = plate.toCharArray()[1] == '大' ? 4 : 2;
        f = size == 2 ? 10 : 22;
        initCoos(oCoo);
        initCount();
    }

    public void addCoo(String coo) {
        int index = _toIndex(coo);
        long i = 1L << (index % 64);// 在64位中的位置
        int s = index / 64;// 在2个或4个long中的位置
        if ((coos[s] & i) == 0) {
            coos[s] |= i;
            ++count;
        }
    }

    // 将字符串转换成索引，如A3->4，这个索引是100个或220个坐标中的索引
    private int _toIndex(String str) {
        //int f = size == 2 ? 10 : 22;
        char[] chars = str.toCharArray();
        int i = (chars[0] - 65) * f;
        int s = (Integer.parseInt(new String(chars, 1, chars.length - 1)) - 1) % f;
        return i + s;
    }

    // 将索引转换成字符串，如12->B3
    private String _toString(int index) {
        int i = index % f;
        int s = index / f;
        return (char) (65 + s) + "" + (i + 1);
    }

    // 移除坐标
    public void removeCoo(String coo) {
        int index = _toIndex(coo);
        long i = 1L << (index % 64);// 在64位中的位置
        int s = index / 64;// 在2个或4个long中的位置
        if ((coos[s] & i) != 0) {
            coos[s] &= (~i);
            --count;
        }
    }

    public String getCoosString() {
        int counts = size == 2 ? 100 : 220;
        StringBuilder builder = new StringBuilder();
        builder.append(plate).append(',');
        for (int i = 0; i < counts; ++i) {
            int f_1 = i % 64;
            if (i > 0 && (i % f) == 0) {
                builder.append("\n,");
            }
            if ((coos[i / 64] & (1L << f_1)) != 0) {
                builder.append(_toString(i));
            }
            builder.append(',');
        }
        return builder.toString();
    }

    // 统计坐标个数
    private void initCount() {
        count  = 0;
        int index = size == 2 ? 100 : 220;
        for (int i = 0; i < index; ++i) {
            if ((coos[i / 64] & (1L << (i % 64))) != 0) {
                ++count;
            }
        }
    }

    // 这里返回的参数是数据库需要的字符串，如：21445,2541,
    public String getCoos() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            str.append(coos[i]).append(",");
        }
        return str.toString();
    }

    // 这里参数是从数据库得到的，如21121,21211,
    private void initCoos(String str) {
        long[] coos = new long[size];
        int i = 0;
        int i_1, i_2 = 0;
        char[] chars = str.toCharArray();
        for (i_1 = 0; i_1 < chars.length; ++i_1) {
            if (chars[i_1] == ',' && i < size) {
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
