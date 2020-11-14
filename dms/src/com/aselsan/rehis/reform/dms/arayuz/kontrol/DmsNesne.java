package com.aselsan.rehis.reform.dms.arayuz.kontrol;

public interface DmsNesne {

	Integer getNesneKodu();

	<T> T getNesne(Class<T> nesneSinifi);

	Long getKisiId();

	Long getGrupId();

}
