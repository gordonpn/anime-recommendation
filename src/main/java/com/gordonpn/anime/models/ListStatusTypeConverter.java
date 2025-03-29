package com.gordonpn.anime.models;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ListStatusTypeConverter implements AttributeConverter<ListStatusType, String> {
    @Override
    public String convertToDatabaseColumn(ListStatusType attribute) {
        return attribute.getValue();
    }

    @Override
    public ListStatusType convertToEntityAttribute(String dbData) {
        return ListStatusType.fromValue(dbData);
    }
}
