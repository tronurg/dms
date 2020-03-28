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

		Kimlik kimlik = session.createQuery("from Kimlik where isim like :isim", Kimlik.class)
				.setParameter("isim", isim).uniqueResult();

		if (kimlik == null) {

			kimlik = new Kimlik(isim);

			session.beginTransaction();

			session.persist(kimlik);

			session.getTransaction().commit();

		}

		session.close();

		return kimlik;

	}

	public List<Kisi> tumKisileriAl() throws HibernateException {

		Session session = factory.openSession();

		Query<Kisi> queryKisi = session.createQuery("from Kisi", Kisi.class);

		List<Kisi> tumKisiler = queryKisi.list();

		session.close();

		return tumKisiler;

	}

	public List<Grup> tumGruplariAl() throws HibernateException {

		Session session = factory.openSession();

		Query<Grup> queryGrup = session.createQuery("from Grup", Grup.class);

		List<Grup> tumGruplar = queryGrup.list();

		session.close();

		return tumGruplar;

	}

	public List<Mesaj> tumMesajlariAl() throws HibernateException {

		Session session = factory.openSession();

		Query<Mesaj> queryMesaj = session.createQuery("from Mesaj", Mesaj.class);

		List<Mesaj> tumMesajlar = queryMesaj.list();

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

	public Kisi kisiEkleGuncelle(Kisi kisi) throws HibernateException {

		Session session = factory.openSession();

		Kisi vtKisi = session.createQuery("from Kisi where uuid like :uuid", Kisi.class)
				.setParameter("uuid", kisi.getUuid()).uniqueResult();

		if (vtKisi == null) {

			vtKisi = kisi;

			vtKisi.setId(null);

			session.beginTransaction();

			session.persist(vtKisi);

			session.getTransaction().commit();

		} else {

			kisi.setId(vtKisi.getId());

			session.beginTransaction();

			vtKisi = (Kisi) session.merge(kisi);

			session.getTransaction().commit();

		}

		session.close();

		return vtKisi;

	}

	public Grup grupEkleGuncelle(Grup grup) throws HibernateException {

		Session session = factory.openSession();

		Grup vtGrup = session.createQuery("from Grup where uuid like :uuid", Grup.class)
				.setParameter("uuid", grup.getUuid()).uniqueResult();

		if (vtGrup == null) {

			vtGrup = grup;

			vtGrup.setId(null);

			session.beginTransaction();

			session.persist(vtGrup);

			session.getTransaction().commit();

		} else {

			grup.setId(vtGrup.getId());

			session.beginTransaction();

			vtGrup = (Grup) session.merge(grup);

			session.getTransaction().commit();

		}

		session.close();

		return vtGrup;

	}

	public Mesaj mesajEkleGuncelle(Mesaj mesaj) throws HibernateException {

		Session session = factory.openSession();

		Mesaj vtMesaj = session.createQuery(
				"from Mesaj where aliciUuid like :aliciUuid and gonderenUuid like :gonderenUuid and mesajId=:mesajId",
				Mesaj.class).setParameter("aliciUuid", mesaj.getAliciUuid())
				.setParameter("gonderenUuid", mesaj.getGonderenUuid()).setParameter("mesajId", mesaj.getMesajId())
				.uniqueResult();

		if (vtMesaj == null) {

			vtMesaj = mesaj;

			vtMesaj.setId(null);

			session.beginTransaction();

			session.persist(vtMesaj);

			session.getTransaction().commit();

		} else {

			mesaj.setId(vtMesaj.getId());

			session.beginTransaction();

			vtMesaj = (Mesaj) session.merge(mesaj);

			session.getTransaction().commit();

		}

		session.close();

		return vtMesaj;

	}

//	public Kisi kisiGuncelle(Kisi kisi) throws HibernateException {
//
//		Session session = factory.openSession();
//
//		session.beginTransaction();
//
//		Kisi yeniKisi = (Kisi) session.merge(kisi);
//
//		session.getTransaction().commit();
//
//		session.close();
//
//		return yeniKisi;
//
//	}
//
//	public void grupEkle(Grup grup) throws HibernateException {
//
//		Session session = factory.openSession();
//
//		session.beginTransaction();
//
//		session.persist(grup);
//
//		session.getTransaction().commit();
//
//		session.close();
//
//	}
//
//	public Grup grupGuncelle(Grup grup) throws HibernateException {
//
//		Session session = factory.openSession();
//
//		session.beginTransaction();
//
//		Grup yeniGrup = (Grup) session.merge(grup);
//
//		session.getTransaction().commit();
//
//		session.close();
//
//		return yeniGrup;
//
//	}
//
//	public void mesajEkle(Mesaj mesaj) throws HibernateException {
//
//		Session session = factory.openSession();
//
//		session.beginTransaction();
//
//		session.persist(mesaj);
//
//		session.getTransaction().commit();
//
//		session.close();
//
//	}
//
//	public Mesaj mesajGuncelle(Mesaj mesaj) throws HibernateException {
//
//		Session session = factory.openSession();
//
//		session.beginTransaction();
//
//		Mesaj yeniMesaj = (Mesaj) session.merge(mesaj);
//
//		session.getTransaction().commit();
//
//		session.close();
//
//		return yeniMesaj;
//
//	}

}
