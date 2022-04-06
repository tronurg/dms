package com.ogya.dms.server.common;

import com.ogya.dms.commons.structures.ContentType;

public class MessageSorterLocal extends MessageSorter {

	@Override
	public int compare(MessageContainerBase m1, MessageContainerBase m2) {
		int result = Boolean.compare(m1.contentType != ContentType.UPLOAD, m2.contentType != ContentType.UPLOAD);
		if (result == 0) {
			result = super.compare(m1, m2);
		}
		return result;
	}

}
