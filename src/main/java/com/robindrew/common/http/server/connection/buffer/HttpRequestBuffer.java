package com.robindrew.common.http.server.connection.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.google.common.base.Charsets;

public class HttpRequestBuffer {

	private static final byte[] END_HEADERS = "\r\n\r\n".getBytes();

	private final byte[] bytes;
	private final ByteBuffer buffer;
	private volatile Charset charset = Charsets.UTF_8;

	public HttpRequestBuffer(int length) {
		bytes = new byte[length];
		buffer = ByteBuffer.wrap(bytes);
	}

	public void reset() {
		buffer.clear();
	}

	public ByteBuffer get() {
		return buffer;
	}

	public String toString() {
		return new String(bytes, 0, buffer.position(), charset);
	}

	public boolean isReadyToHandle() {
		// The HTTP request is ready once the \r\n sequence has been provided
		int index = indexOf(END_HEADERS, this.bytes, buffer.position());
		return index != -1;
	}

	private int indexOf(byte[] needle, byte[] haystack, int length) {
		int needleIndex = 0;
		for (int i = 0; i < length; i++) {
			if (haystack[i] == needle[needleIndex]) {
				needleIndex++;
				if (needleIndex == needle.length) {
					return (i - needle.length) + 1;
				}
			}
		}
		return -1;
	}

}
