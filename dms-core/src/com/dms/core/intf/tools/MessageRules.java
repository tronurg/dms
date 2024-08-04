package com.dms.core.intf.tools;

import java.net.InetAddress;

public interface MessageRules {

	MessageRules useTrackingId(Long trackingId);

	MessageRules useTimeout(Long timeout);

	MessageRules useLocalInterface(InetAddress localInterface);

}
