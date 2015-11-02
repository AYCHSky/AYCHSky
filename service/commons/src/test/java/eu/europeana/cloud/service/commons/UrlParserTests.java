package eu.europeana.cloud.service.commons;

import eu.europeana.cloud.service.commons.urls.UrlBuilderException;
import eu.europeana.cloud.service.commons.urls.UrlParser;
import eu.europeana.cloud.service.commons.urls.UrlPart;
import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;

public class UrlParserTests {


    private static final String MALFORMED_URL = "fdafsdfasdfadagadfa";
    private static final String RANDOM_URL = "http://blog.europeana.eu/2015/10/10-terrifying-costume-ideas-for-halloween/";
    private static final String URL_TO_FILE = "http://127.0.0.1:8080/mcs/records/FUWQ4WMUGIGEHVA3X7FY5PA3DR5Q4B2C4TWKNILLS6EM4SJNTVEQ/representations/TIFF/versions/86318b00-6377-11e5-a1c6-90e6ba2d09ef/files/sampleFileName.txt";
    private static final String URL_TO_FILES_LIST = "http://127.0.0.1:8080/mcs/records/FUWQ4WMUGIGEHVA3X7FY5PA3DR5Q4B2C4TWKNILLS6EM4SJNTVEQ/representations/TIFF/versions/86318b00-6377-11e5-a1c6-90e6ba2d09ef/files";

    private static final String URL_TO_VERSIONS_LIST = "http://127.0.0.1:8080/mcs/records/FUWQ4WMUGIGEHVA3X7FY5PA3DR5Q4B2C4TWKNILLS6EM4SJNTVEQ/representations/TIFF/versions";
    private static final String URL_TO_VERSION = "http://127.0.0.1:8080/mcs/records/FUWQ4WMUGIGEHVA3X7FY5PA3DR5Q4B2C4TWKNILLS6EM4SJNTVEQ/representations/TIFF/versions/86318b00-6377-11e5-a1c6-90e6ba2d09ef";

    private static final String URL_TO_REPRESENTATIONS_LIST = "http://127.0.0.1:8080/mcs/records/FUWQ4WMUGIGEHVA3X7FY5PA3DR5Q4B2C4TWKNILLS6EM4SJNTVEQ/representations";
    private static final String URL_TO_REPRESENTATION = "http://127.0.0.1:8080/mcs/records/FUWQ4WMUGIGEHVA3X7FY5PA3DR5Q4B2C4TWKNILLS6EM4SJNTVEQ/representations/TIFF";

    private static final String URL_TO_CLOUDID = "http://127.0.0.1:8080/mcs/records/FUWQ4WMUGIGEHVA3X7FY5PA3DR5Q4B2C4TWKNILLS6EM4SJNTVEQ";

    private static final String URL_TO_DATASETS_LIST = "http://127.0.0.1:8080/mcs/data-providers/sampleDP/data-sets";
    private static final String URL_TO_DATASET = "http://127.0.0.1:8080/mcs/data-providers/sampleDP/data-sets/sampleDataSet";
    
    private static final String TEST = "http://127.0.0.1:8080/mcs/records/FUWQ4WMUGIGEHVA3X7FY5PA3DR5Q4B2C4TWKNILLS6EM4SJNTVEQ/representations/TIFF/versions/86318b00-6377-11e5-a1c6-90e6ba2d09ef/permissions/all/users/pwoz";

    @Test(expected = MalformedURLException.class)
    public void shouldThrowMalformedURLException() throws MalformedURLException {
        UrlParser urlParser = new UrlParser(MALFORMED_URL);
        Assert.assertTrue(urlParser.isUrlToRepresentations());
    }
    
    @Test
    public void shouldHandleRandomUrl() throws MalformedURLException {
        UrlParser urlParser = new UrlParser(RANDOM_URL);
        Assert.assertFalse(urlParser.isUrlToRepresentationVersionFile());
        Assert.assertFalse(urlParser.isUrlToDatasetsList());
        Assert.assertFalse(urlParser.isUrlToDataset());
        Assert.assertFalse(urlParser.isUrlToRepresentations());
        Assert.assertFalse(urlParser.isUrlToRepresentationVersion());
        Assert.assertNull(urlParser.getPart(UrlPart.VERSIONS));
        Assert.assertNull(urlParser.getPart(UrlPart.REPRESENTATIONS));
        Assert.assertNull(urlParser.getPart(UrlPart.RECORDS));
        Assert.assertNull(urlParser.getPart(UrlPart.DATA_PROVIDERS));
        Assert.assertNull(urlParser.getPart(UrlPart.DATA_SETS));
        Assert.assertNull(urlParser.getPart(UrlPart.FILES));
    }

    @Test
    public void shouldMarkGivenUrlAsUrlToFile() throws MalformedURLException {
        UrlParser urlParser = new UrlParser(URL_TO_FILE);
        Assert.assertTrue(urlParser.isUrlToRepresentationVersionFile());
        Assert.assertFalse(urlParser.isUrlToRepresentationVersionFiles());
    }

    @Test
    public void shouldMarkGivenUrlAsUrlToFilesList() throws MalformedURLException {
        UrlParser urlParser = new UrlParser(URL_TO_FILES_LIST);
        Assert.assertTrue(urlParser.isUrlToRepresentationVersionFiles());
    }

    @Test
    public void shouldMarkGivenUrlAsUrlToVersionsList() throws MalformedURLException {
        UrlParser urlParser = new UrlParser(URL_TO_VERSIONS_LIST);
        Assert.assertTrue(urlParser.isUrlToRepresentationVersions());
        Assert.assertFalse(urlParser.isUrlToRepresentationVersionFiles());
        Assert.assertFalse(urlParser.isUrlToRepresentationVersionFile());
    }

    @Test
    public void shouldMarkGivenUrlAsUrlToVersion() throws MalformedURLException {
        UrlParser urlParser = new UrlParser(URL_TO_VERSION);
        Assert.assertTrue(urlParser.isUrlToRepresentationVersion());
    }

    @Test
    public void shouldMarkGivenUrlAsUrlToRepresentation() throws MalformedURLException {
        UrlParser urlParser = new UrlParser(URL_TO_REPRESENTATION);
        Assert.assertTrue(urlParser.isUrlToRepresentation());
    }

    @Test
    public void shouldMarkGivenUrlAsUrlToRepresentationsList() throws MalformedURLException {
        UrlParser urlParser = new UrlParser(URL_TO_REPRESENTATIONS_LIST);
        Assert.assertTrue(urlParser.isUrlToRepresentations());
    }

    @Test
    public void shouldMarkGivenUrlAsUrlToCloudId() throws MalformedURLException {
        UrlParser urlParser = new UrlParser(URL_TO_CLOUDID);
        Assert.assertTrue(urlParser.isUrlToCloudId());
        Assert.assertFalse(urlParser.isUrlToRepresentations());
        Assert.assertFalse(urlParser.isUrlToRepresentationVersionFiles());
    }

    @Test
    public void shouldMarkGivenUrlAsUrlToDataSetsList() throws MalformedURLException {
        UrlParser urlParser = new UrlParser(URL_TO_DATASETS_LIST);
        Assert.assertTrue(urlParser.isUrlToDatasetsList());
        Assert.assertFalse(urlParser.isUrlToRepresentations());
        Assert.assertFalse(urlParser.isUrlToRepresentationVersionFiles());
        Assert.assertFalse(urlParser.isUrlToDataset());
    }

    @Test
    public void shouldMarkGivenUrlAsUrlToDataSet() throws MalformedURLException {
        UrlParser urlParser = new UrlParser(URL_TO_DATASET);
        Assert.assertTrue(urlParser.isUrlToDataset());
        Assert.assertFalse(urlParser.isUrlToDatasetsList());
        Assert.assertFalse(urlParser.isUrlToRepresentations());
        Assert.assertFalse(urlParser.isUrlToRepresentationVersionFiles());
    }

    @Test
    public void shouldReturnProperUrlPasts() throws MalformedURLException {
        UrlParser urlParser = new UrlParser(URL_TO_DATASET);
        Assert.assertEquals(urlParser.getPart(UrlPart.DATA_SETS), "sampleDataSet");
        Assert.assertEquals(urlParser.getPart(UrlPart.DATA_PROVIDERS), "sampleDP");
        Assert.assertNull(urlParser.getPart(UrlPart.VERSIONS));
        Assert.assertNull(urlParser.getPart(UrlPart.RECORDS));
        Assert.assertNull(urlParser.getPart(UrlPart.REPRESENTATIONS));

        urlParser = new UrlParser(URL_TO_FILE);
        Assert.assertNull(urlParser.getPart(UrlPart.DATA_SETS));
        Assert.assertNull(urlParser.getPart(UrlPart.DATA_PROVIDERS));

        Assert.assertEquals(urlParser.getPart(UrlPart.VERSIONS), "86318b00-6377-11e5-a1c6-90e6ba2d09ef");
        Assert.assertEquals(urlParser.getPart(UrlPart.RECORDS), "FUWQ4WMUGIGEHVA3X7FY5PA3DR5Q4B2C4TWKNILLS6EM4SJNTVEQ");
        Assert.assertEquals(urlParser.getPart(UrlPart.REPRESENTATIONS), "TIFF");
    }

    @Test
    public void shouldReturnProperVersionUrlFromFileUrl() throws MalformedURLException, UrlBuilderException {
        UrlParser urlParser = new UrlParser(URL_TO_FILE);
        Assert.assertEquals(urlParser.getVersionUrl(), URL_TO_VERSION);
    }
    
    @Test(expected = UrlBuilderException.class)
    public void shouldThrowExceptionWhileCreatingUrlToVersionFromWrongUrl() throws MalformedURLException, UrlBuilderException {
        UrlParser urlParser = new UrlParser(URL_TO_CLOUDID);
        Assert.assertEquals(urlParser.getVersionUrl(), URL_TO_VERSION);
    }

    @Test
    public void shouldReturnProperVersionsUrlFromFileUrl() throws MalformedURLException, UrlBuilderException {
        UrlParser urlParser = new UrlParser(URL_TO_FILE);
        Assert.assertEquals(urlParser.getVersionsUrl(), URL_TO_VERSIONS_LIST);
    }
    
    @Test(expected = UrlBuilderException.class)
    public void shouldThrowExceptionWhileTryingToGetUrlToVersionFromDatasetUrl() throws MalformedURLException, UrlBuilderException {
        UrlParser urlParser = new UrlParser(URL_TO_DATASET);
        urlParser.getVersionsUrl();
    }

    @Test
    public void shouldReturnProperDatasetsUrlFromDataSetUrl() throws MalformedURLException, UrlBuilderException {
        UrlParser urlParser = new UrlParser(URL_TO_DATASET);
        Assert.assertEquals(urlParser.getDataSetsUrl(), URL_TO_DATASETS_LIST);
        
    }
}