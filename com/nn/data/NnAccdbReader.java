package com.nn.data;

import java.sql.*;

public class NnAccdbReader {
    private Statement mStatement;
    private String mUrl;
    public NnAccdbReader(String url) throws ClassNotFoundException, SQLException {
        mUrl = url;
        Class.forName("com.hxtt.sql.access.AccessDriver");
        Connection connection = DriverManager.getConnection("jdbc:Access:///" + mUrl);
        mStatement = connection.createStatement();
    }

    public String getUrl() {
        return mUrl;
    }

    public ResultSet getResultSet(String sql) throws SQLException {
        System.out.println(sql);
        return mStatement.executeQuery(sql);
    }

    public boolean execute(String sql) throws SQLException {
        System.out.println(sql);
        return mStatement.execute(sql);
    }
}
