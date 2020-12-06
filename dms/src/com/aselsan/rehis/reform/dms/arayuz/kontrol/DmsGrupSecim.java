package com.aselsan.rehis.reform.dms.arayuz.kontrol;

import java.util.function.Predicate;

import javax.swing.JComponent;

public interface DmsGrupSecim {

	JComponent getGrupSecimPanel();

	JComponent getGrupSecimPanel(Predicate<DmsGrup> filtre);

	Long getSeciliGrupId();

	void secimiSifirla();

}
