package com.ogya.dms.core.database.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.ogya.dms.core.structures.GroupReceiverType;

@Converter
public class GroupReceiverTypeConverter implements AttributeConverter<GroupReceiverType, Integer> {

	@Override
	public Integer convertToDatabaseColumn(GroupReceiverType arg0) {

		if (arg0 == null)
			return null;

		return arg0.index();

	}

	@Override
	public GroupReceiverType convertToEntityAttribute(Integer arg0) {

		return GroupReceiverType.of(arg0);

	}

}
