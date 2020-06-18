package com.ogya.dms.server.model.intf;

import java.util.List;

public interface ModelListener {

	void sendToLocalUser(String receiverUuid, String message);

	void sendToRemoteUser(String receiverUuid, String message);

	void sendToRemoteUsers(List<String> receiverUuids, String message);

	void sendToAllRemoteUsers(String message);

	void publishUuid(String uuid);

}
