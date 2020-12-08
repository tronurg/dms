package com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme;

import java.util.function.Predicate;

import javax.swing.JComponent;

import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsGrup;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsGrupSecim;
import com.ogya.dms.intf.handles.GroupSelectionHandle;

public class DmsAktifGruplarGercekleme implements DmsGrupSecim {

	private final GroupSelectionHandle groupSelectionHandle;

	public DmsAktifGruplarGercekleme(GroupSelectionHandle groupSelectionHandle) {

		this.groupSelectionHandle = groupSelectionHandle;

	}

	@Override
	public JComponent getGrupSecimPanel() {
		return groupSelectionHandle.getGroupSelectionPanel();
	}

	@Override
	public JComponent getGrupSecimPanel(Predicate<DmsGrup> filtre) {
		return groupSelectionHandle
				.getGroupSelectionPanel(groupHandle -> filtre.test(new DmsGrupGercekleme(groupHandle)));
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
