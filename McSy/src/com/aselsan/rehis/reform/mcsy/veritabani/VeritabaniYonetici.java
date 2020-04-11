package com.aselsan.rehis.reform.mcsy.veritabani;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

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
import com.aselsan.rehis.reform.mcsy.veriyapilari.MesajDurumu;

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

		Mesaj vtMesaj = session
				.createQuery("from Mesaj where gonderenUuid like :gonderenUuid and mesajId=:mesajId", Mesaj.class)
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

	public Mesaj mesajDurumGuncelle(String gonderenUuid, long mesajId, MesajDurumu mesajDurumu)
			throws HibernateException {

		Session session = factory.openSession();

		Mesaj vtMesaj = session
				.createQuery("from Mesaj where gonderenUuid like :gonderenUuid and mesajId=:mesajId", Mesaj.class)
				.setParameter("gonderenUuid", gonderenUuid).setParameter("mesajId", mesajId).uniqueResult();

		if (vtMesaj != null) {

			vtMesaj.setMesajDurumu(mesajDurumu);

			session.beginTransaction();

			vtMesaj = (Mesaj) session.merge(vtMesaj);

			session.getTransaction().commit();

		}

		session.close();

		return vtMesaj;

	}

	public Mesaj getMesaj(String gonderenUuid, long mesajId) throws HibernateException {

		Session session = factory.openSession();

		Mesaj vtMesaj = session
				.createQuery("from Mesaj where gonderenUuid like :gonderenUuid and mesajId=:mesajId", Mesaj.class)
				.setParameter("gonderenUuid", gonderenUuid).setParameter("mesajId", mesajId).uniqueResult();

		session.close();

		return vtMesaj;

	}

	public List<Mesaj> getKisiyeGidenBekleyenMesajlar(String aliciUuid) throws HibernateException {

		Session session = factory.openSession();

		List<Mesaj> vtMesajlar = session.createQuery(
				"from Mesaj where aliciUuid like :aliciUuid and (mesajDurumu like :olusturuldu or mesajDurumu like :gonderildi or mesajDurumu like :ulasti)",
				Mesaj.class).setParameter("aliciUuid", aliciUuid).setParameter("olusturuldu", MesajDurumu.OLUSTURULDU)
				.setParameter("gonderildi", MesajDurumu.GONDERILDI).setParameter("ulasti", MesajDurumu.ULASTI).list();

		session.close();

		return vtMesajlar;

	}

	public List<Mesaj> getKisidenGelenBekleyenMesajlar(String gonderenUuid) throws HibernateException {

		Session session = factory.openSession();

		List<Mesaj> vtMesajlar = session
				.createQuery("from Mesaj where gonderenUuid like :gonderenUuid and mesajDurumu like :ulasti",
						Mesaj.class)
				.setParameter("gonderenUuid", gonderenUuid).setParameter("ulasti", MesajDurumu.ULASTI).list();

		session.close();

		return vtMesajlar;

	}

	public List<String> getMesajGonderenUuidler() throws HibernateException {

		Session session = factory.openSession();

		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<String> cq = cb.createQuery(String.class);
		Root<Mesaj> root = cq.from(Mesaj.class);
		cq.select(root.get("gonderenUuid")).distinct(true);
		List<String> vtUuidler = session.createQuery(cq).list();

		session.close();

		return vtUuidler;

	}

	public List<String> getMesajAlanUuidler() throws HibernateException {

		Session session = factory.openSession();

		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<String> cq = cb.createQuery(String.class);
		Root<Mesaj> root = cq.from(Mesaj.class);
		cq.select(root.get("aliciUuid")).distinct(true);
		List<String> vtUuidler = session.createQuery(cq).list();

		session.close();

		return vtUuidler;

	}

	public List<Mesaj> getIlkOkunmamisMesajdanItibarenTumMesajlar(String yerelUuid, String karsiUuid)
			throws HibernateException {

		Session session = factory.openSession();

		List<Mesaj> vtIlkOkunmamisMesaj = session.createQuery(
				"from Mesaj where gonderenUuid like :karsiUuid and aliciUuid like :yerelUuid and mesajDurumu not like :okundu",
				Mesaj.class).setParameter("yerelUuid", yerelUuid).setParameter("karsiUuid", karsiUuid)
				.setParameter("okundu", MesajDurumu.OKUNDU).setMaxResults(1).list();

		if (vtIlkOkunmamisMesaj.size() == 0) {

			return Collections.emptyList();

		}

		Long ilkId = vtIlkOkunmamisMesaj.get(0).getId();

		List<Mesaj> vtMesajlar = session.createQuery(
				"from Mesaj where id >= :ilkId and ((gonderenUuid like :yerelUuid and aliciUuid like :karsiUuid) or (gonderenUuid like :karsiUuid and aliciUuid like :yerelUuid))",
				Mesaj.class).setParameter("ilkId", ilkId).setParameter("yerelUuid", yerelUuid)
				.setParameter("karsiUuid", karsiUuid).list();

		session.close();

		return vtMesajlar;

	}

}
