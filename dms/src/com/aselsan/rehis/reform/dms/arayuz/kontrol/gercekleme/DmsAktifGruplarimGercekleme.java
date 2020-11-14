package com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme;

import javax.swing.JComponent;

import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsGrupSecim;
import com.ogya.dms.intf.handles.GroupSelectionHandle;

public class DmsAktifGruplarimGercekleme implements DmsGrupSecim {

	private final GroupSelectionHandle groupSelectionHandle;

	public DmsAktifGruplarimGercekleme(GroupSelectionHandle groupSelectionHandle) {

		this.groupSelectionHandle = groupSelectionHandle;

	}

	@Override
	public JComponent getGrupSecimPanel() {
		return groupSelectionHandle.getGroupSelectionPanel();
	}

	@Override
	public Long getSeciliGrupId() {
		return groupSelectionHandle.getSelectedGroupId();
	}

	@Override
	public void secimiSifirla() {
		groupSelectionHandle.resetSelection();
	}

}
