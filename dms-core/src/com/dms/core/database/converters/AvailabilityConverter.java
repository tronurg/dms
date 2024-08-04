package com.dms.core.database.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.dms.core.structures.Availability;

@Converter
public class AvailabilityConverter implements AttributeConverter<Availability, Integer> {

	@Override
	public Integer convertToDatabaseColumn(Availability arg0) {

		if (arg0 == null) {
			return null;
		}

		return arg0.index();

	}

	@Override
	public Availability convertToEntityAttribute(Integer arg0) {

		return Availability.of(arg0);

	}

}
