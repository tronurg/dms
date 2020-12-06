package com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme;

import java.util.List;

import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsGrup;
import com.aselsan.rehis.reform.dms.arayuz.veriyapisi.DmsDurum;
import com.ogya.dms.intf.handles.GroupHandle;

public class DmsGrupGercekleme implements DmsGrup {

	private final GroupHandle groupHandle;

	public DmsGrupGercekleme(GroupHandle groupHandle) {

		this.groupHandle = groupHandle;

	}

	@Override
	public Long getGrupId() {
		return groupHandle.getGroupId();
	}

	@Override
	public Long getGrupRefId() {
		return groupHandle.getGroupRefId();
	}

	@Override
	public Long getKurucuId() {
		return groupHandle.getOwnerId();
	}

	@Override
	public String getIsim() {
		return groupHandle.getName();
	}

	@Override
	public String getAciklama() {
		return groupHandle.getComment();
	}

	@Override
	public List<Long> getKisiIdler() {
		return groupHandle.getContactIds();
	}

	@Override
	public DmsDurum getDurum() {
		return DmsDurum.values()[groupHandle.getAvailability().ordinal()];
	}

}
