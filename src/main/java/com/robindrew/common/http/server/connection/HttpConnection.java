package com.robindrew.common.http.server.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.http.server.connection.buffer.HttpRequestBuffer;
import com.robindrew.common.http.server.connection.buffer.HttpResponseBuffer;

public class HttpConnection implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(HttpConnection.class);

	private volatile long id = 0;
	private volatile long timeOpened = 0;
	private volatile long timeClosed = 0;
	private volatile long timeHandling = 0;
	private volatile SocketChannel channel = null;

	private final int connectionId;
	private final HttpRequestBuffer requestBuffer;
	private final HttpResponseBuffer responseBuffer;
	private final HttpConnectionCache cache;

	public HttpConnection(int id, int bufferSize, HttpConnectionCache cache) {
		if (bufferSize < 1000) {
			throw new IllegalArgumentException("bufferSize=" + bufferSize);
		}
		this.connectionId = id;
		this.requestBuffer = new HttpRequestBuffer(bufferSize);
		this.responseBuffer = new HttpResponseBuffer(bufferSize);
		this.cache = cache;
	}

	public int getConnectionId() {
		return connectionId;
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

	public boolean isHandling() {
		return timeHandling > 0;
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
		// log.info("CLOSED");
		try {
			if (channel != null) {
				timeClosed = System.currentTimeMillis();
				channel.close();
			}
		} catch (Exception e) {
			log.warn("Error closing connection *" + id, e);
		} finally {
			cache.release(this);
		}
	}

	public void reset() {
		id = 0;
		timeOpened = 0;
		timeClosed = 0;
		timeHandling = 0;
		channel = null;
		requestBuffer.reset();
		responseBuffer.reset();
	}

	public int readRequest() throws IOException {
		if (isClosed()) {
			return 0;
		}

		int totalRead = 0;
		try {
			int read;
			while (true) {
				read = channel.read(requestBuffer.get());

				// Closed?
				if (read == -1) {
					close();
					return totalRead;
				}

				if (read == 0) {
					break;
				}

				totalRead += read;
			}

		} catch (Exception e) {
			log.warn("Error reading data", e);
			close();
		}
		return totalRead;
	}

	@Override
	public void run() {
		try {
			String request = requestBuffer.getRequest();
			log.info("[Request] #{} ({} bytes)\n{}", id, request.length(), request);

			responseBuffer.writeOk("<title>Test Response</title><body><pre>" + request + "</pre></body>");
			String response = responseBuffer.toString();
			log.info("[Response] #{} ({} bytes)\n{}", id, response.length(), response);
			ByteBuffer buffer = responseBuffer.get();
			buffer.flip();
			channel.write(buffer);

		} catch (Throwable t) {
			log.error("Error handling request", t);
		} finally {
			close();
		}
	}

	public boolean isReadyToHandle() {
		if (isClosed()) {
			return false;
		}
		if (isHandling()) {
			return false;
		}
		if (!requestBuffer.isReadyToHandle()) {
			return false;
		}
		timeHandling = System.currentTimeMillis();
		return true;
	}

}
