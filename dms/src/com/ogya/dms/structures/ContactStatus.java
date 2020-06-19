package com.ogya.dms.structures;

import javafx.scene.paint.Color;

public enum ContactStatus {

	AVAILABLE(Color.LIMEGREEN), AWAY(Color.ORANGE), BUSY(Color.RED), LIMITED(Color.BLUE), OFFLINE(Color.GRAY);

	private final Color statusColor;

	private ContactStatus(Color statusColor) {

		this.statusColor = statusColor;

	}

	public Color getStatusColor() {

		return statusColor;

	}

}
