package org.otaibe.commons.quarkus.mongodb.core.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class DateTimeCodec implements Codec<DateTime> {

    DateTimeFormatter formatter = ISODateTimeFormat.dateTime();

    @Override
    public DateTime decode(BsonReader reader, DecoderContext decoderContext) {
        return formatter.parseDateTime(reader.readString());
    }

    @Override
    public void encode(BsonWriter writer, DateTime value, EncoderContext encoderContext) {
        writer.writeString(formatter.print(value));
    }

    @Override
    public Class<DateTime> getEncoderClass() {
        return DateTime.class;
    }
}
