package com.aselsan.rehis.reform.mcsy.main;

import javax.swing.JPanel;

import org.osgi.service.component.annotations.Component;

import com.aselsan.rehis.reform.mcsy.arayuz.McServisi;
import com.aselsan.rehis.reform.mcsy.arayuz.exceptions.VeritabaniHatasi;
import com.aselsan.rehis.reform.mcsy.kontrol.Kontrol;

@Component
public class Mcsy implements McServisi {

	@Override
	public JPanel getMcPanel(String kullaniciAdi) throws VeritabaniHatasi {

		Kontrol.getInstance(kullaniciAdi);

		return null;

	}

}
