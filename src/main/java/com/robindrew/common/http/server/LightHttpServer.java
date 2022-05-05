package com.robindrew.common.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightHttpServer {

	private static final Logger log = LoggerFactory.getLogger(LightHttpServer.class);

	public static void main(String[] args) throws Throwable {
		String host = "localhost";
		int port = 1111;

		new LightHttpServer(host, port).run();
	}

	private final Selector selector;
	private final ServerSocketChannel server;
	private final AtomicReference<Throwable> crashed = new AtomicReference<>();
	private final HttpDataCache dataCache = new HttpDataCache();

	public LightHttpServer(String host, int port) throws IOException {
		this(new InetSocketAddress(host, port));
	}

	public LightHttpServer(InetSocketAddress bindAddress) throws IOException {

		// Create selectors
		selector = Selector.open();
		server = ServerSocketChannel.open();
		server.bind(bindAddress);
		server.setOption(StandardSocketOptions.SO_RCVBUF, 5000000);
		server.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		log.info("SO_RCVBUF={}", server.getOption(StandardSocketOptions.SO_RCVBUF));
		log.info("SO_REUSEADDR={}", server.getOption(StandardSocketOptions.SO_REUSEADDR));
		server.configureBlocking(false);
		server.register(selector, SelectionKey.OP_ACCEPT, null);

		log.info("Non-Blocking Server Listening on {}", bindAddress);
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
			ServerSocketChannel socket = this.server;

			// Infinite loop..
			// Keep server running
			long checkpoint1 = System.currentTimeMillis();
			long checkpoint2;
			int accepts = 0;
			int reads = 0;
			while (isRunning()) {

				// Selects a set of keys whose corresponding channels are ready for I/O operations
				if (selector.select() == 0) {
					Thread.yield();
					continue;
				}

				Set<SelectionKey> keySet = selector.selectedKeys();
				for (SelectionKey key : keySet) {

					// New Connection
					if (key.isAcceptable()) {
						SocketChannel client = socket.accept();

						// Adjusts this channel's blocking mode to false
						client.configureBlocking(false);

						// Operation-set bit for read operations
						SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
						HttpData data = dataCache.take();
						data.open(client);
						clientKey.attach(data);
						accepts++;
					}

					// Read Data
					if (key.isReadable()) {
						HttpData data = (HttpData) key.attachment();
						if (!data.read()) {
							dataCache.release(data);
						}
						reads++;
					}

					// Logging
					checkpoint2 = System.currentTimeMillis();
					if (checkpoint2 - checkpoint1 > 1000) {
						log.info("[Status] accepts={}, reads={}", accepts, reads);
						accepts = 0;
						reads = 0;
						checkpoint1 = checkpoint2;
					}

				}
				keySet.clear();
			}
		} catch (Throwable t) {
			crashed.compareAndSet(null, t);
			log.error("Server crashed!", t);
		}
	}
}
