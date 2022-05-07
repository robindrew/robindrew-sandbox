package com.robindrew.common.http.server;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.http.server.connection.HttpConnection;

public class HttpReaderService {

	private static final Logger log = LoggerFactory.getLogger(HttpReaderService.class);

	private final HttpEventMonitor monitor;
	private final BlockingQueue<SelectionKey> keyQueue = new ArrayBlockingQueue<>(10000);
	private final List<HttpReaderThread> readerList = new ArrayList<>();
	private final Set<SelectionKey> handledKeys = new HashSet<>();
	private final ExecutorService handlerPool;

	public HttpReaderService(int readerCount, ExecutorService handlerPool, HttpEventMonitor monitor) {
		this.handlerPool = handlerPool;
		this.monitor = monitor;

		for (int i = 1; i <= readerCount; i++) {
			HttpReaderThread reader = new HttpReaderThread(i);
			readerList.add(reader);
			reader.start();
		}
	}

	public void readRequestAsync(SelectionKey key) {
		keyQueue.offer(key);
	}

	private void readRequest(int readerThreadId, SelectionKey connectionKey) throws Exception {
		try {

			// Key has a connection assigned?
			HttpConnection connection = (HttpConnection) connectionKey.attachment();

			// Existing Connection
			if (connection == null || connection.isClosed() || connection.isHandling()) {
				return;
			}

			// Read Data
			if (connectionKey.isValid() && connectionKey.isReadable()) {

				// Attempt to read part (or all) of the request
				connection.readRequest();
				monitor.read();

				// Finished reading HTTP request?
				if (connection.isReadyToHandle()) {

					// Hand over connection to be handled separately
					handlerPool.submit(connection);
					monitor.handle();
				}
			}

		} catch (CancelledKeyException cke) {
			log.warn("Key cancelled: " + connectionKey);
		} catch (Exception e) {
			log.warn("Exception handling key: " + connectionKey, e);
		}
	}

	private boolean lock(SelectionKey key) {
		synchronized (handledKeys) {
			return handledKeys.add(key);
		}
	}

	private void unlock(SelectionKey key) {
		synchronized (handledKeys) {
			handledKeys.remove(key);
		}
	}

	private class HttpReaderThread extends Thread {

		private final int id;

		public HttpReaderThread(int id) {
			super("HttpReaderThread-" + id);
			this.id = id;
			log.info("[Created] {}", getName());
		}

		public void run() {
			try {
				while (true) {
					readRequest();
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		private void readRequest() throws Exception {

			// Wait for next available key
			SelectionKey key = keyQueue.take();
			if (!key.isValid()) {
				return;
			}

			// Check no other handler is already handling it
			if (!lock(key)) {
				return;
			}
			try {
				HttpReaderService.this.readRequest(id, key);
			} finally {
				unlock(key);
			}
		}
	}

}
