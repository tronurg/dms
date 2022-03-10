package com.ogya.dms.core.intf.listeners;

import java.nio.file.Path;

public interface DmsDownloadListener {

	void fileServerNotFound(Long contactId);

	void fileNotFound(Long contactId, Integer fileId);

	void downloadingFile(Long contactId, Integer fileId, int progress);

	void fileDownloaded(Long contactId, Integer fileId, Path path);

	void downloadFailed(Long contactId, Integer fileId);

}
