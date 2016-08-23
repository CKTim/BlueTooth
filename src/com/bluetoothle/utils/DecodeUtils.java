package com.bluetoothle.utils;

import java.math.BigInteger;

import android.util.Log;

public class DecodeUtils {

	// 将GB2312转化为中文,如bdadcbd5→江苏
	public static String stringToGbk(String string) throws Exception {
		byte[] bytes = new byte[string.length() / 2];
		for (int j = 0; j < bytes.length; j++) {
			byte high = Byte.parseByte(string.substring(j * 2, j * 2 + 1), 16);
			byte low = Byte.parseByte(string.substring(j * 2 + 1, j * 2 + 2),
					16);
			bytes[j] = (byte) (high << 4 | low);
		}
		String result = new String(bytes, "GBK");
		return result;
	}

	// 将中文转化为GB2312编码,并且以byte[]形式返回,如江苏→byte[]{0xbd,0xad,0xcb..}
	public static byte[] gbkToString(String str) throws Exception {
		return new String(str.getBytes("GBK"), "gb2312").getBytes("gb2312");
	}

	// 将十六进制的byte[]转化为string，如byte[]{0x7e，0x80,0x11,0x20}→7e801120
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	// 将字符数组byte[]变为16进制,每个byte有空格分开，例如7e 00 03
	public static StringBuilder byte2HexStr(byte[] data) {

		if (data != null && data.length > 0) {
			StringBuilder stringBuilder = new StringBuilder(data.length);
			for (byte byteChar : data) {
				stringBuilder.append(String.format("%02X ", byteChar));
			}
			return stringBuilder;
		}
		return null;
	}

	// 将byte[]数组转化为8、10等各种进制，例如byte[0x11,0x20]→4384，binary（byte[] 10)其中10代表十进制
	public static String bytesToAllHex(byte[] bytes, int radix) {
		return new BigInteger(1, bytes).toString(radix);// 这里的1代表正数
	}

	// 将String的十六进制转化为byte的十六进制，例如7e→new byte[]{0x7e}
	public static byte[] HexString2Bytes(String src) {
		byte[] ret = new byte[src.length() / 2];
		byte[] tmp = src.getBytes();
		for (int i = 0; i < tmp.length / 2; i++) {
			ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
		}
		return ret;
	}

	public static byte uniteBytes(byte src0, byte src1) {
		byte _b0 = Byte.decode("0x" + new String(new byte[] { src0 }))
				.byteValue();
		_b0 = (byte) (_b0 << 4);
		byte _b1 = Byte.decode("0x" + new String(new byte[] { src1 }))
				.byteValue();
		byte ret = (byte) (_b0 ^ _b1);
		return ret;
	}

	// 四位数，未满四位前面补0
	public static String AddZeroToFour(String string) {
		String newString = null;
		if (string.length() == 1) {
			newString = "000" + string;
		}
		if (string.length() == 2) {
			newString = "00" + string;
		}
		if (string.length() == 3) {
			newString = "0" + string;
		}
		if (string.length() == 4) {
			newString = string;
		}
		return newString;
	}
	
   //两位数,未满两位数前面补0
	public static String AddZeroToTwo(String string){
		String newString = null;
		if(string.length()==1){
			newString="0"+string;
		}else{
			newString = string;
		}
		return newString;
		
	}
}
