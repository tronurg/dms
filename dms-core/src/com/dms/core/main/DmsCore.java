package com.dms.core.main;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.dms.core.control.DmsControl;
import com.dms.core.intf.DmsException;
import com.dms.core.intf.DmsHandle;

public class DmsCore {

	private static final Map<String, DmsControl> INSTANCES = Collections
			.synchronizedMap(new HashMap<String, DmsControl>());

	public static DmsHandle login(String username, String password) throws DmsException {

		DmsControl dmsControl = INSTANCES.get(username);
		if (dmsControl == null) {
			try {
				dmsControl = new DmsControl(username, password, () -> INSTANCES.remove(username));
				INSTANCES.put(username, dmsControl);
			} catch (Exception e) {
				throw new DmsException(e);
			}
		}
		return dmsControl;

	}

}
