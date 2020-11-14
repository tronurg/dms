package com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme;

import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsNesne;
import com.ogya.dms.intf.handles.ObjectHandle;

public class DmsNesneGercekleme implements DmsNesne {

	private final ObjectHandle objectHandle;

	public DmsNesneGercekleme(ObjectHandle objectHandle) {

		this.objectHandle = objectHandle;

	}

	@Override
	public Integer getNesneKodu() {
		return objectHandle.getObjectCode();
	}

	@Override
	public <T> T getNesne(Class<T> nesneSinifi) {
		return objectHandle.getObject(nesneSinifi);
	}

	@Override
	public Long getKisiId() {
		return objectHandle.getContactId();
	}

	@Override
	public Long getGrupId() {
		return objectHandle.getGroupId();
	}

}
