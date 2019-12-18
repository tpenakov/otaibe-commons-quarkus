package org.otaibe.commons.quarkus.core.converter.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.time.ZonedDateTime;

@Getter
@Setter
public class ZonedDateTimeSerializer extends StdSerializer<ZonedDateTime> {

    public ZonedDateTimeSerializer() {
        super(ZonedDateTime.class);
    }

    @Override
    public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.toString());
    }
}
