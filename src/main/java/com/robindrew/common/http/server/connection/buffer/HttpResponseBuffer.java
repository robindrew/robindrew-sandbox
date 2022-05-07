package com.robindrew.common.http.server.connection.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.google.common.base.Charsets;

public class HttpResponseBuffer {

	private static final byte[] RESPONSE_OK = "HTTP/1.1 200 OK\r\n".getBytes();
	private static final byte[] CONTENT_LENGTH = "Content-Length: ".getBytes();
	private static final byte[] CONNECTION_CLOSE = "Connection: close\r\n".getBytes();
	private static final byte[] NEW_LINE = "\r\n".getBytes();

	private final byte[] bytes;
	private final ByteBuffer buffer;
	private volatile Charset charset = Charsets.UTF_8;

	private final byte[] intBuffer = new byte[10];

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

	public void writeOk(String content) {
		writeOk(content.getBytes(charset));
	}

	public void writeOk(byte[] content) {
		if (content.length == 0) {
			throw new IllegalArgumentException("content is empty");
		}
		write(RESPONSE_OK);
		write(CONNECTION_CLOSE);
		writeContentLength(content.length);
		write(NEW_LINE);
		write(content);
	}

	public void writeContentLength(int length) {
		write(CONTENT_LENGTH);
		writeInt(length);
		write(NEW_LINE);
	}

	private void writeInt(int value) {
		if (value < 0) {
			throw new IllegalArgumentException();
		}

		// Encode integer as bytes
		int offset = 0;
		final byte[] intBuffer = this.intBuffer;
		for (int i = 0; i < intBuffer.length; i++) {
			offset = i;
			intBuffer[i] = (byte) ((value % 10) + 48);
			if (value < 10) {
				break;
			}
			value = value / 10;
		}

		// Transfer to buffer
		for (int i = 0; i <= offset; i++) {
			buffer.put(intBuffer[offset - i]);
		}
	}

	public void write(String text) {
		write(text.getBytes(Charsets.UTF_8));
	}

	public void write(byte[] data) {
		buffer.put(data);
	}

}
