package com.robindrew.common.http.server;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpConnectionCache {

	private static final Logger log = LoggerFactory.getLogger(HttpConnectionCache.class);

	private final int bufferSize;
	private final List<HttpConnection> dataList = new ArrayList<>();

	public HttpConnectionCache(int bufferSize) {
		if (bufferSize < 1000) {
			throw new IllegalArgumentException("bufferSize=" + bufferSize);
		}
		this.bufferSize = bufferSize;
	}

	public HttpConnection take() {
		HttpConnection data = null;
		synchronized (dataList) {
			if (!dataList.isEmpty()) {
				data = dataList.remove(dataList.size() - 1);
			}
		}
		if (data == null) {
			data = new HttpConnection(bufferSize, this);
			log.info("New Data");
		}
		return data;
	}

	public void release(HttpConnection data) {
		data.reset();
		synchronized (dataList) {
			dataList.add(data);
		}
	}

}
