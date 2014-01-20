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

package org.opencastproject.manager.api.workflow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This class represents a WorkflowArtifact.
 * 
 * @author Leonid Oldenburger
 */
@XmlType(name = "workflow-artifact", namespace = "http://workflow-artifact.opencastproject.org")
@XmlRootElement(name = "workflow-artifact", namespace = "http://workflow-artifact.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class WorkflowArtifact {
  
    /**
     * The file's name.
     */
	@XmlElement
	private String fileName;
	
    /**
     * The workflow's id.
     */
	@XmlElement
	private String id;
	
    /**
     * Sets the workflow's file name.
     */
	public void setFileName(String fileName) {
		 this.fileName = fileName;
	}
	
    /**
     * Returns the workflow's file name.
     * 
     * @return the file name
     */
	public String getFileName() {
		return fileName;
	}
	
    /**
     * Sets the workflow's id.
     */
	public void setID(String id) {
		this.id = id;
	}
	
    /**
     * Returns the workflow's id.
     * 
     * @return the id
     */
	public String getID() {
		return this.id;
	}
}