package com.aselsan.rehis.reform.dms.arayuz.kontrol;

import java.util.List;

public interface DmsListe {

	Integer getListeKodu();

	<T> List<T> getListe(Class<T> elemanTipi);

	Long getKisiId();

	Long getGrupId();

}
