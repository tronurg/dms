package com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme;

import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsMesaj;
import com.ogya.dms.intf.handles.MessageHandle;

public class DmsMesajGercekleme implements DmsMesaj {

	private final MessageHandle messageHandle;

	public DmsMesajGercekleme(MessageHandle messageHandle) {

		this.messageHandle = messageHandle;

	}

	@Override
	public Integer getMesajKodu() {
		return messageHandle.getMessageCode();
	}

	@Override
	public String getMesaj() {
		return messageHandle.getMessage();
	}

	@Override
	public Long getKisiId() {
		return messageHandle.getContactId();
	}

	@Override
	public Long getGrupId() {
		return messageHandle.getGroupId();
	}

}
