package com.aselsan.rehis.reform.mcsy.arayuz;

import javax.swing.JComponent;

import com.aselsan.rehis.reform.mcsy.arayuz.exceptions.VeritabaniHatasi;

public interface McServisi {

	JComponent getMcPanel(String kullaniciAdi) throws VeritabaniHatasi;

}
