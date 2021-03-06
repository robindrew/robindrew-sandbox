package com.robindrew.common.http.server;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.http.server.connection.HttpConnection;
import com.robindrew.common.http.server.connection.HttpConnectionCache;
import com.robindrew.common.util.Threads;

public class LightHttpServer {

	private static final Logger log = LoggerFactory.getLogger(LightHttpServer.class);

	public static void main(String[] args) throws Throwable {

		String host = "localhost";
		int port = 1111;
		int bufferSize = 10000;

		LightHttpServerConfig config = new LightHttpServerConfig(host, port);
		config.setConnectionBuffer(bufferSize);

		new LightHttpServer(config).run();
	}

	private final AtomicLong connectionCount = new AtomicLong(0);
	private final Selector selector;
	private final ServerSocketChannel server;
	private final AtomicReference<Throwable> crashed = new AtomicReference<>();
	private final HttpEventMonitor monitor = new HttpEventMonitor();
	private final HttpConnectionCache connectionCache;
	private final ExecutorService handlerPool;
	private final HttpReaderService readerService;

	public LightHttpServer(LightHttpServerConfig config) throws IOException {
		this.connectionCache = new HttpConnectionCache(config.getConnectionBuffer());
		this.handlerPool = Threads.newFixedThreadPool("HttpHandlerThread-%d", config.getHandlerThreads());

		// Create selectors
		selector = Selector.open();
		server = ServerSocketChannel.open();
		server.bind(config.getBindAddress());
		server.setOption(StandardSocketOptions.SO_RCVBUF, config.getReceiveBuffer());
		server.setOption(StandardSocketOptions.SO_REUSEADDR, config.isReuseAddress());
		server.configureBlocking(config.isBlocking());
		server.register(selector, SelectionKey.OP_ACCEPT, null);

		log.info("[Server] Receive Buffer: {}", server.getOption(StandardSocketOptions.SO_RCVBUF));
		log.info("[Server] Reuse Address: {}", server.getOption(StandardSocketOptions.SO_REUSEADDR));
		log.info("[Server] Blocking: {}", server.isBlocking());
		log.info("[Server] Listening on {}", server.getLocalAddress());

		readerService = new HttpReaderService(config.getEventThreads(), handlerPool, monitor);
	}

	public boolean isRunning() {
		return crashed.get() == null;
	}

	public boolean hasCrashed() {
		return crashed.get() != null;
	}

	public void run() {
		try {
			Selector selector = this.selector;

			// Infinite loop..
			// Keep server running
			while (isRunning()) {

				// Selects a set of keys whose corresponding channels are ready for I/O
				// operations
				if (selector.select() == 0) {
					Threads.sleep(20);
					continue;
				}

				Set<SelectionKey> keySet = selector.selectedKeys();
				for (SelectionKey key : keySet) {
					try {

						// New Connection
						if (key.attachment() == null) {
							if (key.isAcceptable()) {
								acceptKey(selector, server);
							}

							// Read Request
						} else {
							readerService.readRequestAsync(key);
						}
					} catch (CancelledKeyException cke) {
						log.warn("Key Cancelled", cke);
					}

				}
				keySet.clear();
			}
		} catch (Throwable t) {
			crashed.compareAndSet(null, t);
			log.error("Server crashed!", t);
		}
	}

	private void acceptKey(Selector selector, ServerSocketChannel socket) throws IOException {
		SocketChannel client = socket.accept();
		long id = connectionCount.incrementAndGet();
		client.configureBlocking(false);

		// Register the connection
		SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
		HttpConnection connection = connectionCache.take();
		connection.open(client, id);
		clientKey.attach(connection);
		monitor.accept();
	}

}
