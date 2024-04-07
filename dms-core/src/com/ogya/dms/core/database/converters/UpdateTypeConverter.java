package com.ogya.dms.core.database.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.ogya.dms.core.structures.UpdateType;

@Converter
public class UpdateTypeConverter implements AttributeConverter<UpdateType, Integer> {

	@Override
	public Integer convertToDatabaseColumn(UpdateType arg0) {

		if (arg0 == null) {
			return null;
		}

		return arg0.index();

	}

	@Override
	public UpdateType convertToEntityAttribute(Integer arg0) {

		return UpdateType.of(arg0);

	}

}
