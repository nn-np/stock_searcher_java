package com.nn.main;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.*;

/**
 * 测试库存查找
 * 创建日期 2018.5.4
 * 注意：文件命名格式：新单（当日需要查库存的新单），库存（当前的最新库存），history（从系统中找的以前做过的单子）
 * 先这样明命名，上班看了再改
 * @author 徐世宁
 */

public class DataManager {
    public static void main(String[] args) {
        try {
            new Control();
        } catch (IOException | SQLException | ParserConfigurationException | SAXException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
