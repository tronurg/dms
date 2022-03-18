package com.ogya.dms.core.intf.tools;

import java.net.InetAddress;

public interface MessageRules {

	MessageRules useTimeout(Long timeout);

	MessageRules useLocalInterface(InetAddress localInterface);

}
