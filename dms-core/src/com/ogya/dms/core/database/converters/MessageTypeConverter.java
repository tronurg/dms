package com.ogya.dms.core.database.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.ogya.dms.core.structures.MessageType;

@Converter
public class MessageTypeConverter implements AttributeConverter<MessageType, Integer> {

	@Override
	public Integer convertToDatabaseColumn(MessageType arg0) {

		return arg0.index();

	}

	@Override
	public MessageType convertToEntityAttribute(Integer arg0) {

		return MessageType.of(arg0);

	}

}
