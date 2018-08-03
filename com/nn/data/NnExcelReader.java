package com.nn.data;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

/**
 * 处理excel数据
 */
public class NnExcelReader {
    private Workbook mBook;
    private String url;

    public NnExcelReader(String url) throws IOException {
        this.url = url;
        System.out.println(url);
        InputStream in = new FileInputStream(url);
        String suffix = url.substring(url.lastIndexOf("."));
        if (".xls".equals(suffix)) {
            mBook = new HSSFWorkbook(in);
        } else if (".xlsx".equals(suffix)) {
            mBook = new XSSFWorkbook(in);
        } else {
            System.out.println("不支持的文件类型，请打开excel文件！");
        }
        in.close();
    }

    public NnExcelReader() {
        mBook = new XSSFWorkbook();
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void createSheet(String s) {
        mBook.removeSheetAt(1);
        mBook.createSheet(s);
    }

    public String getUrl() {
        return url;
    }

    // 得到表
    public Sheet getSheet(int sheetIndex) {
        return mBook.getSheetAt(sheetIndex);
    }

    public Sheet getSheet(String sheetName) {
        return mBook.getSheet(sheetName);
    }

    // 得到表中行数
    public int getRowSize(int sheetIndex) {
        return getSheet(sheetIndex).getLastRowNum() + 1;
    }

    public int getRowSize() {
        return getRowSize(0);
    }


    /**
     * @param sheetIndex 第几个表格
     * @param x          第几行
     * @param y          第几列
     * @return Cell
     */
    public Cell getCell(int sheetIndex, int x, int y) {
        Sheet sheet = mBook.getSheetAt(sheetIndex);
        if (sheet == null) {
            sheet = mBook.createSheet();
        }
        Row row = sheet.getRow(x);
        if (row == null) {
            row = sheet.createRow(x);
        }
        Cell cell = row.getCell(y);
        return cell == null ? row.createCell(y) : cell;
    }

    /**
     * 默认第一个表格
     */
    public Cell getCell(int x, int y) {
        return getCell(0, x, y);
    }

    // 得到cell中的文字
    public String getCellString(int sheetIndex, int x, int y) {
        Cell cell = getCell(sheetIndex, x, y);
        if (cell == null) {
            return "";
        }
        switch (cell.getCellTypeEnum()) {
            case NUMERIC:
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    // TODO 这里的日期格式写死了，M/d/yyyy，以后为了复用可以不用写死
                    return new SimpleDateFormat("M/d/yyyy").format(cell.getDateCellValue());
                }
                return new DecimalFormat("0.000").format(cell.getNumericCellValue());
            case STRING:
                return cell.getStringCellValue();
            default:
                return "";
        }
    }

    public String getCellString(int x, int y) {
        return getCellString(0, x, y);
    }

    public NnExcelReader setCellValue(int sheetIndex, int x, int y, String value, CellStyle style) {
        Cell cell = createCell(sheetIndex, x, y);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
        return this;
        // 注意，这里还没有写入文件 调用output一次性写入文件
    }

    public NnExcelReader setCellValue(int x, int y, String value) {
        return setCellValue(0, x, y, value, null);
    }

    public NnExcelReader setCellValue(int x, int y, String value, CellStyle style) {
        return setCellValue(0, x, y, value, style);
    }

    public Cell createCell(int sheetIndex, int x, int y) {
        Sheet sheet = mBook.getSheetAt(sheetIndex);
        if (sheet == null) {
            sheet = mBook.createSheet();
        }
        Row row = sheet.getRow(x);
        if (row == null) {
            row = sheet.createRow(x);
        }
        return row.createCell(y);
    }

    public Cell createCell(int x, int y) {
        return createCell(0, x, y);
    }

    public CellStyle createCellStyle() {
        return mBook.createCellStyle();
    }

    // 使用完一定要关闭，好习惯
    public void close() throws IOException {
        mBook.close();
    }

    public void output() throws IOException {
        FileOutputStream out = new FileOutputStream(url);
        mBook.write(out);
        out.flush();
        out.close();
    }
}
