package com.dms.core.intf.handles;

import java.util.List;

public interface ListHandle {

	Integer getListCode();

	<T> List<T> getList(Class<T> elementType);

}
