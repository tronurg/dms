package com.aselsan.rehis.reform.mcsy.ortak;

public class OrtakSabitler {

	public static final String SUNUCU_IP = OrtakMetotlar.getSunucuIp();
	public static final int SUNUCU_PORT = OrtakMetotlar.getSunucuPort();

	public static final Integer DURUM_MUSAIT = 0;
	public static final Integer DURUM_UZAKTA = 1;
	public static final Integer DURUM_MESGUL = 2;

	public static final Integer OZEL_MESAJ_KODU_METIN = 0;
	public static final Integer OZEL_MESAJ_KODU_NESNE = 1;
	public static final Integer OZEL_MESAJ_KODU_DOSYA = 2;
	public static final Integer OZEL_MESAJ_KODU_LISTE = 3;
	public static final Integer OZEL_MESAJ_KODU_SES = 4;
	public static final Integer OZEL_MESAJ_KODU_RAPOR = 5;

}
