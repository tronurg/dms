package com.dms.core.intf.handles;

import java.util.List;
import java.util.function.Predicate;

import javax.swing.JComponent;

import com.dms.core.intf.tools.ContactId;

public interface ContactSelectionHandle {

	JComponent getContactSelectionPanel();

	JComponent getContactSelectionPanel(Predicate<ContactHandle> filter);

	List<ContactId> getSelectedContactIds();

	void resetSelection();

}
