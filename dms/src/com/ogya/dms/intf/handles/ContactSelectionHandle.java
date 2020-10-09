package com.ogya.dms.intf.handles;

import java.util.List;

import javax.swing.JPanel;

public interface ContactSelectionHandle {

	JPanel getContactSelectionPane();

	List<String> getSelectedContactUuids();

}
