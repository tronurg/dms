package com.ogya.dms.core.main;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ogya.dms.core.control.DmsControl;
import com.ogya.dms.core.intf.DmsException;
import com.ogya.dms.core.intf.DmsHandle;

public class DmsCore {

	private static final Map<String, DmsControl> INSTANCES = Collections
			.synchronizedMap(new HashMap<String, DmsControl>());

	public static DmsHandle login(String username, String password) throws DmsException {

		DmsControl dmsControl = INSTANCES.get(username);
		if (dmsControl == null) {
			try {
				dmsControl = new DmsControl(username, password);
				INSTANCES.put(username, dmsControl);
			} catch (Exception e) {
				throw new DmsException(e);
			}
		}
		return dmsControl;

	}

	public static void logout(String username) {

		DmsControl dmsControl = INSTANCES.remove(username);
		if (dmsControl == null) {
			return;
		}
		dmsControl.close();

	}

}
