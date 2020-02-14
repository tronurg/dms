package com.aselsan.rehis.reform.mcsy.veritabani;

import java.io.File;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Grup;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;

public class VeritabaniYonetici {

	public VeritabaniYonetici(String veritabaniAdi) {

		SessionFactory factory = new Configuration().configure(new File("./plugins/hibernate.cfg/mcsy.cfg.xml"))
				.setProperty("hibernate.connection.url", "jdbc:h2:./h2/" + veritabaniAdi).addAnnotatedClass(Grup.class)
				.addAnnotatedClass(Kisi.class).addAnnotatedClass(Mesaj.class).buildSessionFactory();

		factory.close();

	}

}
