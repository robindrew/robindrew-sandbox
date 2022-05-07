package com.robindrew.common.http.server;

public class HttpUtils {

	public static int indexOf(byte[] needle, byte[] haystack, int length) {
		int needleIndex = 0;
		for (int i = 0; i < length; i++) {
			if (haystack[i] == needle[needleIndex]) {
				needleIndex++;
				if (needleIndex == needle.length) {
					return (i - needle.length) + 1;
				}
			} else {
				needleIndex = 0;
			}
		}
		return -1;
	}

}
