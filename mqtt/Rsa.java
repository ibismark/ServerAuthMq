package mqtt;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * 公開鍵暗号の一種RSA暗号をJavaで実装
 * http://lab.moyo.biz/recipes/java/security/publickey.xsp
 * -キーペア生成
 * -個人鍵での復号
 * -公開鍵での暗号化
 * -鍵のファイル入出力機能
 * @author Guernsey
 */
public class Rsa {

	private static final String CRYPT_ALGORITHM = "RSA";

	//public static void main(String[] args) {
	public void show(byte[] digest) {
		// キーペア生成
		KeyPair keyPair = genKeyPair();

		// 個人鍵と公開鍵を取得
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();

		// 使用するキーペア
		System.out.println("個人鍵");
		System.out.println(byte2String(privateKey.getEncoded()));
		System.out.println("公開鍵");
		System.out.println(byte2String(publicKey.getEncoded()));

		// 暗号化したいデータをバイト列で用意
		//String string = "暗号化したい．abcabc123123!!";
		//byte[] src = string.getBytes();
		//System.out.println("暗号化するデータ（String）");
		//System.out.println(string);
		System.out.println("暗号化するデータ（byte）");
		System.out.println(byte2String(digest));

		// 暗号化
		byte[] enc = encrypt(digest, publicKey);
		System.out.println("暗号化後データ");
		System.out.println(byte2String(enc));

		// 復号
		byte[] dec = decrypt(enc, privateKey);
		System.out.println("復号後データ");
		System.out.println(byte2String(dec));

	}

    private static String byte2String(byte[] b){

        // ハッシュを16進数文字列に変換
        StringBuffer sb= new StringBuffer();
        int cnt= b.length;
        for(int i= 0; i< cnt; i++){
            sb.append(Integer.toHexString( (b[i]>> 4) & 0x0F ) );
            sb.append(Integer.toHexString( b[i] & 0x0F ) );
        }
        return sb.toString();
    }

	/**
	 * バイナリを公開鍵で復号
	 * @param source 復号したいバイト列
	 * @param privateKey
	 * @return 復号したバイト列．失敗時は null
	 */
	public static byte[] decrypt(byte[] source, PrivateKey privateKey){
		try {
			Cipher cipher = Cipher.getInstance(getCRYPT_ALGORITHM());
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			return cipher.doFinal(source);
		} catch (IllegalBlockSizeException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		} catch (BadPaddingException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InvalidKeyException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		} catch (NoSuchPaddingException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		}

		return null;
	}

	/**
	 * バイナリを秘密鍵で暗号化
	 * @param source 暗号化したいバイト列
	 * @param publicKey
	 * @return 暗号化したバイト列．失敗時は null
	 */
	public static byte[] encrypt(byte[] source, PublicKey publicKey){
		try {
			Cipher cipher = Cipher.getInstance(getCRYPT_ALGORITHM());
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			return cipher.doFinal(source);
		} catch (IllegalBlockSizeException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		} catch (BadPaddingException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InvalidKeyException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		} catch (NoSuchPaddingException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	/**
	 * 公開鍵暗号のキーペアを生成する．
	 * 公開鍵ビット長は 1024
	 * getPrivate(), getPublic() で秘密鍵，公開鍵を参照できる．
	 * @return キーペア．失敗時は null
	 */
	public static KeyPair genKeyPair(){
		KeyPairGenerator generator;
		try {
			generator = KeyPairGenerator.getInstance(getCRYPT_ALGORITHM());
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			generator.initialize(1024, random);
			return generator.generateKeyPair();
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		}

		return null;
	}

	/**
	 * 個人鍵を外部ファイルから読み込む
	 * @param filename ファイル名
	 * @return 個人鍵．失敗時は null
	 */
	public static PrivateKey loadPrivateKey(String filename){
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(getCRYPT_ALGORITHM());
			byte[] b = loadBinary(filename);
			EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(b);
			return keyFactory.generatePrivate(keySpec);
		} catch (InvalidKeySpecException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	/**
	 * 公開鍵を外部ファイルから読み込む
	 * @param filename ファイル名
	 * @return 公開鍵．失敗時は null
	 */
	public static PublicKey loadPublicKey(String filename){
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(getCRYPT_ALGORITHM());
			byte[] b = loadBinary(filename);
			EncodedKeySpec keySpec = new X509EncodedKeySpec(b);
			return keyFactory.generatePublic(keySpec);
		} catch (InvalidKeySpecException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	/**
	 * バイナリを外部ファイルから入力する
	 * @param filename ファイル名
	 * @return 読み込んだバイト配列．失敗時は null
	 */
	private static byte[] loadBinary(String filename){
		try {
			FileInputStream in = new FileInputStream(filename);
			byte[] b = null;
			in.read(b);
			return b;
		} catch (FileNotFoundException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;

	}

	/**
	 * 個人鍵を外部ファイルに保存する
	 * @param privateKey
	 * @param filename ファイル名
	 */
	public static void savePrivateKey(PrivateKey privateKey, String filename){
		saveBinary(privateKey.getEncoded(), filename);
	}

	/**
	 * 公開鍵を外部ファイルに保存する
	 * @param publicKey
	 * @param filename ファイル名
	 */
	public static void savePublickkey(PublicKey publicKey, String filename)
	{
		saveBinary(publicKey.getEncoded(), filename);
	}
	/**
	 * バイナリを外部ファイルに保存する
	 * @param b 保存したいバイト配列
	 * @param filename ファイル名
	 */
	private static void saveBinary(byte[] b, String filename)
	{
		FileOutputStream out;
		try {
			out = new FileOutputStream(filename);
			out.write(b);
			out.close();
		} catch (FileNotFoundException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(Rsa.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * @return the CRYPT_ALGORITHM
	 */
	public static String getCRYPT_ALGORITHM() {
		return CRYPT_ALGORITHM;
	}
}
