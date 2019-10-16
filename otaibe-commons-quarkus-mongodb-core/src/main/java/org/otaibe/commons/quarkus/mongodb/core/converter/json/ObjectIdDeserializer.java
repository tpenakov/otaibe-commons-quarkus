package org.otaibe.commons.quarkus.mongodb.core.converter.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.io.IOException;

@Getter
@Setter
public class ObjectIdDeserializer extends StdDeserializer<ObjectId> {

    public ObjectIdDeserializer() {
        super(ObjectId.class);
    }

    @Override
    public ObjectId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String text = p.getText();
        return new ObjectId(text);
    }
}
