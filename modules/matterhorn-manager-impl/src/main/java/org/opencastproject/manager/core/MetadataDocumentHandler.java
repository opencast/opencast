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

package org.opencastproject.manager.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class represents meta data document handler.
 * 
 * @author Leonid Oldenburger
 */
public class MetadataDocumentHandler {

    /**
     * Returns the document builder factory object.
     * 
     * @return the document builder factory object
     * @throws ParserConfigurationException
     */
	public DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		return dbFactory.newDocumentBuilder();
	}
	
    /**
     * Returns the document object.
     * 
     * @return the document object
     * @throws ParserConfigurationException
     */
	public Document getNewDocument() throws ParserConfigurationException {
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		return dbFactory.newDocumentBuilder().newDocument();
	}
	
	/**
	 * Sets data in a XML-file.
	 * 
	 * @throws TransformerFactoryConfigurationError 
	 * @throws TransformerException 
	 */
	public synchronized void writeNewPluginNodesInFile(File file, Document doc) throws TransformerFactoryConfigurationError, TransformerException {
		
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		//initialize StreamResult with File object to save to file
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);

		String xmlString = result.getWriter().toString();
	
		//writing to file
		FileOutputStream fop = null;
		  
		try {
		    fop = new FileOutputStream(file);
		    
		    byte[] contentInBytes = xmlString.getBytes();
		
		    fop.write(contentInBytes);
		    fop.flush();
		    fop.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		      try {
		          if (fop != null) {
		              fop.close();
		          }
		      } catch (IOException e) {
		          e.printStackTrace();
		      }
		}
	}
	
	/**
	 * Returns node value. 
	 * 
	 * @param tag
	 * @param element
	 * @param k index
	 * @return node value
	 */
	public String getValue(String tag, Element element, int k) {
		
		NodeList nodes = element.getElementsByTagName(tag).item(k).getChildNodes();
		Node node = (Node) nodes.item(0);
		return node.getNodeValue();
	}
	
	/**
	 * Copy files. 
	 * 
	 * @param srcFile
	 * @param srcDir
	 */
	public void moveDocumentDirectory(File srcFile, File destDir) {
		
		try {
            FileUtils.forceMkdir(destDir);
            FileUtils.copyDirectoryToDirectory(srcFile, destDir);
            FileUtils.deleteDirectory(srcFile);
        } catch (Exception e) { }
	}
	
	/**
	 * Returns files byte array. 
	 * 
	 * @param file
	 * @return byte array
	 * @throws IOException
	 */
	public byte[] loadFile(File file) throws IOException {
	
		InputStream is = new FileInputStream(file);
		 
		long length = file.length();
		
		if (length > Integer.MAX_VALUE) {
		// File is too large
		}
		
		byte[] bytes = new byte[(int)length];
		int offset = 0;
		int numRead = 0;
		
		while (offset < bytes.length && numRead >= 0) {
			offset += numRead;
			numRead = is.read(bytes, offset, bytes.length - offset);
		}
		 
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file " + file.getName());
		}
		 
		is.close();
		
		return bytes;
	}
	
	/**
	 * Returns boolean if copying file is done. 
	 * 
	 * @param file name
	 * @param file URL
	 * @return boolean
	 * @throws IOException
	 */
	public Boolean copyFileFromURL(String fileName, String fileUrl) throws IOException {
		
		try {
			FileUtils.copyURLToFile(new URL(fileUrl), new File(fileName));
		} catch (FileNotFoundException ex) {
			return false;
		} catch (MalformedURLException ex) {
			return false;
		}
		return true;
	}
	
	/**
	 * Returns date. 
	 * 
	 * @param file
	 * @return date
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public Date getDateFromFile(File dateFile) throws IOException, FileNotFoundException {
		
		FileReader namereader = new FileReader(dateFile);
		BufferedReader in = new BufferedReader(namereader);
		String unixTime = in.readLine();
		
		return new Date((Long.parseLong(unixTime) * 1000));
	}
}