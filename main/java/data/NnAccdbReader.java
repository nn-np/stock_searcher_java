package main.java.data;

import java.sql.*;

public class NnAccdbReader {
    private Statement mStatement;
    private String mUrl;
    private Connection mConnection;
    public NnAccdbReader(String url) throws ClassNotFoundException, SQLException {
        mUrl = url;
        Class.forName("com.hxtt.sql.access.AccessDriver");
        mConnection = DriverManager.getConnection("jdbc:Access:///" + mUrl);
        mStatement = mConnection.createStatement();
    }

    public void close() throws SQLException {
        mConnection.close();
        mStatement.close();
    }

    public Statement getStatement() throws SQLException {
        return mConnection.createStatement();
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

    public int executeUpdate(String sql) throws SQLException {
        System.out.println(sql);
        return mStatement.executeUpdate(sql);
    }
}
