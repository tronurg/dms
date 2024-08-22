package com.dms.core.database.tables;

import java.util.Objects;

import com.dms.core.structures.Availability;
import com.dms.core.structures.ViewStatus;

public abstract class DmsEntity {

	public abstract Long getId();

	public abstract boolean isGroup();

	public abstract String getName();

	public abstract String getComment();

	public abstract Availability getStatus();

	public abstract ViewStatus getViewStatus();

	public abstract Double getLatitude();

	public abstract Double getLongitude();

	public EntityId getEntityId() {
		return EntityId.of(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DmsEntity)) {
			return false;
		}
		DmsEntity entity = (DmsEntity) obj;
		return Objects.equals(this.getId(), entity.getId());
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

}
