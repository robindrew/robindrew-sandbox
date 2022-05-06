package com.robindrew.common.http.server;

import java.net.InetSocketAddress;

public class LightHttpServerConfig {

	private final InetSocketAddress bindAddress;
	private int connectionBuffer = 2000000;
	private int receiveBuffer = 5000000;
	private int handlerThreads = 20;
	private int eventThreads = 20;
	private boolean reuseAddress = true;
	private boolean blocking = false;

	public LightHttpServerConfig(String host, int port) {
		this(new InetSocketAddress(host, port));
	}

	public LightHttpServerConfig(InetSocketAddress bindAddress) {
		if (bindAddress == null) {
			throw new NullPointerException();
		}
		this.bindAddress = bindAddress;
	}

	public InetSocketAddress getBindAddress() {
		return bindAddress;
	}

	public int getEventThreads() {
		return eventThreads;
	}

	public void setEventThreads(int eventThreads) {
		this.eventThreads = eventThreads;
	}

	public int getHandlerThreads() {
		return handlerThreads;
	}

	public void setHandlerThreads(int handlerThreads) {
		this.handlerThreads = handlerThreads;
	}

	public int getConnectionBuffer() {
		return connectionBuffer;
	}

	public int getReceiveBuffer() {
		return receiveBuffer;
	}

	public boolean isReuseAddress() {
		return reuseAddress;
	}

	public void setConnectionBuffer(int connectionBuffer) {
		this.connectionBuffer = connectionBuffer;
	}

	public void setReceiveBuffer(int receiveBuffer) {
		this.receiveBuffer = receiveBuffer;
	}

	public void setReuseAddress(boolean reuseAddress) {
		this.reuseAddress = reuseAddress;
	}

	public boolean isBlocking() {
		return blocking;
	}

	public void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}

}
