package com.dms.core.intf.listeners;

import java.nio.file.Path;

public interface DmsDownloadListener {

	void fileServerNotFound(Long downloadId);

	void fileNotFound(Long downloadId);

	void downloadingFile(Long downloadId, int progress);

	void fileDownloaded(Long downloadId, Path path);

	void downloadPaused(Long downloadId);

	void downloadFailed(Long downloadId);

}
