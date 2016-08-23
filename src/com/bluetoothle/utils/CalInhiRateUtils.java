package com.bluetoothle.utils;

public class CalInhiRateUtils {
	private static double nYingPinVal;// 样品测试值
	private static double nCompareVal;// 对照值
	private static double nFenziVal;// 样品测试值和对照值之差
	private static double fYiZhiLvVal;

	// 样品大于对照
	public static double CalInhiRate(double nCompareVal, double nYingPinVal) {
		if (nYingPinVal > nCompareVal) {
			nFenziVal = nYingPinVal - nCompareVal;
			if (nFenziVal > 0 && nFenziVal < 4) {
				fYiZhiLvVal = 2.9 + 0.9 * nFenziVal;
			}
			if (nFenziVal == 4) {
				fYiZhiLvVal = 2.6;
			}
			if (nFenziVal == 5) {
				fYiZhiLvVal = 6.1;
			}
			if (nFenziVal == 6) {
				fYiZhiLvVal = 8.7;
			}
			if (nFenziVal > 6 && nFenziVal < 13) {
				fYiZhiLvVal = 10.1 + 1.7 * (nFenziVal - 7);
			}
			if (nFenziVal > 12 && nFenziVal < 17) {
				fYiZhiLvVal = (nFenziVal - 12) * 2.9 + 18.6;
			}
			if (nFenziVal > 16 && nFenziVal < 25) {
				fYiZhiLvVal = (nFenziVal - 16) * 2.1 + 30.2;
			}
			if (nFenziVal > 24 && nFenziVal < 56) {
				fYiZhiLvVal = (nFenziVal - 24) * 0.9 + 50.8;
			}
			if (nFenziVal > 55 && nFenziVal < 63) {
				fYiZhiLvVal = (nFenziVal - 55) * 2.9 + 78.7;
			}
			if (nFenziVal > 62) {
				fYiZhiLvVal = 100;
			}
			return fYiZhiLvVal;
		} else if (nCompareVal >= nYingPinVal) {
			nFenziVal = nCompareVal - nYingPinVal;
			if (nFenziVal < 4) {
				fYiZhiLvVal = 2.9 - 0.9 * nFenziVal;
			}
			if (nFenziVal > 3 && nFenziVal < 11) {
				fYiZhiLvVal = 7.7 - 0.7 * nFenziVal;
			}
			if (nFenziVal > 10) {
				fYiZhiLvVal = 0;
			}
			return fYiZhiLvVal;
		}
		return 100;

	}

}
