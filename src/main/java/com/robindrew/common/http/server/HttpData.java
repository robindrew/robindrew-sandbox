package com.robindrew.common.http.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpData {

	private static final Logger log = LoggerFactory.getLogger(HttpData.class);

	private volatile long timeOpened = 0;
	private volatile long timeClosed = 0;
	private volatile SocketChannel channel = null;

	private final byte[] readBytes = new byte[1000000];
	private final ByteBuffer readBuffer = ByteBuffer.wrap(readBytes);

	public void open(SocketChannel channel) {
		if (isOpen()) {
			throw new IllegalStateException("Data is already open");
		}
		this.timeOpened = System.currentTimeMillis();
		this.channel = channel;
	}

	public void close() throws IOException {
		if (!isOpen()) {
			throw new IllegalStateException("Data is not open");
		}
		timeClosed = System.currentTimeMillis();
		channel.close();
		channel = null;
	}

	public void reset() {
		timeOpened = 0;
		timeClosed = 0;
		channel = null;
		readBuffer.clear();
	}

	public long getTimeOpened() {
		return timeOpened;
	}

	public long getTimeClosed() {
		return timeClosed;
	}

	public boolean isOpen() {
		return timeOpened > 0;
	}

	public boolean isClosed() {
		return timeClosed > 0;
	}

	public boolean read() throws IOException {
		if (isClosed()) {
			return false;
		}

		try {
			int read;
			while (true) {
				read = channel.read(readBuffer);

				// Closed?
				if (read == -1) {
					close();
					return false;
				}

				if (read == 0) {
					break;
				}
			}
		} catch (Exception e) {
			log.warn("Error reading data", e);
			close();
			return false;
		}
		return true;
	}

}
