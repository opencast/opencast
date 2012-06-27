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
package org.opencastproject.security;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TrustedHttpClientTest {

  @Test
  public void testConnectionWithTimeout() throws Exception {
    TrustedHttpClientImpl client = new TrustedHttpClientImpl("admin", "opencast");
    HttpGet get = new HttpGet("http://opencast00.usask.ca:8080/welcome.html");
    HttpResponse response = client.execute(get);
    Assert.assertEquals("With no timeout, we should get a 200 OK", HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    try {
      // After 1 millisecond, timeout
      client.execute(get, 1, 1);
      Assert.fail("A 1ms timeout should have caused this test to fail");
    } catch(Exception e) {
      // expected
    }
  }
}
