package com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme;

import java.util.List;

import javax.swing.JComponent;

import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsKisiSecim;
import com.ogya.dms.intf.handles.ContactSelectionHandle;

public class DmsCevrimiciKisilerGercekleme implements DmsKisiSecim {

	private final ContactSelectionHandle contactSelectionHandle;

	public DmsCevrimiciKisilerGercekleme(ContactSelectionHandle contactSelectionHandle) {

		this.contactSelectionHandle = contactSelectionHandle;

	}

	@Override
	public JComponent getKisiSecimPanel() {
		return contactSelectionHandle.getContactSelectionPanel();
	}

	@Override
	public List<Long> getSeciliKisiIdler() {
		return contactSelectionHandle.getSelectedContactIds();
	}

	@Override
	public void secimiSifirla() {
		contactSelectionHandle.resetSelection();
	}

}
