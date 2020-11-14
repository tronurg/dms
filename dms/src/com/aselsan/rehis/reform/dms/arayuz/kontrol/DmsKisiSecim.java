package com.aselsan.rehis.reform.dms.arayuz.kontrol;

import java.util.List;

import javax.swing.JComponent;

public interface DmsKisiSecim {

	JComponent getKisiSecimPanel();

	List<Long> getSeciliKisiIdler();

	void secimiSifirla();

}
