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
package org.opencastproject.manager.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.manager.api.PluginManagerConstants;
import org.opencastproject.manager.system.workflow.WorkflowManager;
import org.xml.sax.SAXException;

/**
 * This class is for workflow's tests.
 */
public class WorkflowManagerTest {
  
  private HttpServletRequest request;
  
  private HttpServletResponse response;

  private WorkflowManager manager;
  
  /**
   * Method starts before tests.
   */
  @Before
  public void testCreateWorkflow() {

    this.manager = new WorkflowManager(null);
    
    String fileName = "tmpworkflow";
    
    // mocks for request and response
    request = EasyMock.createMock(HttpServletRequest.class);
    response = EasyMock.createMock(HttpServletResponse.class);
    
    // create new workflow
    EasyMock.expect(request.getParameter("workflow_file")).andReturn(null).anyTimes();
    EasyMock.expect(request.getParameter("new_workflow_file")).andReturn(fileName).anyTimes();
    EasyMock.expect(request.getParameter("delete_workflow_file")).andReturn(null).anyTimes();
    EasyMock.expect(request.getContentType()).andReturn("***").anyTimes();
    
    EasyMock.replay(request);
    
    try {
      // create file
      manager.handleWorkflowOperations(request, response);
      
    } catch (TransformerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ServletException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  /**
   * Method starts after tests.
   */
  @After
  public void deleteWorkflowTest() {

    String fileName = "tmpworkflow";
    
    // mocks for request and response
    request = EasyMock.createMock(HttpServletRequest.class);
    response = EasyMock.createMock(HttpServletResponse.class);
    
    // for delete new workflow
    EasyMock.expect(request.getParameter("workflow_file")).andReturn(null).anyTimes();
    EasyMock.expect(request.getParameter("new_workflow_file")).andReturn(null).anyTimes();
    EasyMock.expect(request.getParameter("delete_workflow_file")).andReturn(fileName).anyTimes();
    EasyMock.expect(request.getContentType()).andReturn("***").anyTimes();
    
    EasyMock.replay(request);
    
    try {
      // delete file
      manager.handleWorkflowOperations(request, response);
      // try to delete file
      Assert.assertFalse(manager.deleteWorkflowFile(fileName));
      
    } catch (TransformerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ServletException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  /**
   * This method test the creation of worflow file functionality.
   */
  @Test
  public void createWorfklowFileTest() {

    this.manager = new WorkflowManager(null);
    
    String xmlContent  = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><definition xmlns=\"http://workflow.opencastproject.org\"><id>unit-test</id><operations><operation></operation></operations></definition>";
    
    String fileName = "tmpworkflow";
    
    // mocks for request and response
    request = EasyMock.createMock(HttpServletRequest.class);
    response = EasyMock.createMock(HttpServletResponse.class);
    
    // for create new workflow
    EasyMock.expect(request.getParameter("workflow_file")).andReturn(null).anyTimes();
    EasyMock.expect(request.getParameter("new_workflow_file")).andReturn(null).anyTimes();
    EasyMock.expect(request.getParameter("delete_workflow_file")).andReturn(null).anyTimes();
    EasyMock.expect(request.getHeader("FileName")).andReturn(fileName).anyTimes();
    
    try {
      EasyMock.expect(request.getInputStream()).andReturn(new MockServletInputStream(xmlContent)).anyTimes();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
  
    EasyMock.expect(request.getContentType()).andReturn("text/xml; charset=UTF-8").anyTimes();
    
    EasyMock.replay(request);
    
    try {
      // handle file
      manager.handleWorkflowOperations(request, response);
      
      // test for creation of new workflow file
      manager.createNewWorkflowFile(request.getHeader("FileName"));
      
      File file = new File(PluginManagerConstants.WORKFLOWS_PATH + fileName);
      
      // was file created? 
      Assert.assertTrue(file.exists());
      // end of test for new file creation
    } catch (TransformerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ServletException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  /**
   * This method handles the XML content of the workflow file.
   */
  @Test
  public void handleWorfkflowFileTest() {
    
    this.manager = new WorkflowManager(null);
    
    String fileName = "tmpworkflow.xml";
    
    String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><definition xmlns=\"http://workflow.opencastproject.org\"><id>unit-test</id><operations><operation></operation></operations></definition>";

    // mocks for request and response
    request = EasyMock.createMock(HttpServletRequest.class);
    response = EasyMock.createMock(HttpServletResponse.class);
    
    // for create new workflow
    EasyMock.expect(request.getParameter("workflow_file")).andReturn(null).anyTimes();
    EasyMock.expect(request.getParameter("new_workflow_file")).andReturn(null).anyTimes();
    EasyMock.expect(request.getParameter("delete_workflow_file")).andReturn(null).anyTimes();
    EasyMock.expect(request.getHeader("FileName")).andReturn(fileName).anyTimes();
    
    try {
      EasyMock.expect(request.getInputStream()).andReturn(new MockServletInputStream(xmlContent)).anyTimes();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
  
    EasyMock.expect(request.getContentType()).andReturn("text/xml; charset=UTF-8").anyTimes();
    
    EasyMock.replay(request);
    
    try {
      // handle file
      manager.handleWorkflowOperations(request, response);
      
      // test for handle a new workflow content
      manager.handleNewWorkflowFile(request, response);
      
      File file = new File(PluginManagerConstants.WORKFLOWS_PATH + fileName);
      Assert.assertTrue(file.exists());
      
      // content really saved? 
      Assert.assertTrue(getContentFromFile(file).equals(xmlContent));
      
    } catch (TransformerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ServletException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } 
  }
  
  /**
   * Method test delete workflow file.
   */
  @Test
  public void deleteWorkflowFileTest() {
    
    this.manager = new WorkflowManager(null);
    
    String fileName = "tmpworkflow";
    String xmlContent  = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><definition xmlns=\"http://workflow.opencastproject.org\"><id>unit-test</id><operations><operation></operation></operations></definition>";

    // mocks for request and response
    request = EasyMock.createMock(HttpServletRequest.class);
    response = EasyMock.createMock(HttpServletResponse.class);
    
    // for create new workflow
    EasyMock.expect(request.getParameter("workflow_file")).andReturn(null).anyTimes();
    EasyMock.expect(request.getParameter("new_workflow_file")).andReturn(null).anyTimes();
    EasyMock.expect(request.getParameter("delete_workflow_file")).andReturn(null).anyTimes();
    EasyMock.expect(request.getHeader("FileName")).andReturn(fileName).anyTimes();
    
    try {
      EasyMock.expect(request.getInputStream()).andReturn(new MockServletInputStream(xmlContent)).anyTimes();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    
    EasyMock.expect(request.getContentType()).andReturn("text/xml; charset=UTF-8").anyTimes();
    EasyMock.replay(request);
    
    try {
      // handle file
      manager.handleWorkflowOperations(request, response);

      // test for delete file
      Assert.assertTrue(manager.deleteWorkflowFile(request.getHeader("FileName")));
  
    } catch (TransformerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ServletException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  /**
   * This method returns ServletInputStream.
   * 
   * @param object
   * @return ServletInputStream
   * @throws Exception
   */
  public static ServletInputStream createServletInputStream(Object object) throws Exception {
               
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream os = new ObjectOutputStream(baos);
    os.writeObject(object);
    
    final InputStream bais = new ByteArrayInputStream(baos.toByteArray());

    return new ServletInputStream() {

      @Override
      public int read() throws IOException {
      
        return bais.read();
      }
    };  
  }
  
  /**
   * Method return XML content from workflow file.
   * 
   * @param xml
   * @return string
   * @throws Exception
   * @throws ServletException
   */
  public static String getContentFromFile(File xml) throws Exception, ServletException {
    
       ServletOutputStream stream = null;
       BufferedInputStream buf = null;
       String s = "";

       try {
         FileInputStream input = new FileInputStream(xml);

         buf = new BufferedInputStream(input);
         
         int readBytes = 0;
         
         // byte array to store input
         byte[] contents = new byte[1024];
         
         while ((readBytes = buf.read(contents)) != -1) {
           s = new String(contents, 0, readBytes);
         }

       } catch (IOException ioe) {

          throw new ServletException(ioe.getMessage());
       } finally {

           if (stream != null)
               stream.close();
           
            if (buf != null)
                buf.close();
       }

       return s;
    
  }
  
  /**
   * Inner Class to handle String to ServletInputStream.
   */
  public class MockServletInputStream extends ServletInputStream {
    
        private final String body;
        private int index = 0;

        /**
         * Constructor
         * 
         * @param body
         */
        public MockServletInputStream(String body) {
            this.body = body;
        }

        /**
         * 
         * @return 
         * @throws IOException
         */
        public int read() throws IOException {
            if (index == body.length()) {
                return -1;
            }

            return body.codePointAt(index++);
        }
    }

}