package org.otaibe.commons.quarkus.mongodb.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.io.Serializable;

@EqualsAndHashCode
@ToString
public abstract class IdEntity implements Serializable {
    public static final String ID = "_id";

    @JsonIgnore
    @BsonId()
    @BsonProperty(ID)
    ObjectId id;

    @JsonProperty(ID)
    @BsonIgnore
    String idPretty;

    public String getIdPretty() {
        return idPretty;
    }

    public void setIdPretty(String idPretty) {
        this.idPretty = idPretty;
        if (idPretty == null) {
            this.id = null;
            return;
        }
        this.id = new ObjectId(idPretty);

    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
        if (id == null) {
            this.idPretty = null;
            return;
        }
        this.idPretty = id.toHexString();
    }
}
