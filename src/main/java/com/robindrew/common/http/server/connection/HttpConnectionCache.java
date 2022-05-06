package com.robindrew.common.http.server.connection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpConnectionCache {

	private static final Logger log = LoggerFactory.getLogger(HttpConnectionCache.class);

	private final AtomicInteger nextId = new AtomicInteger(0);
	private final int bufferSize;
	private final List<HttpConnection> connectionList = new ArrayList<>();

	public HttpConnectionCache(int bufferSize) {
		if (bufferSize < 1000) {
			throw new IllegalArgumentException("bufferSize=" + bufferSize);
		}
		this.bufferSize = bufferSize;
	}

	public HttpConnection take() {
		HttpConnection connection = null;
		synchronized (connectionList) {
			if (!connectionList.isEmpty()) {
				connection = connectionList.remove(connectionList.size() - 1);
			}
		}
		if (connection == null) {
			connection = new HttpConnection(nextId.incrementAndGet(), bufferSize, this);
			log.info("[Cached] Connection #{}", connection.getConnectionId());
		}
		return connection;
	}

	public void release(HttpConnection connection) {
		connection.reset();
		synchronized (connectionList) {
			connectionList.add(connection);
		}
	}

}
