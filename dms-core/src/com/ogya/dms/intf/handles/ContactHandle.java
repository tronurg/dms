package com.ogya.dms.intf.handles;

public interface ContactHandle {

	Long getId();

	String getName();

	String getComment();

	Double getLattitude();

	Double getLongitude();

	boolean isOnline();

}
