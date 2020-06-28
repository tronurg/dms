package com.ogya.dms.structures;

import java.util.HashMap;
import java.util.Map;

public class StatusReport {

	public final Map<String, MessageStatus> uuidStatus = new HashMap<String, MessageStatus>();

	public MessageStatus getOverallStatus() {

		int minOrder = 0;

		for (MessageStatus messageStatus : uuidStatus.values()) {

			minOrder = Math.min(minOrder, messageStatus.getLogicalOrder());

		}

		return MessageStatus.getMessageStatusByLogicalOrder(minOrder);

	}

}
