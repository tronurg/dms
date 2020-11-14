package com.ogya.dms.main;

import com.ogya.dms.control.DmsControl;
import com.ogya.dms.intf.DmsHandle;
import com.ogya.dms.intf.exceptions.DbException;

public class DmsCore {

	public static DmsHandle login(String username, String password) throws DbException {

		return new DmsControl(username, password);

	}

}
