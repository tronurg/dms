package com.ogya.dms.main;

import org.osgi.service.component.annotations.Component;

import com.ogya.dms.control.Control;
import com.ogya.dms.intf.DmsHandle;
import com.ogya.dms.intf.DmsService;
import com.ogya.dms.intf.exceptions.DbException;

@Component
public class Dms implements DmsService {

	@Override
	public DmsHandle login(String username, String password) throws DbException {

		return Control.getInstance(username, password);

	}

}
