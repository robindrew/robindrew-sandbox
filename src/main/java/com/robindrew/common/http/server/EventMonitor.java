package com.robindrew.common.http.server;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.text.Strings;
import com.robindrew.common.util.Threads;

class EventMonitor extends Thread {

	private static final Logger log = LoggerFactory.getLogger(EventMonitor.class);

	private long checkpoint1;
	private long checkpoint2;
	private final AtomicInteger acceptCount = new AtomicInteger();
	private final AtomicInteger readCount = new AtomicInteger();
	private final AtomicInteger handleCount = new AtomicInteger();

	public EventMonitor() {
		start();
	}

	public void accept() {
		acceptCount.incrementAndGet();
	}

	public void read() {
		readCount.incrementAndGet();
	}

	public void handle() {
		handleCount.incrementAndGet();
	}

	public void run() {
		checkpoint1 = System.currentTimeMillis();
		while (true) {
			Threads.sleep(1000);

			checkpoint2 = System.currentTimeMillis();
			int accepts = acceptCount.getAndSet(0);
			int reads = readCount.getAndSet(0);
			int handles = handleCount.getAndSet(0);
			if (reads > 0 || accepts > 0 || handles > 0) {
				log.info("[Events] accepts={}, reads={}, handles={} (in {})", accepts, reads, handles, Strings.duration(checkpoint2, checkpoint1));
			}
			checkpoint1 = checkpoint2;
		}
	}

}
