package com.ogya.dms.intf.handles;

import java.util.function.Predicate;

import javax.swing.JComponent;

public interface GroupSelectionHandle {

	JComponent getGroupSelectionPanel();

	JComponent getGroupSelectionPanel(Predicate<GroupHandle> filter);

	Long getSelectedGroupId();

	void resetSelection();

}
