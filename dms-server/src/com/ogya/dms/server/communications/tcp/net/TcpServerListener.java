package com.ogya.dms.server.communications.tcp.net;

import java.nio.file.Path;

public interface TcpServerListener {

	void connected(int id);

	void disconnected(int id);

	void messageReceived(int id, byte[] message);

	void fileReceived(int id, Path path);

}
