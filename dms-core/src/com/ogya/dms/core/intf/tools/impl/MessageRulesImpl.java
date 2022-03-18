package com.ogya.dms.core.intf.tools.impl;

import java.net.InetAddress;

import com.ogya.dms.core.intf.tools.MessageRules;

public class MessageRulesImpl implements MessageRules {

	private Long timeout;
	private InetAddress localInterface;

	public MessageRulesImpl() {

		super();

	}

	public Long getTimeout() {
		return timeout;
	}

	public InetAddress getLocalInterface() {
		return localInterface;
	}

	@Override
	public MessageRules useTimeout(Long timeout) {

		this.timeout = timeout;

		return this;

	}

	@Override
	public MessageRules useLocalInterface(InetAddress localInterface) {

		this.localInterface = localInterface;

		return this;

	}

}
