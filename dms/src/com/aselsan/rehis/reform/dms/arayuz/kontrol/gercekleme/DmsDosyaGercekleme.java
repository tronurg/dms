package com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme;

import java.nio.file.Path;

import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsDosya;
import com.ogya.dms.intf.handles.FileHandle;

public class DmsDosyaGercekleme implements DmsDosya {

	private final FileHandle fileHandle;

	public DmsDosyaGercekleme(FileHandle fileHandle) {

		this.fileHandle = fileHandle;

	}

	@Override
	public Integer getDosyaKodu() {
		return fileHandle.getFileCode();
	}

	@Override
	public Path getDosyaYolu() {
		return fileHandle.getPath();
	}

	@Override
	public Long getKisiId() {
		return fileHandle.getContactId();
	}

	@Override
	public Long getGrupId() {
		return fileHandle.getGroupId();
	}

}
