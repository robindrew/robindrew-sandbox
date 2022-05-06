package com.robindrew.common.http.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.util.Threads;

public class LightHttpTestClient extends Thread {

	private static final Logger log = LoggerFactory.getLogger(LightHttpTestClient.class);

	public static void main(String[] args) throws Throwable {
		String host = "localhost";
		int port = 1111;
		int clients = 100;
		Random random = new Random(clients);

		CountDownLatch latch = new CountDownLatch(clients);
		for (int i = 1; i <= clients; i++) {
			new LightHttpTestClient(i, host, port, latch, random).start();
			latch.countDown();
		}

		Threads.sleepForever();
	}

	private final int id;
	private final InetSocketAddress address;
	private final CountDownLatch latch;
	private final byte[] message;

	public LightHttpTestClient(int id, String host, int port, CountDownLatch latch, Random random) throws IOException {
		this.id = id;
		this.latch = latch;
		this.address = new InetSocketAddress(host, port);
		this.message = new byte[100000];
		random.nextBytes(message);

	}

	public void run() {
		try {
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log.info("Running Client #" + id);
		while (true) {
			try (Socket socket = new Socket()) {
				socket.setSoLinger(false, 0);
				log.info("Connect #" + id);
				socket.connect(address);

				byte[] message = createMessage();
				OutputStream output = socket.getOutputStream();
				output.write(message);
				output.flush();

				
				StringWriter writer = new StringWriter();
				InputStream input = new BufferedInputStream(socket.getInputStream());
				while (true) {
					int read = input.read();
					if (read == -1) {
						break;
					}
					writer.write(read);
				}

				//log.info("Response: " + writer.toString());

			} catch (Throwable t) {
				t.printStackTrace();
			}
			Threads.sleep(5000);
		}
	}

	private byte[] createMessage() {
		String request = "GET / HTTP/1.1\r\nConnection: close\r\n\r\n";
		return request.getBytes();
	}

}