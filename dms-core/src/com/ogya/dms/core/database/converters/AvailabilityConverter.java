package com.ogya.dms.core.database.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.ogya.dms.core.structures.Availability;

@Converter
public class AvailabilityConverter implements AttributeConverter<Availability, Integer> {

	@Override
	public Integer convertToDatabaseColumn(Availability arg0) {

		return arg0.index();

	}

	@Override
	public Availability convertToEntityAttribute(Integer arg0) {

		return Availability.of(arg0);

	}

}
