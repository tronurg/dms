package com.aselsan.rehis.reform.mcsy.test;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.aselsan.rehis.reform.mcsy.arayuz.McServisi;
import com.aselsan.rehis.reform.mcsy.arayuz.exceptions.VeritabaniHatasi;

@Component(immediate = true)
public class TestMcsy {

	private McServisi mcServisi;

	@Activate
	protected void activate() {

		try {
			mcServisi.getMcPanel("onur");
		} catch (VeritabaniHatasi e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Reference
	protected void addMcServisi(McServisi mcServisi) {

		this.mcServisi = mcServisi;

	}

	protected void removeMcServisi(McServisi mcServisi) {

		this.mcServisi = null;

	}

}
