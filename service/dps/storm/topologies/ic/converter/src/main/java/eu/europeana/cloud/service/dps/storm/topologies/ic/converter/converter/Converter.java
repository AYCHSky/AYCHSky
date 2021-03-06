package eu.europeana.cloud.service.dps.storm.topologies.ic.converter.converter;


import eu.europeana.cloud.service.dps.storm.topologies.ic.converter.exceptions.ConversionException;
import eu.europeana.cloud.service.dps.storm.topologies.ic.converter.exceptions.UnexpectedExtensionsException;

import java.io.IOException;
import java.util.List;

/**
 * Service for converting one file to another
 */
public interface Converter {
    /**
     * Converts File to another based on a context and list of properties
     *
     * @param inputFilePath  The input file full path
     * @param outputFilePath The output file full path
     * @param properties     List of properties attached to the kakadu command
     * @throws UnexpectedExtensionsException : throws when providing invalid or inconsistent extensions
     * @throws IOException
     */
    void convert(String inputFilePath, String outputFilePath, List<String> properties) throws UnexpectedExtensionsException,ConversionException, IOException;

}
