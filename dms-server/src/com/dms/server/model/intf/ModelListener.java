package com.dms.server.model.intf;

import com.dms.server.structures.LocalChunk;
import com.dms.server.structures.RemoteChunk;

public interface ModelListener {

	void sendToLocalUsers(LocalChunk chunk);

	void sendToRemoteServer(RemoteChunk chunk, String dmsUuid);

	void publishImmediately();

}
