package com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme;

import java.util.List;

import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsListe;
import com.ogya.dms.intf.handles.ListHandle;

public class DmsListeGercekleme implements DmsListe {

	private final ListHandle listHandle;

	public DmsListeGercekleme(ListHandle listHandle) {

		this.listHandle = listHandle;

	}

	@Override
	public Integer getListeKodu() {
		return listHandle.getListCode();
	}

	@Override
	public <T> List<T> getListe(Class<T> elemanTipi) {
		return listHandle.getList(elemanTipi);
	}

	@Override
	public Long getKisiId() {
		return listHandle.getContactId();
	}

	@Override
	public Long getGrupId() {
		return listHandle.getGroupId();
	}

}
