package com.ogya.dms.server.model.intf;

import com.ogya.dms.server.structures.Chunk;

public interface ModelListener {

	void sendToLocalUsers(Chunk chunk);

	void sendToRemoteServer(Chunk chunk, String dmsUuid);

	void publishImmediately();

}
