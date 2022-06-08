package com.ogya.dms.server.model.intf;

import com.ogya.dms.server.structures.LocalChunk;
import com.ogya.dms.server.structures.RemoteChunk;

public interface ModelListener {

	void sendToLocalUsers(LocalChunk chunk);

	void sendToRemoteServer(RemoteChunk chunk, String dmsUuid);

	void publishImmediately();

}
