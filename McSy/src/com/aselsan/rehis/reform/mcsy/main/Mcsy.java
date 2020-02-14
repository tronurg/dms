package com.aselsan.rehis.reform.mcsy.main;

import javax.swing.JPanel;

import org.osgi.service.component.annotations.Component;

import com.aselsan.rehis.reform.mcsy.arayuz.McServisi;
import com.aselsan.rehis.reform.mcsy.veritabani.VeritabaniYonetici;

@Component
public class Mcsy implements McServisi {

	@Override
	public JPanel getMcPanel(String kullaniciAdi) {

		System.out.println("Mcsy.getMcPanel()");

		new VeritabaniYonetici(kullaniciAdi);

		return null;

	}

}
