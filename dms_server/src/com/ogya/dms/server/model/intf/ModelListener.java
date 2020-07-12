package com.ogya.dms.server.model.intf;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ModelListener {

	void sendToLocalUser(String receiverUuid, String message);

	void sendToRemoteUser(String receiverUuid, String message, Consumer<Integer> progressMethod);

	void sendToRemoteUsers(List<String> receiverUuids, String message,
			BiConsumer<List<String>, Integer> progressMethod);

	void sendToAllRemoteUsers(String message);

	void publishUuid(String uuid);

}
