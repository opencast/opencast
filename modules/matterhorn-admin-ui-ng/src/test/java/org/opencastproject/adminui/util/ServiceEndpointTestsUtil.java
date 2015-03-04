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
package org.opencastproject.adminui.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;

import java.util.Iterator;
import java.util.Set;

public final class ServiceEndpointTestsUtil {

  private static final String COUNT = "count";
  private static final String TOTAL = "total";
  private static final String LIMIT = "limit";
  private static final String OFFSET = "offset";
  private static final String RESULTS = "results";

  private ServiceEndpointTestsUtil() {
  }

  public static void testJSONObjectEquality(JSONObject expected, JSONObject actual) {
    Assert.assertEquals(expected.size(), actual.size());
    testSimpleProperty(COUNT, expected, actual);
    testSimpleProperty(TOTAL, expected, actual);
    testSimpleProperty(LIMIT, expected, actual);
    testSimpleProperty(OFFSET, expected, actual);
    testArrayProperty(RESULTS, expected, actual);
  }

  private static void testSimpleProperty(String key, JSONObject expected, JSONObject actual) {
    Assert.assertEquals(expected.get(key), actual.get(key));
  }

  private static void testArrayProperty(String key, JSONObject expected, JSONObject actual) {
    JSONArray expectedArray = (JSONArray) expected.get(key);
    JSONArray actualArray = (JSONArray) actual.get(key);

    Assert.assertEquals(expectedArray.size(), actualArray.size());
    JSONObject exObject;
    JSONObject acObject;

    for (int i = 0; i < expectedArray.size(); i++) {
      exObject = (JSONObject) expectedArray.get(i);
      acObject = (JSONObject) actualArray.get(i);
      Set<String> exEntrySet = exObject.keySet();
      Assert.assertEquals(exEntrySet.size(), acObject.size());
      Iterator<String> exIter = exEntrySet.iterator();

      while (exIter.hasNext()) {
        String item = exIter.next();
        Object exValue = exObject.get(item);
        Object acValue = acObject.get(item);
        Assert.assertEquals(exValue, acValue);
      }
    }
  }
}
