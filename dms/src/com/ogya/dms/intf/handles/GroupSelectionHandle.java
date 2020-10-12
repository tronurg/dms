package com.ogya.dms.intf.handles;

import javax.swing.JComponent;

public interface GroupSelectionHandle {

	JComponent getGroupSelectionPanel();

	String getSelectedGroupUuid();

	void resetSelection();

}
