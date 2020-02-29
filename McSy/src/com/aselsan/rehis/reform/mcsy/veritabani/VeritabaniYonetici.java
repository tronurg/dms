package com.aselsan.rehis.reform.mcsy.veritabani;

import java.io.File;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import com.aselsan.rehis.reform.mcsy.arayuz.exceptions.VeritabaniHatasi;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Grup;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;

public class VeritabaniYonetici {

	private final String isim;

	private final SessionFactory factory;

	public VeritabaniYonetici(String veritabaniAdi) throws VeritabaniHatasi {

		isim = veritabaniAdi;

		try {

			factory = new Configuration().configure(new File("./plugins/hibernate.cfg/mcsy.cfg.xml"))
					.setProperty("hibernate.connection.url", "jdbc:h2:./h2/" + veritabaniAdi)
					.addAnnotatedClass(Grup.class).addAnnotatedClass(Kimlik.class).addAnnotatedClass(Kisi.class)
					.addAnnotatedClass(Mesaj.class).buildSessionFactory();

		} catch (HibernateException e) {

			throw new VeritabaniHatasi("Veritabanina erisilemiyor. Hesap kullanimda olabilir.");

		}

	}

	public Kimlik getKimlik() throws VeritabaniHatasi {

		if (factory.isClosed())
			throw new VeritabaniHatasi("Veritabani baglantisi sonlandirilmis.");

		Session session = factory.openSession();

		Query<Kimlik> queryKimlik = session.createQuery("from Kimlik where isim='" + isim + "'", Kimlik.class);

		if (queryKimlik.getResultList().size() == 0) {

			session.beginTransaction();

			session.persist(new Kimlik(isim));

			session.getTransaction().commit();

		}

		Kimlik kimlik = queryKimlik.getResultList().get(0);

		session.close();

		return kimlik;

	}

	public List<Kisi> tumKisileriAl() throws VeritabaniHatasi {

		if (factory.isClosed())
			throw new VeritabaniHatasi("Veritabani baglantisi sonlandirilmis.");

		Session session = factory.openSession();

		Query<Kisi> queryKimlik = session.createQuery("from Kisi", Kisi.class);

		List<Kisi> tumKisiler = queryKimlik.getResultList();

		session.close();

		return tumKisiler;

	}

	public List<Grup> tumGruplariAl() throws VeritabaniHatasi {

		if (factory.isClosed())
			throw new VeritabaniHatasi("Veritabani baglantisi sonlandirilmis.");

		Session session = factory.openSession();

		Query<Grup> queryKimlik = session.createQuery("from Grup", Grup.class);

		List<Grup> tumGruplar = queryKimlik.getResultList();

		session.close();

		return tumGruplar;

	}

	public void kisiEkleGuncelle(Kisi kisi) {

	}

	public void grupEkleGuncelle(Grup grup) {

	}

	public void kapat() {

		factory.close();

	}

}
