package com.ogya.dms.server.common;

import java.util.Comparator;

public class MessageSorter implements Comparator<MessageContainerBase> {

	@Override
	public int compare(MessageContainerBase m1, MessageContainerBase m2) {
		int result = Boolean.compare(m1.isSecondary(), m2.isSecondary());
		if (result == 0) {
			result = Long.compare(m1.checkInTime, m2.checkInTime);
		}
		if (result == 0) {
			result = Integer.compare(m1.messageNumber, m2.messageNumber);
		}
		return result;
	}

}
