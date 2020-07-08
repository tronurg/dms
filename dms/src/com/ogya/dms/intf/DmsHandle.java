package com.ogya.dms.intf;

import javax.swing.JComponent;

import com.ogya.dms.intf.listeners.DmsListener;

public interface DmsHandle {

	JComponent getDmsPanel();

	void addListener(DmsListener listener);

	void removeListener(DmsListener listener);

}
