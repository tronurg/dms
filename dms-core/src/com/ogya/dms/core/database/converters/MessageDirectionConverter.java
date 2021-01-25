package com.ogya.dms.core.database.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.ogya.dms.core.structures.MessageDirection;

@Converter
public class MessageDirectionConverter implements AttributeConverter<MessageDirection, Integer> {

	@Override
	public Integer convertToDatabaseColumn(MessageDirection arg0) {

		if (arg0 == null)
			return null;

		return arg0.index();

	}

	@Override
	public MessageDirection convertToEntityAttribute(Integer arg0) {

		return MessageDirection.of(arg0);

	}

}
