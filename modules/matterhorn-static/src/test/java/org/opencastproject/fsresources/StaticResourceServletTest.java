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
package org.opencastproject.fsresources;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletOutputStream;

/**
 * Test for StaticResourceServlet
 * 
 * Sept 22, 2014 MH-10447, fix for files of size 2048*C bytes in copyRange()
 * 
 */
public class StaticResourceServletTest {

  private StaticResourceServlet servlet;
  private ServletOutputStream ostream;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {

    servlet = new StaticResourceServlet();

    // Mock the ostream
    // Specifically, throw exception if len is -1
    // http://docs.oracle.com/javase/1.5.0/docs/api/java/io/OutputStream.html#write%28byte[],%20int,%20int%29
    ostream = EasyMock.createNiceMock(ServletOutputStream.class);
    ostream.write((byte[]) EasyMock.anyObject(), EasyMock.eq(0), EasyMock.anyInt());
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() {
        //supply your mock implementation here...
        int len = (Integer) EasyMock.getCurrentArguments()[2];
        // negative len
        if (len == -1) {
          throw new IndexOutOfBoundsException();
        }
        return null;
      }
    }).anyTimes();
    EasyMock.replay(ostream);

  }

  /**
   * Helper utility for copyRange() method test
   * 
   * @param byteArray
   * @param start
   * @param end
   */
  private void testCopyRangeMethod(byte[] byteArray, long start, long end) {
    InputStream instream = new ByteArrayInputStream(byteArray);
    try {
      servlet.copyRange(instream, ostream, start, end);
    } catch (Exception io) {
      throw new AssertionError(io);
    } finally {
      try {
        instream.close();
      } catch (IOException e) {
        // Ignore quietly
      }
    }
  }

  // Test copyRange with multiple size files
  @Test
  public void testCopyRange() {
    testCopyRangeMethod(new byte[2048], 0, 2048);
    testCopyRangeMethod(new byte[2049], 0, 2049);
    testCopyRangeMethod(new byte[2047], 0, 2047);
    testCopyRangeMethod(new byte[0], 0, 0);
  }
}
