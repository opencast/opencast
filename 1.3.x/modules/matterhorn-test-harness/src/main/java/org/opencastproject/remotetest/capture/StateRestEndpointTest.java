package org.opencastproject.remotetest.capture;

import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;
import org.opencastproject.remotetest.util.UniversalNamespaceResolver;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


public class StateRestEndpointTest {
  TrustedHttpClient client;
  
  
  @Before
  public void setUp() throws Exception {
    client = Main.getClient();
  }
  
  @After
  public void tearDown() throws Exception {
    Main.returnClient(client);
  }
  
  @Test
  public void testGetStateGet() throws Exception {
    HttpGet request = new HttpGet(BASE_URL + "/state/state");
    HttpResponse response = client.execute(request);
    
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    
    String responseString = EntityUtils.toString(response.getEntity());
    
    Assert.assertNotNull(responseString);
    
    HashSet<String> states = new HashSet<String>();
    states.add("idle");
    states.add("capturing");
    states.add("uploading");
    states.add("unknown");
    
    Assert.assertTrue(states.contains(responseString));
    
    Thread.sleep(3000);
  }
  
  @Test
  public void testGetRecordingsNoneGet() throws Exception {
    HttpGet request = new HttpGet(BASE_URL + "/state/recordings");
    HttpResponse response = client.execute(request);
    
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    
    String responseXml = EntityUtils.toString(response.getEntity());
    Document parsedResponse = parseResponse(responseXml);
    
    Assert.assertTrue((Boolean) getXPath(parsedResponse, "/ns1:recording-state-updates", XPathConstants.BOOLEAN));

  }
  
  @Test
  public void testGetRecordingsPresentGet() throws Exception {
    String recordingId;
    
    recordingId = createRecording();
    
    HttpGet request = new HttpGet(BASE_URL + "/state/recordings");
    HttpResponse response = client.execute(request);
    
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    
    String responseXml = EntityUtils.toString(response.getEntity());
    Document parsedResponse = parseResponse(responseXml);
    
    Assert.assertTrue((Boolean) getXPath(parsedResponse, "/ns1:recording-state-updates", XPathConstants.BOOLEAN));
    
    Assert.assertTrue((Boolean) getXPath(parsedResponse, "//ns1:recording-state-update", XPathConstants.BOOLEAN));
    
    Assert.assertTrue((Boolean) getXPath(parsedResponse, "//ns1:recording-state-update/name", XPathConstants.BOOLEAN));
    Assert.assertTrue((Boolean) getXPath(parsedResponse, "//ns1:recording-state-update/state", XPathConstants.BOOLEAN));
    Assert.assertTrue((Boolean) getXPath(parsedResponse, "//ns1:recording-state-update/time-since-last-update", XPathConstants.BOOLEAN));

    Thread.sleep(10000);

    HttpDelete deleteRecordingRequest = 
      new HttpDelete(BASE_URL + "/capture-admin/recordings/"+ recordingId);
    HttpResponse deleteRecordingResponse = client.execute(deleteRecordingRequest);
    
    Assert.assertEquals(200, deleteRecordingResponse.getStatusLine().getStatusCode());
    Thread.sleep(3000);
  }
  
  protected Document parseResponse(String responseXml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(IOUtils.toInputStream(responseXml, "UTF-8"));
  }
  
  protected Object getXPath(Document document, String path, QName returnType) 
    throws XPathExpressionException, TransformerException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    xPath.setNamespaceContext(new UniversalNamespaceResolver(document));
    return xPath.compile(path).evaluate(document, returnType); 
  }

  
  protected String createRecording() throws Exception {
    String recordingId;
    
    HttpGet startCaptureRequest = new HttpGet(BASE_URL + "/captureagent/startCapture");
    HttpResponse startCaptureResponse = client.execute(startCaptureRequest);
    
    Assert.assertEquals(200, startCaptureResponse.getStatusLine().getStatusCode());
    String responseString = EntityUtils.toString(startCaptureResponse.getEntity());
    
    String pattern = "Unscheduled-\\w+-\\d+";
    Matcher matcher = Pattern.compile(pattern).matcher(responseString);
    matcher.find();
    recordingId = matcher.group();
    
    startCaptureRequest.abort();
    
    HttpGet stopCaptureRequest = new HttpGet(BASE_URL + "/captureagent/stopCapture");
    HttpResponse stopCaptureResponse = client.execute(stopCaptureRequest);
    
    Assert.assertEquals(200, stopCaptureResponse.getStatusLine().getStatusCode());
    
    stopCaptureRequest.abort();
    
    return recordingId;
  }
}