package com.ogya.dms.intf.handles;

import javax.swing.JComponent;

public interface GroupSelectionHandle {

	JComponent getGroupSelectionPanel();

	Long getSelectedGroupId();

	void resetSelection();

}
