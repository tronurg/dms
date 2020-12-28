package com.ogya.dms.core.main;

import com.ogya.dms.core.control.DmsControl;
import com.ogya.dms.core.intf.DmsHandle;
import com.ogya.dms.core.intf.exceptions.DbException;

public class DmsCore {

	public static DmsHandle login(String username, String password) throws DbException {

		return new DmsControl(username, password);

	}

}
