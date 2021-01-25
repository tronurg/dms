package com.ogya.dms.core.database.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.ogya.dms.core.structures.MessageSubType;

@Converter
public class MessageSubTypeConverter implements AttributeConverter<MessageSubType, Integer> {

	@Override
	public Integer convertToDatabaseColumn(MessageSubType arg0) {

		if (arg0 == null)
			return null;

		return arg0.index();

	}

	@Override
	public MessageSubType convertToEntityAttribute(Integer arg0) {

		return MessageSubType.of(arg0);

	}

}
