package com.ogya.dms.intf.handles;

import java.util.List;

import javax.swing.JComponent;

public interface ContactSelectionHandle {

	JComponent getContactSelectionPanel();

	List<String> getSelectedContactUuids();

	void resetSelection();

}
