package com.dms.core.database.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.dms.core.structures.AttachmentType;

@Converter
public class AttachmentTypeConverter implements AttributeConverter<AttachmentType, Integer> {

	@Override
	public Integer convertToDatabaseColumn(AttachmentType arg0) {

		if (arg0 == null) {
			return null;
		}

		return arg0.index();

	}

	@Override
	public AttachmentType convertToEntityAttribute(Integer arg0) {

		return AttachmentType.of(arg0);

	}

}
