package jblubble.sample;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import jblubble.BlobKey;

@Converter
public class BlobKeyConverter implements AttributeConverter<BlobKey, String> {

	@Override
	public String convertToDatabaseColumn(BlobKey blobKey) {
		return blobKey.stringValue();
	}

	@Override
	public BlobKey convertToEntityAttribute(String dbData) {
		return new BlobKey(dbData);
	}

}
