package com.ogya.dms.intf;

import com.ogya.dms.intf.exceptions.DbException;

public interface DmsService {

	DmsHandle login(String kullaniciAdi, String kullaniciSifresi) throws DbException;

}
