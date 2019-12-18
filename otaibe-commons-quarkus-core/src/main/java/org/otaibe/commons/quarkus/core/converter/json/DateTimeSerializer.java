package org.otaibe.commons.quarkus.core.converter.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;

@Getter
@Setter
public class DateTimeSerializer extends StdSerializer<DateTime> {

    private DateTimeFormatter formatter = ISODateTimeFormat.dateTime().withZoneUTC();

    public DateTimeSerializer() {
        super(DateTime.class);
    }

    @Override
    public void serialize(DateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(getFormatter().print(value));
    }
}
