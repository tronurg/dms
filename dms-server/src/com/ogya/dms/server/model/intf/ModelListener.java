package com.ogya.dms.server.model.intf;

import java.util.List;

import com.ogya.dms.server.common.SendMorePojo;

public interface ModelListener {

	void sendToLocalUsers(int messageNumber, byte[] data, List<String> receiverUuids, SendMorePojo sendMore);

	void sendToRemoteServer(int messageNumber, byte[] data, String dmsUuid);

	void publishImmediately();

}
