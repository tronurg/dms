package com.ogya.dms.core.database.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.ogya.dms.core.structures.MessageStatus;

@Converter
public class MessageStatusConverter implements AttributeConverter<MessageStatus, Integer> {

	@Override
	public Integer convertToDatabaseColumn(MessageStatus arg0) {

		if (arg0 == null)
			return null;

		return arg0.index();

	}

	@Override
	public MessageStatus convertToEntityAttribute(Integer arg0) {

		return MessageStatus.of(arg0);

	}

}
