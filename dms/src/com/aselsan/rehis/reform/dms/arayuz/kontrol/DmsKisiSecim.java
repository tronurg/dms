package com.aselsan.rehis.reform.dms.arayuz.kontrol;

import java.util.List;
import java.util.function.Predicate;

import javax.swing.JComponent;

public interface DmsKisiSecim {

	JComponent getKisiSecimPanel();

	JComponent getKisiSecimPanel(Predicate<DmsKisi> filtre);

	List<Long> getSeciliKisiIdler();

	void secimiSifirla();

}
