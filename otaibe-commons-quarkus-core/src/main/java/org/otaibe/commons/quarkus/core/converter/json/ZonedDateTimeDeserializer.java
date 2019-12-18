package org.otaibe.commons.quarkus.core.converter.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;

import java.io.IOException;
import java.time.ZonedDateTime;

@Getter
@Setter
public class ZonedDateTimeDeserializer extends StdDeserializer<ZonedDateTime> {

    public ZonedDateTimeDeserializer() {
        super(DateTime.class);
    }

    @Override
    public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String text = p.getText();
        return ZonedDateTime.parse(text);
    }
}
