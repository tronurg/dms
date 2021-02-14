package com.ogya.dms.server.communications.tcp.net;

import java.nio.file.Path;

public interface TcpClientListener {

	void connected();

	void couldNotConnect();

	void disconnected();

	void messageReceived(byte[] message);

	void fileReceived(Path path);

}
