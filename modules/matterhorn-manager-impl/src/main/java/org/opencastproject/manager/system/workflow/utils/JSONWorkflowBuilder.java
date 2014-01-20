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

package org.opencastproject.manager.system.workflow.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import javax.xml.parsers.ParserConfigurationException;

import org.opencastproject.manager.api.workflow.WorkflowArtifact;
import org.opencastproject.manager.api.workflow.WorkflowInstallerService;
import org.opencastproject.manager.service.WorkflowInstallerServiceImpl;

/**
 * This class handles JSON objects for workflow's.
 * 
 * @author Leonid Oldenburger
 */
public class JSONWorkflowBuilder {	
	
	/**
	 * The bundle context
	 */
	private BundleContext bundleContext;
	
	/**
	 * The workflow's artifact hash map.
	 */
	private HashMap<String, WorkflowArtifact> workflowArtifactList;

	/**
	 * Constructor
	 * 
	 * @param bundleContext
	 */
	public JSONWorkflowBuilder(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}
	
	/**
	 * Creates HashMap with workflow's data.
	 * 
	 * @return string writer object
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public StringWriter createHashMapWorkflowDataFromXML() throws ParserConfigurationException, IOException {

	    ServiceReference sRef = bundleContext.getServiceReference(WorkflowInstallerService.class.getName());
	    
	    workflowArtifactList = null;
	    
	    if (sRef != null) {
	    	
	    	workflowArtifactList = ((WorkflowInstallerServiceImpl) bundleContext.getService(sRef)).getInstalledWorkflowArtifacts();
	    }
		
        StringWriter w = new StringWriter();

        for (String key : workflowArtifactList.keySet()) {
   			w.append("{\"id\":\"" + workflowArtifactList.get(key).getID() + "\", \"name\":\"" + workflowArtifactList.get(key).getFileName() + "\"}, ");
        }
        
		return w;
	}
}