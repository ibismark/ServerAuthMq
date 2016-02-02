package mqttAuth;

import java.sql.ResultSet;
import java.sql.Statement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnect{

	String file;

	public DbConnect(String file){
		this.file = file;
	}

	public Connection createConnection(){

		try{

			Class.forName("org.sqlite.JDBC");
			Connection connect = DriverManager.getConnection("jdbc:sqlite:" + this.file);
			return connect;

		}catch(ClassNotFoundException cnfe){
			cnfe.printStackTrace();
		}catch(SQLException e){
			e.printStackTrace();
		}

		return null;
	}




	//test
	public static void main(String[] args){
		Connection con = null;
		Statement smt = null;


		try{
			con = new DbConnect("/vagrant/ServerAuthMq/db/mqtt.db").createConnection();
			smt = con.createStatement();

			String sql = "SELECT * FROM user";
            		ResultSet rs = smt.executeQuery(sql);
            		while (rs.next()) {
                		System.out.println(rs.getString(1));
            		}
		
		}catch(SQLException sqle){
			sqle.printStackTrace();
		}finally{

			try{

				if(smt != null)
					smt.close();
				if(con != null)
					con.close();

			}catch(SQLException sqle){
				sqle.printStackTrace();
			}
		}

	}
}

