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

    public boolean execute(String sql) throws SQLException {
        System.out.println(sql);
        return mStatement.execute(sql);
    }
}
