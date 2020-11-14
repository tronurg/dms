package com.ogya.dms.test;

import java.io.IOException;
import java.nio.file.Path;

import com.aselsan.rehis.reform.dms.arayuz.DmsKontrol;
import com.aselsan.rehis.reform.dms.arayuz.dinleyici.DmsDinleyici;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsDosya;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsKisi;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsListe;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsMesaj;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsNesne;

public class DmsDinleyiciGercekleme implements DmsDinleyici {

	private final DmsKontrol dmsHandle;

	public DmsDinleyiciGercekleme(DmsKontrol dmsHandle) {

		this.dmsHandle = dmsHandle;

	}

	@Override
	public void dosyaTiklandi(Path dosyaYolu) {

		try {

			new ProcessBuilder().directory(dosyaYolu.getParent().toFile())
					.command("cmd", "/C", dosyaYolu.getFileName().toString()).start();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void mesajAlindi(DmsMesaj dmsMesaj) {

		Long contactId = dmsMesaj.getKisiId();
		Long groupId = dmsMesaj.getGrupId();

		System.out.println(String.format("Message received from: %s (group: %s)\nContent: %s\n",
				dmsHandle.getDmsKisi(contactId).getIsim(),
				groupId == null ? null : dmsHandle.getDmsGrup(groupId).getIsim(), dmsMesaj.getMesaj()));

	}

	@Override
	public void nesneAlindi(DmsNesne dmsNesne) {

		Long contactId = dmsNesne.getKisiId();
		Long groupId = dmsNesne.getGrupId();

		System.out.println(String.format("Object received from: %s (group: %s)\nContent: %s\n",
				dmsHandle.getDmsKisi(contactId).getIsim(),
				groupId == null ? null : dmsHandle.getDmsGrup(groupId).getIsim(), dmsNesne.getNesne(TestPojo.class)));

	}

	@Override
	public void listeAlindi(DmsListe dmsListe) {

		Long contactId = dmsListe.getKisiId();
		Long groupId = dmsListe.getGrupId();

		System.out.println(String.format("List received from: %s (group: %s)\nContent: %s\n",
				dmsHandle.getDmsKisi(contactId).getIsim(),
				groupId == null ? null : dmsHandle.getDmsGrup(groupId).getIsim(),
				dmsListe.getListe(TestPojoConverted.class)));

	}

	@Override
	public void dosyaAlindi(DmsDosya dmsDosya) {

		Long contactId = dmsDosya.getKisiId();
		Long groupId = dmsDosya.getGrupId();

		System.out.println(String.format("File received from: %s (group: %s)\nContent: %s\n",
				dmsHandle.getDmsKisi(contactId).getIsim(),
				groupId == null ? null : dmsHandle.getDmsGrup(groupId).getIsim(), dmsDosya.getDosyaYolu()));

		try {

			new ProcessBuilder().directory(dmsDosya.getDosyaYolu().getParent().toFile())
					.command("cmd", "/C", dmsDosya.getDosyaYolu().getFileName().toString()).start();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void kisiGuncellendi(DmsKisi dmsKisi) {

//		System.out.println(String.format("Contact updated: %s\n", dmsKisi.getIsim()));

	}

}
