package org.otaibe.commons.quarkus.mongodb.core.converter.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.io.IOException;

@Getter
@Setter
public class ObjectIdSerializer extends StdSerializer<ObjectId> {

    public ObjectIdSerializer() {
        super(ObjectId.class);
    }

    @Override
    public void serialize(ObjectId value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.toHexString());
    }
}
