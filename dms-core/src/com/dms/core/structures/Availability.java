package com.dms.core.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.paint.Color;

public enum Availability {

	AVAILABLE(0, Color.LIMEGREEN), AWAY(1, Color.ORANGE), BUSY(2, Color.RED), LIMITED(3, Color.BLUE),
	OFFLINE(4, Color.GRAY);

	private static final Map<Integer, Availability> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, Availability>());

	private final int index;
	private final Color statusColor;

	static {
		for (Availability value : values()) {
			INDEX_MAP.put(value.index, value);
		}
	}

	private Availability(int index, Color statusColor) {

		this.index = index;
		this.statusColor = statusColor;

	}

	public static Availability of(Integer index) {
		return INDEX_MAP.get(index);
	}

	public int index() {
		return index;
	}

	public Color getStatusColor() {

		return statusColor;

	}

}
