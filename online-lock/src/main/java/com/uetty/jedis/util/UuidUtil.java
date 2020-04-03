package com.uetty.jedis.util;

import java.util.UUID;

public class UuidUtil {

	private static final char[] _UU64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
			.toCharArray();

	public static String getUuid(){
		UUID uu = UUID.randomUUID();

		long L = uu.getMostSignificantBits();
		long R = uu.getLeastSignificantBits();

		char[] cs = new char[22];

		int hex;
		// 从L64位取10次，每次取6位, 1-10位
		long cur = L;
		for (int i = 10; i >= 1; --i) {
			hex = ((int) cur & 0x3f);
			cs[i] = _UU64[hex];
			cur = cur >>> 6;
		}
		hex = ((int) cur & 0xf);
		// 第0位只取4bit
		cs[0] = _UU64[hex];

		// 从R64位取4bit作为最后一个字符
		cur = R;
		hex = ((int) cur & 0xf);
		cs[21] = _UU64[hex];
		cur = cur >>> 4;
		// 从R64位取10次，每次取6位, 12-21位
		for (int i = 20; i >= 11; --i) {
			hex = ((int) cur & 0x3f);
			cs[i] = _UU64[hex];
			cur = cur >>> 6;
		}

		// 返回字符串
		return new String(cs);
	}
	
}
