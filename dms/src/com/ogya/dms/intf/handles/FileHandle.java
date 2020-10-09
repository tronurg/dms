package com.ogya.dms.intf.handles;

import java.nio.file.Path;

public interface FileHandle {

	Integer getFileCode();

	Path getPath();

	String getSenderUuid();

	String getGroupUuid();

}
