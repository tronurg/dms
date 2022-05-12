package com.ogya.dms.server.model.intf;

import java.util.List;

public interface ModelListener {

	void sendToLocalUsers(int messageNumber, byte[] data, List<String> receiverUuids);

	void sendToRemoteServer(int messageNumber, byte[] data, String dmsUuid);

	void publishImmediately();

}
