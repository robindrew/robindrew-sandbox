package com.robindrew.common.http.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpConnection implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(HttpConnection.class);

	private volatile long id;
	private volatile long timeOpened = 0;
	private volatile long timeClosed = 0;
	private volatile SocketChannel channel = null;

	private final byte[] readBytes;
	private final ByteBuffer readBuffer;

	public HttpConnection(int bufferSize) {
		if (bufferSize < 1000) {
			throw new IllegalArgumentException("bufferSize=" + bufferSize);
		}
		this.readBytes = new byte[bufferSize];
		this.readBuffer = ByteBuffer.wrap(readBytes);
	}

	public long getId() {
		return id;
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

	public void open(SocketChannel channel, long id) {
		if (isOpen()) {
			throw new IllegalStateException("Data is already open");
		}
		this.id = id;
		this.timeOpened = System.currentTimeMillis();
		this.channel = channel;
	}

	public void close() {
		if (!isOpen()) {
			throw new IllegalStateException("Data is not open");
		}
		try {
			if (channel != null) {
				timeClosed = System.currentTimeMillis();
				channel.close();
			}
		} catch (Exception e) {
			log.warn("Error closing connection *" + id, e);
		} finally {
			channel = null;

		}
	}

	public void reset() {
		timeOpened = 0;
		timeClosed = 0;
		channel = null;
		readBuffer.clear();
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

	@Override
	public void run() {
		try {

		} catch (Throwable t) {
			close();
		}
	}

}
