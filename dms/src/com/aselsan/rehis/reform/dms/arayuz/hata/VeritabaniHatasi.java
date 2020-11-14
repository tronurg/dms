package com.aselsan.rehis.reform.dms.arayuz.hata;

import com.ogya.dms.intf.exceptions.DbException;

public class VeritabaniHatasi extends DbException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public VeritabaniHatasi(String errorMessage) {

		super(errorMessage);

	}

}
