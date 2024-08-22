package com.dms.core.database.tables;

import java.util.Optional;

public class EntityId {

	private final long id;
	private final boolean isGroup;

	private EntityId(Long id, boolean isGroup) {
		super();
		this.id = Optional.ofNullable(id).orElse(Long.MAX_VALUE);
		this.isGroup = isGroup;
	}

	static EntityId of(DmsEntity entity) {
		if (entity == null) {
			return null;
		}
		return new EntityId(entity.getId(), entity.isGroup());
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
		return this.getId() == entityId.getId();
	}

	@Override
	public int hashCode() {
		return Long.hashCode(getId());
	}

}
