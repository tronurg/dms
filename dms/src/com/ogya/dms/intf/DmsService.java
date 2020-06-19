package com.ogya.dms.intf;

import com.ogya.dms.intf.exceptions.DbException;

public interface DmsService {

	DmsHandle login(String username, String password) throws DbException;

}
