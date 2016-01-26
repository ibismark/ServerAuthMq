package mqtt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
 
public class DbTest {
 
    public static void main(String[] args) {
 
        Connection connection = null;
        Statement statement = null;
 
        try {
            Class.forName("org.sqlite.JDBC");
 
            connection = DriverManager.getConnection("jdbc:sqlite:/vagrant/ServerAuthMq/db/hoge.db");
            statement = connection.createStatement();
            String sql = "select * from fruits";
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                System.out.println(rs.getString(1));
            }
        } catch (ClassNotFoundException cnfe) {
          cnfe.printStackTrace();
        } catch (SQLException se) {
          se.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
