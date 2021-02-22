package com.ogya.dms.server.communications.tcp.net;

public interface TcpClientListener {

	void connected();

	void couldNotConnect();

	void disconnected();

	void messageReceived(byte[] message);

}
