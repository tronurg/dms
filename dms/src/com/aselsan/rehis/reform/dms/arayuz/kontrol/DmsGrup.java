package com.aselsan.rehis.reform.dms.arayuz.kontrol;

import java.util.List;

import com.aselsan.rehis.reform.dms.arayuz.veriyapisi.DmsDurum;

public interface DmsGrup {

	Long getGrupId();

	Long getGrupRefId();

	Long getKurucuId();

	String getIsim();

	String getAciklama();

	List<Long> getKisiIdler();

	DmsDurum getDurum();

}
