package com.ogya.dms.core.structures;

import javafx.scene.paint.Color;

public enum Availability {

	AVAILABLE(Color.LIMEGREEN), AWAY(Color.ORANGE), BUSY(Color.RED), LIMITED(Color.BLUE), OFFLINE(Color.GRAY);

	private final Color statusColor;

	private Availability(Color statusColor) {

		this.statusColor = statusColor;

	}

	public Color getStatusColor() {

		return statusColor;

	}

}
