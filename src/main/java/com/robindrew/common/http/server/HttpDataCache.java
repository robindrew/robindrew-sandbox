package com.robindrew.common.http.server;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpDataCache {

	private static final Logger log = LoggerFactory.getLogger(HttpDataCache.class);

	private final List<HttpData> dataList = new ArrayList<>();

	public HttpData take() {
		HttpData data = null;
		synchronized (dataList) {
			if (!dataList.isEmpty()) {
				data = dataList.remove(dataList.size() - 1);
			}
		}
		if (data == null) {
			data = new HttpData();
			log.info("New Data");
		}
		data.reset();
		return data;
	}

	public void release(HttpData data) {
		synchronized (dataList) {
			dataList.add(data);
		}
	}

}
