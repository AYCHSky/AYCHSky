package eu.europeana.cloud.service.dps.service.kafka.util;

import kafka.serializer.Encoder;
import kafka.utils.VerifiableProperties;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/*
This class is used inside {@link eu.europeana.cloud.service.dps.service.kafka.KafkaSubmitService}  to serialize kafka tasks
 */
public class JsonEncoder implements Encoder<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonEncoder.class);

    public JsonEncoder(VerifiableProperties verifiableProperties) {
    }

    @Override
    public byte[] toBytes(Object object) {

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(object).getBytes();
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Json processing failed for object: %s", object.getClass().getName()), e);
        } catch (IOException e) {
            LOGGER.error("Exception happened because of {} for the object {} ", e.getMessage(), object.getClass().getName());
        }
        return "".getBytes();
    }
}