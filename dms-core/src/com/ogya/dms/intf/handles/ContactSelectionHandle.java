package com.ogya.dms.intf.handles;

import java.util.List;
import java.util.function.Predicate;

import javax.swing.JComponent;

public interface ContactSelectionHandle {

	JComponent getContactSelectionPanel();

	JComponent getContactSelectionPanel(Predicate<ContactHandle> filter);

	List<Long> getSelectedContactIds();

	void resetSelection();

}
