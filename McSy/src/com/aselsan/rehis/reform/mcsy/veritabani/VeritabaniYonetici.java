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

			Runtime.getRuntime().addShutdownHook(new Thread(() -> factory.close()));

		} catch (HibernateException e) {

			throw new VeritabaniHatasi("Veritabanina erisilemiyor. Hesap kullanimda olabilir.");

		}

	}

	public Kimlik getKimlik() throws HibernateException {

		Session session = factory.openSession();

		Query<Kimlik> queryKimlik = session.createQuery("from Kimlik where isim='" + isim + "'", Kimlik.class);

		if (queryKimlik.list().size() == 0) {

			session.beginTransaction();

			session.persist(new Kimlik(isim));

			session.getTransaction().commit();

		}

		Kimlik kimlik = queryKimlik.list().get(0);

		session.close();

		return kimlik;

	}

	public List<Kisi> tumKisileriAl() throws HibernateException {

		Session session = factory.openSession();

		Query<Kisi> queryKisi = session.createQuery("from Kisi", Kisi.class);

		List<Kisi> tumKisiler = queryKisi.getResultList();

		session.close();

		return tumKisiler;

	}

	public List<Grup> tumGruplariAl() throws HibernateException {

		Session session = factory.openSession();

		Query<Grup> queryGrup = session.createQuery("from Grup", Grup.class);

		List<Grup> tumGruplar = queryGrup.getResultList();

		session.close();

		return tumGruplar;

	}

	public List<Mesaj> tumMesajlariAl() throws HibernateException {

		Session session = factory.openSession();

		Query<Mesaj> queryMesaj = session.createQuery("from Mesaj", Mesaj.class);

		List<Mesaj> tumMesajlar = queryMesaj.getResultList();

		session.close();

		return tumMesajlar;

	}

	public Kimlik kimlikGuncelle(Kimlik kimlik) throws HibernateException {

		Session session = factory.openSession();

		session.beginTransaction();

		Kimlik yeniKimlik = (Kimlik) session.merge(kimlik);

		session.getTransaction().commit();

		session.close();

		return yeniKimlik;

	}

	public void kisiEkle(Kisi kisi) throws HibernateException {

		Session session = factory.openSession();

		session.beginTransaction();

		session.persist(kisi);

		session.getTransaction().commit();

		session.close();

	}

	public Kisi kisiGuncelle(Kisi kisi) throws HibernateException {

		Session session = factory.openSession();

		session.beginTransaction();

		Kisi yeniKisi = (Kisi) session.merge(kisi);

		session.getTransaction().commit();

		session.close();

		return yeniKisi;

	}

	public void grupEkle(Grup grup) throws HibernateException {

		Session session = factory.openSession();

		session.beginTransaction();

		session.persist(grup);

		session.getTransaction().commit();

		session.close();

	}

	public Grup grupGuncelle(Grup grup) throws HibernateException {

		Session session = factory.openSession();

		session.beginTransaction();

		Grup yeniGrup = (Grup) session.merge(grup);

		session.getTransaction().commit();

		session.close();

		return yeniGrup;

	}

	public void mesajEkle(Mesaj mesaj) throws HibernateException {

		Session session = factory.openSession();

		session.beginTransaction();

		session.persist(mesaj);

		session.getTransaction().commit();

		session.close();

	}

	public Mesaj mesajGuncelle(Mesaj mesaj) throws HibernateException {

		Session session = factory.openSession();

		session.beginTransaction();

		Mesaj yeniMesaj = (Mesaj) session.merge(mesaj);

		session.getTransaction().commit();

		session.close();

		return yeniMesaj;

	}

}
