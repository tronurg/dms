package com.dms.core.database.tables;

import java.util.Objects;

public class EntityId {

	private final long id;
	private final boolean isGroup;
	private final Long uid;

	private EntityId(long id, boolean isGroup) {
		this.id = id;
		this.isGroup = isGroup;
		this.uid = id;
	}

	static EntityId of(long id, boolean isGroup) {
		return new EntityId(id, isGroup);
	}

	public long getId() {
		return id;
	}

	public boolean isGroup() {
		return isGroup;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EntityId)) {
			return false;
		}
		EntityId entityId = (EntityId) obj;
		return Objects.equals(this.uid, entityId.uid);
	}

	@Override
	public int hashCode() {
		return uid.hashCode();
	}

}
