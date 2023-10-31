package org.otaibe.commons.quarkus.core.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class CustomObjectMapperConfig {

    /*
    @Singleton
    @Produces
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        //todo - perform configuration
        fillObjectMapper(mapper);
        //mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }
     */

    public void fillObjectMapper(final ObjectMapper objectMapper1) {

        // perform configuration
        objectMapper1
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        ;
        objectMapper1.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //objectMapper1.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"));
    }

}
