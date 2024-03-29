package eu.europeana.cloud.service.dps.storm.topologies.ic.topology.api;

import eu.europeana.cloud.service.dps.storm.StormTaskTuple;
import eu.europeana.cloud.service.dps.storm.topologies.ic.converter.exceptions.ICSException;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import org.apache.tika.mime.MimeTypeException;

import java.io.IOException;

/**
 * Service for converting an image with a format to the same image with a different format
 */
public interface ImageConverterService {
    /**
     * Converts image file with a format to the same image with different format
     *
     * @param stormTaskTuple Tuple which DpsTask is part of ...
     * @return path for the newly created file
     * @throws MCSException on unexpected situations.
     * @throws ICSException
     * @throws IOException
     */
    void convertFile(StormTaskTuple stormTaskTuple) throws IOException, MimeTypeException, MCSException, ICSException;
}

