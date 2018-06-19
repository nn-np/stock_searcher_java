package com.nn.data;

import java.sql.*;

public class NnAccdbReader {
    private Statement mStatement;
    public NnAccdbReader(String url) throws ClassNotFoundException, SQLException {
        Class.forName("com.hxtt.sql.access.AccessDriver");
        Connection connection = DriverManager.getConnection("jdbc:Access:///" + url);
        mStatement = connection.createStatement();
    }

    public ResultSet getResultSet(String sql) throws SQLException {
        System.out.println(sql);
        return mStatement.executeQuery(sql);
    }

    /*public void createTable(String tableName, String[] tables) throws SQLException {
        StringBuilder sql = new StringBuilder("create table " + tableName + "(");
        int len = tables.length - 1, i;
        for (i = 0; i < len; ++i) {
            sql.append(tables[i]).append(", ");
        }
        sql.append(tables[i]).append(")");
        System.out.println(sql);
        mStatement.execute(sql.toString());
    }*/

    /*public void truncateTable(String table) throws SQLException {
        mStatement.execute("truncate table nn");
    }*/

    public boolean execute(String sql) throws SQLException {
        System.out.println(sql);
        return mStatement.execute(sql);
    }
}
