package com.dms.core.intf.listeners;

import java.nio.file.Path;

public interface DmsFileServer {

	Path fileRequested(Long fileId);

}
