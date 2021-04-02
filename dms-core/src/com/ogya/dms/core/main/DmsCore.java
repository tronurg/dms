package com.ogya.dms.core.main;

import com.ogya.dms.core.control.DmsControl;
import com.ogya.dms.core.intf.DmsException;
import com.ogya.dms.core.intf.DmsHandle;

public class DmsCore {

	public static DmsHandle login(String username, String password) throws DmsException {

		try {
			return new DmsControl(username, password);
		} catch (Exception e) {
			throw new DmsException(e);
		}

	}

}
