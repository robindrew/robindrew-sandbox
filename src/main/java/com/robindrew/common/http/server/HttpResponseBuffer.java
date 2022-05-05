package com.robindrew.common.http.server;

import java.nio.ByteBuffer;

import com.google.common.base.Charsets;

public class HttpResponseBuffer {

	private final byte[] bytes;
	private final ByteBuffer buffer;

	public HttpResponseBuffer(int length) {
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
		return new String(bytes, 0, buffer.position());
	}

	public void set(String text) {
		byte[] data = text.getBytes(Charsets.UTF_8);
		buffer.put(data);
	}

}
