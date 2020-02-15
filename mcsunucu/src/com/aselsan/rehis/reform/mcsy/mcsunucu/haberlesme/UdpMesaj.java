package com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme;

import java.nio.ByteBuffer;
import java.util.TreeMap;

public class UdpMesaj {

	final int mesajId;
	final int parcaSayisi;

	private final TreeMap<Integer, byte[]> mesajParcalari = new TreeMap<Integer, byte[]>();

	private int toplamMesajBoyu = 0;

	UdpMesaj(int mesajId, int parcaSayisi) {

		this.mesajId = mesajId;
		this.parcaSayisi = parcaSayisi;

	}

	boolean karsilastir(int mesajId, int parcaSayisi) {

		return this.mesajId == mesajId && this.parcaSayisi == parcaSayisi;

	}

	boolean mesajAlindi(int parcaNo, byte[] mesaj) {

		mesajParcalari.put(parcaNo, mesaj);

		toplamMesajBoyu += mesaj.length;

		return mesajParcalari.firstKey() == 0 && mesajParcalari.lastKey() == parcaSayisi - 1
				&& mesajParcalari.size() == parcaSayisi;

	}

	byte[] mesajiAl() {

		final ByteBuffer buffer = ByteBuffer.allocate(toplamMesajBoyu);

		mesajParcalari.forEach((e0, e1) -> buffer.put(e1));

		return buffer.array();

	}

}
