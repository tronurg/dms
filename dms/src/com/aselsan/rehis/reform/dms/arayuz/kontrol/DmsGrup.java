package com.aselsan.rehis.reform.dms.arayuz.kontrol;

import java.util.List;

public interface DmsGrup {

	Long getGrupId();

	String getIsim();

	String getAciklama();

	List<Long> getKisiIdler();

}
