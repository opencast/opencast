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

package org.opencastproject.manager.service;

import java.io.File;
import java.util.HashMap;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.opencastproject.manager.api.workflow.WorkflowArtifact;
import org.opencastproject.manager.api.workflow.WorkflowInstallerService;
import org.opencastproject.manager.core.MetadataDocumentHandler;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * This class implements the WorkflowInstallerServer and the ArtifactInstaller.
 * 
 * @author Leonid Oldenburger
 */
public class WorkflowInstallerServiceImpl implements WorkflowInstallerService, ArtifactInstaller {

	/**
	 * Logger
	 */
	private static final Logger log = LoggerFactory.getLogger(WorkflowInstallerServiceImpl.class);
	
	/**
	 * The artifacts hash map.
	 */
	private HashMap<String, WorkflowArtifact> artifacts = new HashMap<String, WorkflowArtifact>();
	
	/**
	 * The Document Handler
	 */
	private MetadataDocumentHandler handleDocument = new MetadataDocumentHandler();
	
    /**
     * Sets the activation.
     * 
     * @param component context
     */
	protected synchronized void activate(ComponentContext cc) throws Exception { }

    /**
     * Sets the de-activation.
     * 
     * @param component context
     */
	protected void deactivate(ComponentContext cc) { }
  
    /**
     * Returns the artifacts hash map.
     */
	@Override
	public HashMap<String, WorkflowArtifact> getInstalledWorkflowArtifacts() {	
		return (HashMap<String, WorkflowArtifact>) artifacts.clone();
	}

    /**
     * Sets the installed file.
     * 
     * @param file
     * @throws Exception
     */
	@Override
	public void install(File file) throws Exception {
        
        if (file.getAbsolutePath().contains("etc/workflows")) {
            Document doc = handleDocument.getDocumentBuilder().parse(file); 
            doc.getDocumentElement().normalize();
            WorkflowArtifact workflowArtifact = new WorkflowArtifact();
            workflowArtifact.setFileName(file.getName());
            String id = doc.getDocumentElement().getElementsByTagName("id").item(0).getTextContent();
            workflowArtifact.setID(id);

        	artifacts.put(file.getName(), workflowArtifact);
		}
	}

    /**
     * Sets the updated file.
     * 
     * @param file
     * @throws Exception
     */
	@Override
	public void update(File file) throws Exception {
		
		File fileTMP = file;
		uninstall(file);
		install(fileTMP);
	}
	
    /**
     * Sets the un-installe file.
     * 
     * @param file
     * @throws Exception
     */
	@Override
	public void uninstall(File file) throws Exception {
		
		if (file.getAbsolutePath().contains("etc/workflows")) {
			artifacts.remove(file.getName());
        }
	}

    /**
     * Handle file.
     * 
     * @param file
     */
	@Override
	public boolean canHandle(File file) {

		File parent = new File(file.getParent());
		if (parent.getAbsolutePath().contains("etc/workflows")) {
			return true;
		}
		
		return false;
	}
}