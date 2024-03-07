package com.ogya.dms.core.intf.handles;

import java.util.function.Predicate;

import javax.swing.JComponent;

import com.ogya.dms.core.intf.tools.GroupId;

public interface GroupSelectionHandle {

	JComponent getGroupSelectionPanel();

	JComponent getGroupSelectionPanel(Predicate<GroupHandle> filter);

	GroupId getSelectedGroupId();

	void resetSelection();

}
