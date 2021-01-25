package com.ogya.dms.core.database.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.ogya.dms.core.structures.ReceiverType;

@Converter
public class ReceiverTypeConverter implements AttributeConverter<ReceiverType, Integer> {

	@Override
	public Integer convertToDatabaseColumn(ReceiverType arg0) {

		return arg0.index();

	}

	@Override
	public ReceiverType convertToEntityAttribute(Integer arg0) {

		return ReceiverType.of(arg0);

	}

}
