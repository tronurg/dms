package com.dms.server.communications.tcp.net;

public interface TcpServerListener {

	void connected(int id, TcpConnection tcpConnection);

	void disconnected(int id);

	void messageReceived(int id, int messageNumber, byte[] message);

}
