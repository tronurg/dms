package com.ogya.dms.core.database.tables;

import com.ogya.dms.core.structures.Availability;

public abstract class EntityBase {

	public abstract Long getId();

	public abstract String getName();

	public abstract String getComment();

	public abstract Availability getStatus();

	public abstract Double getLattitude();

	public abstract Double getLongitude();

	public abstract EntityId getEntityId();

}
