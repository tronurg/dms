package com.ogya.dms.core.database.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.ogya.dms.core.structures.WaitStatus;

@Converter
public class WaitStatusConverter implements AttributeConverter<WaitStatus, Integer> {

	@Override
	public Integer convertToDatabaseColumn(WaitStatus arg0) {

		return arg0.index();

	}

	@Override
	public WaitStatus convertToEntityAttribute(Integer arg0) {

		return WaitStatus.of(arg0);

	}

}
