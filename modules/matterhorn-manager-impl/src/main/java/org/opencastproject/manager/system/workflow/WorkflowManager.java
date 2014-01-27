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

package org.opencastproject.manager.system.workflow;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.util.HashMap;
import java.io.File;
import java.io.FileNotFoundException;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.opencastproject.manager.api.PluginManagerConstants;
import org.opencastproject.manager.core.MetadataDocumentHandler;
import org.opencastproject.manager.system.workflow.utils.JSONWorkflowBuilder;
import org.osgi.framework.BundleContext;
import org.xml.sax.SAXException;
import org.apache.commons.io.FileUtils;

/**
 * This class represents the worflow's manager.
 *
 * @author Leonid Oldenburger
 */
public class WorkflowManager {
	
	/**
	 * The bundle context
	 */
	private BundleContext bundleContext;
	
	/**
	 * The Document handler
	 */
	private MetadataDocumentHandler handleDocument;

	/**
	 * The workflow's manager constructor.
	 * 
	 * @param bundleContext
	 */
	public WorkflowManager(BundleContext bundleContext) {
		
		this.bundleContext = bundleContext;
	}
	
	/**
	 * Returns the workflow's variable in hash map.
	 * 
	 * @param hash map
	 * @return hash map
	 * @throws ServletException
	 * @throws IOException
	 */
	public HashMap<String, String> createWorkflowVars(HashMap<String, String> map) throws ServletException, IOException {
	     
		StringWriter w = null;
	      
		HashMap<String, String> vars = map;
	    
		try {
			JSONWorkflowBuilder jsonObject = new JSONWorkflowBuilder(bundleContext);
		
			w = jsonObject.createHashMapWorkflowDataFromXML();
			vars.put("workflow_data", "[" + w.toString() + "]");
	    	
		} catch (ParserConfigurationException e) { 
		} catch (SAXException e) { }

		return vars;
	}
	
	/**
	 * Handles the workflow's operations.
	 * 
	 * @param request
	 * @param response
	 * @throws TransformerException
	 * @throws ServletException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public void handleWorkflowOperations(HttpServletRequest request, HttpServletResponse response) throws TransformerException, ServletException, ParserConfigurationException, SAXException, IOException {
		
		String contentTyp = "text/xml; charset=UTF-8";
		String newWorkflowFile = request.getParameter("new_workflow_file");
		String deleteWorkflowFile = request.getParameter("delete_workflow_file");
		
		if (newWorkflowFile != null) {
			try {
				createNewWorkflowFile(newWorkflowFile);
			} catch (TransformerException e) { }
		}
		
		if (deleteWorkflowFile != null) {
			try {
				deleteWorkflowFile(deleteWorkflowFile);
			} catch (TransformerException e) { }
		}
		
		if (contentTyp.contains(request.getContentType())) {
			
			try {
				handleNewWorkflowFile(request, response);
			} catch (TransformerException e) { 
			} catch (ParserConfigurationException e) { 
			} catch (SAXException e) { }
		}
		
		if (request.getParameter("workflow_file") != null) {
			handleWorkflowFiles(request, response);
		}
	}
	
	/**
	 * Handles the new worklfow file.
	 * 
	 * @param request
	 * @param response
	 * @throws TransformerException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public void handleNewWorkflowFile(HttpServletRequest request, HttpServletResponse response) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		
        File xmlWorkflowFile = new File(PluginManagerConstants.WORKFLOWS_PATH + "TMPfile.xml");
        
        int counter = 0;
        
        try {
                InputStreamReader inputStream = new InputStreamReader(request.getInputStream());
                FileOutputStream outputStream = new FileOutputStream(xmlWorkflowFile);

                int c = 0;
		
		         while ((c = inputStream.read()) != -1) {
		                 outputStream.write(c);
		                        counter++;
		         }
        
		         inputStream.close();
		         outputStream.close();
		 } catch (FileNotFoundException e) {
			 System.err.println("FileStreamsTest: " + e);
		 } catch (IOException e) {
			 System.err.println("FileStreamsTest: " + e);
		 }
        
        if (counter == 0) {
                xmlWorkflowFile.delete();
                return;
        }

        String newXMLFile = stringValidator(request.getHeader("FileName"));
        File newXMLWorkflowFile = new File(PluginManagerConstants.WORKFLOWS_PATH + newXMLFile);
        newXMLWorkflowFile.delete();
        xmlWorkflowFile.renameTo(new File(PluginManagerConstants.WORKFLOWS_PATH + newXMLFile));
	}
	
	/**
	 * Create a new workflow's file.
	 * 
	 * @param new workflow's file
	 * @throws TransformerException
	 * @throws IOException
	 */
	public void createNewWorkflowFile(String newWorkflowFile) throws TransformerException, IOException {

		newWorkflowFile = stringValidator(newWorkflowFile);
		File xmlWorkflowFile = new File(PluginManagerConstants.WORKFLOWS_PATH + newWorkflowFile + ".xml");
		String inStream = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<definition xmlns=\"http://workflow.opencastproject.org\">\n"
				+ "<id>" + newWorkflowFile + "</id>\n"
				+ "<operations>\n"
				+ "<operation>"
				+ "</operation>\n"
				+ "</operations>\n"
				+ "</definition>\n";
		FileUtils.writeStringToFile(xmlWorkflowFile, inStream);
	}
	
	/**
	 * Delete the workflow's file.
	 * 
	 * @param delete workflow's file
	 * @throws TransformerException
	 * @throws IOException
	 */
	public boolean deleteWorkflowFile(String deleteWorkflowFile) throws TransformerException, IOException {

		deleteWorkflowFile = stringValidator(deleteWorkflowFile);
		File xmlWorkflowFile = new File(PluginManagerConstants.WORKFLOWS_PATH + deleteWorkflowFile);
		
		return xmlWorkflowFile.delete();
	}
	
	/**
	 * RegEx handler
	 * 
	 * @param string
	 * @return string
	 */
	public String stringValidator(String string) {
		
	     String fileName = string;
	     fileName = fileName.replaceAll("[^a-z0-9-_. ]", "");
	     
	     int length = fileName.length();
	     
	     if (length > 50) {
	    	 length = 50;
	     }
	     return fileName.substring(0, length);
	}
	
	/**
	 * Handle workflow's files.
	 * 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public void handleWorkflowFiles(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	     String fileName = stringValidator((String) request.getParameter("workflow_file"));
	     String empty = ""; 
	     
	     if (empty.equals(fileName) || fileName == null)
	           throw new ServletException(
	            "Invalid or non-existent file parameter in SendXml servlet.");
	          
	     String xmlDir = PluginManagerConstants.WORKFLOWS_PATH;

	     if (empty.equals(xmlDir) || xmlDir == null)
	           throw new ServletException(
	             "Invalid or non-existent xmlDir context-param.");

	     ServletOutputStream stream = null;
	     BufferedInputStream buf = null;
	     
	     try {
		     stream = response.getOutputStream();
		     File xml = new File(xmlDir + fileName);
		     
		     response.setContentType("text/xml");
		     response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
		     response.setContentLength((int) xml.length());
		     
		     FileInputStream input = new FileInputStream(xml);
		     
		     buf = new BufferedInputStream(input);
		     
		     int readBytes = 0;
	
		     //read from the file; write to the ServletOutputStream
		     while ((readBytes = buf.read()) != -1) {
		        stream.write(readBytes);
		     }

	     } catch (IOException ioe) {
	        throw new ServletException(ioe.getMessage());
	     } finally {

			     if (stream != null)
			         stream.close();
			     
			      if (buf != null)
			          buf.close();
	     }
	}
}