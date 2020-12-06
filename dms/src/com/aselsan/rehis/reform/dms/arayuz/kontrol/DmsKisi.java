package com.aselsan.rehis.reform.dms.arayuz.kontrol;

import com.aselsan.rehis.reform.dms.arayuz.veriyapisi.DmsDurum;

public interface DmsKisi {

	Long getId();

	String getUuid();

	String getIsim();

	String getAciklama();

	Double getEnlem();

	Double getBoylam();

	DmsDurum getDurum();

}
