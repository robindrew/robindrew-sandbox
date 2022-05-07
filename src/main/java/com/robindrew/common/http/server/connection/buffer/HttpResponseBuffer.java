package com.robindrew.common.http.server.connection.buffer;

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

	public void ok(byte[] data) {
		
	}

	public void write(String text) {
		write(text.getBytes(Charsets.UTF_8));
	}

	public void write(byte[] data) {
		buffer.put(data);
	}

}
