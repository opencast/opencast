/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.loadtest.impl;

import org.opencastproject.security.api.TrustedHttpClient;

import junit.framework.Assert;
import static org.easymock.EasyMock.*;

import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.util.Properties;

public class LoadTestTest {
  private Properties properties;
  private TrustedHttpClient clientMock;
  private ComponentContext contextMock;
  private BundleContext bundleContextMock;
  private LoadTest loadTest;
   
  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    properties = new Properties();
    clientMock = EasyMock.createNiceMock(TrustedHttpClient.class);
    contextMock = EasyMock.createMock(ComponentContext.class);
    bundleContextMock = EasyMock.createNiceMock(BundleContext.class);
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {

  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistribution()}.
   * 
   * @throws Exception
   */
  @Test
  public void emptyPackageDistributionResultsInDefaultDistribution() throws Exception {
    // Empty Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 1 }", loadTest.getPrettyPackageDistribution());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistribution()}.
   * 
   * @throws Exception
   */
  @Test
  public void nullPackageDistributionResultsInDefaultDistribution() throws Exception {
    // Null Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_KEY, "");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 1 }", loadTest.getPrettyPackageDistribution());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistribution()}.
   * 
   * @throws Exception
   */
  @Test
  public void stringPackageDistributionResultsInDefaultDistribution() throws Exception {
    // Text instead of integer.
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_KEY, "Some String");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 1 }", loadTest.getPrettyPackageDistribution());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistribution()}.
   * 
   * @throws Exception
   */
  @Test
  public void zeroPackageDistributionResultsInDefaultDistribution() throws Exception {
    // 0 Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_KEY, "0");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 0 }", loadTest.getPrettyPackageDistribution());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistribution()}.
   * 
   * @throws Exception
   */
  @Test
  public void onePackageDistributionResultsInOneLoadTestDistribution() throws Exception {
    // 1 Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_KEY, "1");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 1 }", loadTest.getPrettyPackageDistribution());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistribution()}.
   * 
   * @throws Exception
   */
  @Test
  public void tenPackageDistributionResultsInOneGroupOfTenLoadTestDistribution() throws Exception {
    // 10 Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_KEY, "10");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 10 }", loadTest.getPrettyPackageDistribution());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistribution()}.
   * 
   * @throws Exception
   */
  @Test
  public void tooLargeIntegerDistributionResultsInDefaultPackageDistribution() throws Exception {
    // Number too large for Integer.
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_KEY, "21111111111111111111");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 1 }", loadTest.getPrettyPackageDistribution());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistribution()}.
   * 
   * @throws Exception
   */
  @Test
  public void negativeIntegerDistributionResultsInPositivePackageDistribution() throws Exception {
    // Number too large for Integer.
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_KEY, "-3");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 3 }", loadTest.getPrettyPackageDistribution());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistribution()}.
   * 
   * @throws Exception
   */
  @Test
  public void twoNumberDistributionInputCreatesTwoNumberOutput() throws Exception {
    // Two number (1,0) Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_KEY, "1,0");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 1 0 }", loadTest.getPrettyPackageDistribution());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistribution()}.
   * 
   * @throws Exception
   */
  @Test
  public void reallyLongNumberOfDistributionInputCreatesCorrectNumberOutput() throws Exception {
    // Two number (1,0) Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_KEY, "1,2,3,4,5,6,7,8,9,10,0,11,12,13,14,15,16,17,18,19,20,211111111");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 1 2 3 4 5 6 7 8 9 10 0 11 12 13 14 15 16 17 18 19 20 211111111 }", loadTest
            .getPrettyPackageDistribution());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistributionTimings()}.
   * 
   * @throws Exception
   */
  @Test
  public void emptyPackageDistributionTimingsResultsInDefault() throws Exception {
    // Empty Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_TIMINGS_KEY, "");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 0 }", loadTest.getPrettyPackageDistributionTimings());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistributionTimings()}.
   * 
   * @throws Exception
   */
  @Test
  public void textInsteadOfIntegerPackageDistributionTimingsResultsInDefault() throws Exception {
    // Text Input for Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_TIMINGS_KEY, "Some String");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 0 }", loadTest.getPrettyPackageDistributionTimings());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistributionTimings()}.
   * 
   * @throws Exception
   */
  @Test
  public void zeropPackageDistributionTimingsResultsInZeroDelay() throws Exception {
    // 0 Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_TIMINGS_KEY, "0");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 0 }", loadTest.getPrettyPackageDistributionTimings());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistributionTimings()}.
   * 
   * @throws Exception
   */
  @Test
  public void oneDelayPackageDistributionTimingsResultsInOneMinuteDelay() throws Exception {
    // 1 Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_TIMINGS_KEY, "1");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 1 }", loadTest.getPrettyPackageDistributionTimings());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistributionTimings()}.
   * 
   * @throws Exception
   */
  @Test
  public void negativePackageDistributionTimingsResultsInPositiveValues() throws Exception {
    // -5 Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_TIMINGS_KEY, "-5");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 5 }", loadTest.getPrettyPackageDistributionTimings());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistributionTimings()}.
   * 
   * @throws Exception
   */
  @Test
  public void tooLargeForIntegerPackageDistributionTimingsResultsInDefaultValue() throws Exception {
    // 21111111111111111111 Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_TIMINGS_KEY, "21111111111111111111");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 0 }", loadTest.getPrettyPackageDistributionTimings());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistributionTimings()}.
   * 
   * @throws Exception
   */
  @Test
  public void twoValuePackageDistributionTimingsResultsInCorrectOutput() throws Exception {
    // Two number (1,0) Distribution
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_TIMINGS_KEY, "1,0");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 1 0 }", loadTest.getPrettyPackageDistributionTimings());
  }

  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updatePackageDistributionTimings()}.
   * 
   * @throws Exception
   */
  @Test
  public void longPackageDistributionTimingsResultsInCorrectOutput() throws Exception {
    // Long number of distributions.
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.PACKAGE_DISTRIBUTION_TIMINGS_KEY,
            "1,2,3,4,5,6,7,8,9,10,0,11,12,13,14,15,16,17,18,19,20,211111111");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("{ 1 2 3 4 5 6 7 8 9 10 0 11 12 13 14 15 16 17 18 19 20 211111111 }", loadTest
            .getPrettyPackageDistributionTimings());
  }
  
  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updateCoreAddress()}.
   * 
   * @throws Exception
   */
  @Test
  public void validIPCoreAddressResultsInGoodURL() throws Exception {
    // 
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.CORE_ADDRESS_KEY,
            "http://1.2.3.4:8080");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("http://1.2.3.4:8080", loadTest.getCoreAddress());
  }
  
  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updateCoreAddress()}.
   * 
   * @throws Exception
   */
  @Test
  public void validHostnameCoreAddressResultsInGoodURL() throws Exception {
    // http://test.url.com:8080
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    replay(contextMock);
    properties.put(LoadTest.CORE_ADDRESS_KEY,
            "http://test.url.com:8080");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("http://test.url.com:8080", loadTest.getCoreAddress());
    verify(contextMock);
  }
  
  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updateCoreAddress()}.
   * 
   * @throws Exception
   */
  @Test
  public void noHostnameUsesCorePropertyFromFelixContext() throws Exception {
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKSPACE_KEY, "/example/path");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    expect(contextMock.getBundleContext()).andReturn(bundleContextMock).times(2);
    expect(bundleContextMock.getProperty(LoadTest.BUNDLE_CONTEXT_SERVER_URL)).andReturn("http://test.url.com:8080");
    replay(contextMock);
    replay(bundleContextMock);
    loadTest = new LoadTest(properties, clientMock, contextMock);
    verify(contextMock);
    verify(bundleContextMock);
    Assert.assertEquals("http://test.url.com:8080", loadTest.getCoreAddress());
  }
  
  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updateCoreAddress()}.
   * 
   * @throws Exception
   */
  @Test
  public void validWorkspaceLocationIsSetCorrectly() throws Exception {
    // 
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.WORKSPACE_KEY, "/example/location");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("/example/location", loadTest.getWorkspaceLocation());
  }
  
  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updateCoreAddress()}.
   * 
   * @throws Exception
   */
  @Test
  public void noWorkspaceUsesCorePropertyFromFelixContext() throws Exception {
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.CORE_ADDRESS_KEY, "http://test.url.com:8080");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    expect(contextMock.getBundleContext()).andReturn(bundleContextMock).times(2);
    expect(bundleContextMock.getProperty(LoadTest.BUNDLE_CONTEXT_STORAGE_DIR)).andReturn("/example/location");
    replay(contextMock);
    replay(bundleContextMock);
    loadTest = new LoadTest(properties, clientMock, contextMock);
    verify(contextMock);
    verify(bundleContextMock);
    Assert.assertEquals("/example/location/loadtest/workspace/", loadTest.getWorkspaceLocation());
  }
  
  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updateCoreAddress()}.
   * 
   * @throws Exception
   */
  @Test
  public void validSourceLocationIsSetCorrectly() throws Exception {
    // 
    properties.put(LoadTest.WORKSPACE_KEY, "/example/location");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("/example/location", loadTest.getWorkspaceLocation());
  }
  
  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updateCoreAddress()}.
   * 
   * @throws Exception
   */
  @Test
  public void noSourceUsesCorePropertyFromFelixContext() throws Exception {
    properties.put(LoadTest.WORKSPACE_KEY, "/example/location");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    properties.put(LoadTest.CORE_ADDRESS_KEY, "http://test.url.com:8080");
    expect(contextMock.getBundleContext()).andReturn(bundleContextMock);
    expect(bundleContextMock.getProperty(LoadTest.BUNDLE_CONTEXT_STORAGE_DIR)).andReturn("/example/location");
    replay(contextMock);
    replay(bundleContextMock);
    loadTest = new LoadTest(properties, clientMock, contextMock);
    verify(contextMock);
    verify(bundleContextMock);
    Assert.assertEquals("/example/location/loadtest/source/media.zip", loadTest.getSourceMediaPackageLocation());
  }
  
  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updateCoreAddress()}.
   * 
   * @throws Exception
   */
  @Test
  public void jobCheckIntervalSetCorrectly() throws Exception {
    // 
    properties.put(LoadTest.WORKSPACE_KEY, "/example/location");
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.JOB_CHECK_INTERVAL_KEY, "55");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals(55, loadTest.getJobCheckInterval());
  }
  
  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updateCoreAddress()}.
   * 
   * @throws Exception
   */
  @Test
  public void jobCheckIntervalNotSetDefaultIsUsed() throws Exception {
    // 
    properties.put(LoadTest.WORKSPACE_KEY, "/example/location");
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals(5, loadTest.getJobCheckInterval());
  }
  
  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updateCoreAddress()}.
   * 
   * @throws Exception
   */
  @Test
  public void jobCheckIntervalNegativeIsSetPositive() throws Exception {
    // 
    properties.put(LoadTest.WORKSPACE_KEY, "/example/location");
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.JOB_CHECK_INTERVAL_KEY, "-20");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals(20, loadTest.getJobCheckInterval());
  }
  
  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updateCoreAddress()}.
   * 
   * @throws Exception
   */
  @Test
  public void jobCheckSetToStringInsteadOfNumberGetsDefaultValue() throws Exception {
    // 
    properties.put(LoadTest.WORKSPACE_KEY, "/example/location");
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKFLOW_KEY, "full");
    
    properties.put(LoadTest.JOB_CHECK_INTERVAL_KEY, "Job Interval");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals(LoadTest.DEFAULT_JOB_CHECK_INTERVAL, loadTest.getJobCheckInterval());
  }
  
  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updateCoreAddress()}.
   * 
   * @throws Exception
   */
  @Test
  public void defaultWorkflowIDIsUsed() throws Exception {
    properties.put(LoadTest.CORE_ADDRESS_KEY, "http://test.url.com:8080");
    expect(contextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    expect(bundleContextMock.getProperty(LoadTest.WORKFLOW_KEY)).andReturn(null).anyTimes();
    replay(contextMock);
    replay(bundleContextMock);
    properties.put(LoadTest.WORKSPACE_KEY, "/example/location");
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals(LoadTest.DEFAULT_WORKFLOW_ID, loadTest.getWorkflowID());
  }
  
  /**
   * Test method for {@link org.opencastproject.load.test.LoadTesting#updateCoreAddress()}.
   * 
   * @throws Exception
   */
  @Test
  public void validCustomWorkflowIDIsUsed() throws Exception {
    // 
    properties.put(LoadTest.WORKSPACE_KEY, "/example/location");
    properties.put(LoadTest.SOURCE_MEDIA_PACKAGE_KEY, "/example/location");
    properties.put(LoadTest.WORKFLOW_KEY, "Custom Workflow ID #1");
    loadTest = new LoadTest(properties, clientMock, contextMock);
    Assert.assertEquals("Custom Workflow ID #1", loadTest.getWorkflowID());
  }
}
