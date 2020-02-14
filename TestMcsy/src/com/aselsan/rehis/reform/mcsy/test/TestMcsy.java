package com.aselsan.rehis.reform.mcsy.test;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.aselsan.rehis.reform.mcsy.arayuz.McServisi;

@Component(immediate = true)
public class TestMcsy {

	private McServisi mcServisi;

	@Activate
	protected void activate() {

		mcServisi.getMcPanel("onur");

	}

	@Reference
	protected void addMcServisi(McServisi mcServisi) {

		this.mcServisi = mcServisi;

	}

	protected void removeMcServisi(McServisi mcServisi) {

		this.mcServisi = null;

	}

}
