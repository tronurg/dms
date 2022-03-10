package com.ogya.dms.core.intf.listeners;

import java.nio.file.Path;

public interface DmsFileServer {

	Path fileRequested(Integer fileId);

}
