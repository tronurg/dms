package com.ogya.dms.core.database.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.ogya.dms.core.structures.ViewStatus;

@Converter
public class ViewStatusConverter implements AttributeConverter<ViewStatus, Integer> {

	@Override
	public Integer convertToDatabaseColumn(ViewStatus arg0) {

		if (arg0 == null) {
			return null;
		}

		return arg0.index();

	}

	@Override
	public ViewStatus convertToEntityAttribute(Integer arg0) {

		return ViewStatus.of(arg0);

	}

}
