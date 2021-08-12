package com.ogya.dms.server.communications.tcp.net;

public interface TcpClientListener {

	void connected(TcpConnection tcpConnection);

	void couldNotConnect();

	void disconnected();

	void messageReceived(int messageNumber, byte[] message);

}
