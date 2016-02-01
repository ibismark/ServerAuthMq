package mqttAuth;

import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.security.interfaces.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

//SSL
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

//SQLite3
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class MqttAuth{
	private static final String KEYPATH = "/vagrant/ServerAuthMq/key/keystore";
	private static final String KEYPASS = "mqttAuth";	
	private static final int PORT = 10000;	

	public static void main(String[] args) {
		SSLServerSocketFactory SSL_SOCKET_FACTORY = null;
		SSLSocket sslSock = null;
		SSLContext sContext = null;
		KeyStore keyStore = null;
		KeyManagerFactory kmf = null;
		SSLServerSocket serverSocket = null;


		try{

			// SSL init
			char[] keyPassChar = null;
			keyStore = KeyStore.getInstance("JKS");
			keyPassChar = KEYPASS.toCharArray();
			keyStore.load(new FileInputStream(KEYPATH), keyPassChar);
			kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(keyStore, keyPassChar);
			KeyManager[] key_managers = null;
			key_managers = kmf.getKeyManagers();
			sContext = SSLContext.getInstance("TLS");
			sContext.init(key_managers, null, null);
			SSL_SOCKET_FACTORY = sContext.getServerSocketFactory();

			// ポート番号:10000
			//serverSoc = new ServerSocket(10000);
			serverSocket=(SSLServerSocket)SSL_SOCKET_FACTORY.createServerSocket(PORT);
			serverSocket.setNeedClientAuth(false);
			boolean flag=true;

			//アクセス待ち
			System.out.println("waiting...  ");
			while(flag){
				Socket socket=null;
				socket = serverSocket.accept();
				//スレッドスタート
				new ServerThread(socket).start();
			}
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}finally{
			try{
				if (serverSocket != null){
					serverSocket.close();
				}
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
}



class ServerThread extends Thread{
	private Socket soc;
	private static final String DBPATH = "/vagrant/ServerAuthMq/db/mqtt.db";
	Connection con = null;
        Statement smt = null;


	//コンストラクタ	
	public ServerThread(Socket sct){
		this.soc=sct;
		try{
                        this.con = new DbConnect(DBPATH).createConnection();
                        this.smt = this.con.createStatement();

		}catch(SQLException sqle){
                        sqle.printStackTrace();
		}

		System.out.println("create new connection.  Connect to " + this.soc.getInetAddress());
	}


	//出力用
	public static String printBytes(byte[] b){
		String s= "";
		for(int i = 0 ; i < b.length; i++){
			s = s + Integer.toHexString((0x0f&((char)b[i])>>4));
			s = s + Integer.toHexString(0x0f&(char)b[i]); }
		return s;
	}



        //DB問い合わせ
        public ResultSet dbQuery(String usrID, String sel, String where){
                try{
                        String sql = String.format("SELECT %s FROM User WHERE %s='%s'", sel, where, usrID);
                        ResultSet rs = this.smt.executeQuery(sql);
                        //System.out.println(rs.getString(sel));
                        return rs;

                }catch(SQLException sqle){
                        sqle.printStackTrace();
                }

                return null;
        }


	//共通鍵生成
	public byte[] generatedKey(String id){
		char[] password = id.toCharArray();
		byte[] salt = new byte[] {1};

		try{
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			KeySpec spec = new PBEKeySpec(password, salt, 65536, 128); // 128Bit
			SecretKey tmp = factory.generateSecret(spec);
			return tmp.getEncoded();

		}catch(NoSuchAlgorithmException ne){
                        ne.printStackTrace();
		}catch(InvalidKeySpecException ie){
			ie.printStackTrace();
		}

		//KeyGenerator gen = KeyGenerator.getInstance("AES");
		//gen.init(128);
		//SecretKey key=gen.generateKey();

		return null;

	}


	//RabbitMQへアカウント登録	
	public static void RMQSignUP(String user, String hash_pass){
		System.out.println("password: " + hash_pass);
		try{
			ProcessBuilder pb1 = new ProcessBuilder("sudo", "rabbitmqctl", "add_user", user, hash_pass);
			Process process1 = pb1.start();
			ProcessBuilder pb2 = new ProcessBuilder("sudo", "rabbitmqctl", "set_permissions", user, ".*", ".*", ".*");
			Process process2 = pb2.start();
			System.out.println("Sined up");
				 
		}catch(Exception exc){
			exc.printStackTrace();
		}


	}



	public void run(){
		BufferedReader in = null;
        	BufferedWriter out = null;
		DataOutputStream dos = null;
		DataInputStream dis = null;
		ByteBuffer bytebuf = null;

		int len = 0;
		byte[] challenge = new byte[16];
		byte[] usrID = null;
		byte[] nonceR = new byte[16];
		byte[] nonceRR = new byte[16];
		byte[] c_hash = null;
		byte[] s_hash = null;
		byte[] hh = null;
		SecureRandom r = null;
		
		try{
			
			//nonce Rを送信
			r = new SecureRandom();
			r.nextBytes(challenge);
			dos = new DataOutputStream(this.soc.getOutputStream());
			dos.write(challenge);
			dos.flush();


			//(usrID, nonceR, nonceRR, hash)をクライアントから受け取る 
			//usrIDとc_hashは先にバイト長を受け取ってからデータを受け取る
			dis = new DataInputStream(this.soc.getInputStream());
			len = dis.readInt();
			usrID = new byte[len];
			dis.readFully(usrID);
			dis.readFully(nonceR);
			dis.readFully(nonceRR);
			len = dis.readInt();
			c_hash = new byte[len];
			dis.readFully(c_hash);


			//確認用
			System.out.println(printBytes(nonceR));
			System.out.println(printBytes(nonceRR));
			System.out.println(printBytes(c_hash));
		

			//hash値の生成
			//hash' = (usrID, pass', nonceR, nonceRR)	
			ResultSet p = dbQuery(new String(usrID, "UTF-8"), "password", "user_id");
			byte[] pass = p.getString("password").getBytes();
			
			bytebuf = ByteBuffer.allocate(usrID.length + pass.length + nonceR.length + nonceRR.length);
			bytebuf.put(usrID);
			bytebuf.put(pass);
			bytebuf.put(nonceR);
			bytebuf.put(nonceRR);
			hh = bytebuf.array();

			MessageDigest md = MessageDigest.getInstance("SHA-1");
			s_hash = md.digest(hh);

		
			//hash値の検証
			boolean succeeded = Arrays.equals(s_hash, c_hash);
			if(succeeded){
				System.out.println("Authentication: Successfully ->  " + this.soc.getInetAddress());		

				//認証通知
				dos.writeBoolean(succeeded);
				dos.flush();

				//RabbitMQへユーザ登録
				RMQSignUP(new String(usrID, "UTF-8"), printBytes(c_hash));
				
			}else{
				System.out.println("Authentication: Invalid id/password -> " + this.soc.getInetAddress());
			        	
			}
	
			
		}catch(IOException ioe){
			ioe.printStackTrace();
		}catch(NoSuchAlgorithmException ne){
			ne.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
                }finally{
                        try{
                                if(this.soc != null){

                                        this.soc.close();
                                }
                        }
                        catch (IOException ioex){
                                ioex.printStackTrace();
                        }

			try{

                                if(this.smt != null)
                                        this.smt.close();
                                if(this.con != null)
                                        this.con.close();

                        }catch(SQLException sqle){
                                sqle.printStackTrace();
                        }

                }

	}

}
