package org.otaibe.commons.quarkus.core.converter.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;

@Getter
@Setter
public class DateTimeDeserializer extends StdDeserializer<DateTime> {

    private DateTimeFormatter formatter = ISODateTimeFormat.dateTime().withZoneUTC();

    public DateTimeDeserializer() {
        super(DateTime.class);
    }

    @Override
    public DateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String text = p.getText();
        return getFormatter().parseDateTime(text);
    }
}
