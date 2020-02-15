package com.aselsan.rehis.reform.mcsy.arayuz;

import javax.swing.JPanel;

import com.aselsan.rehis.reform.mcsy.arayuz.exceptions.VeritabaniHatasi;

public interface McServisi {

	JPanel getMcPanel(String kullaniciAdi) throws VeritabaniHatasi;

}
