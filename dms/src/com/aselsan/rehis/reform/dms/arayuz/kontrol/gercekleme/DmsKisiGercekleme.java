package com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme;

import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsKisi;
import com.ogya.dms.intf.handles.ContactHandle;

public class DmsKisiGercekleme implements DmsKisi {

	private final ContactHandle contactHandle;

	public DmsKisiGercekleme(ContactHandle contactHandle) {

		this.contactHandle = contactHandle;

	}

	@Override
	public Long getId() {
		return contactHandle.getId();
	}

	@Override
	public String getIsim() {
		return contactHandle.getName();
	}

	@Override
	public String getAciklama() {
		return contactHandle.getComment();
	}

	@Override
	public Double getEnlem() {
		return contactHandle.getLattitude();
	}

	@Override
	public Double getBoylam() {
		return contactHandle.getLongitude();
	}

	@Override
	public boolean isCevrimici() {
		return contactHandle.isOnline();
	}

}
