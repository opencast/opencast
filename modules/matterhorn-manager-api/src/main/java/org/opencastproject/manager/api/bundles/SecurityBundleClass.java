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

package org.opencastproject.manager.api.bundles;

import java.util.LinkedList;

/**
 * This is a security bundle class.
 * 
 * @author Leonid Oldenburger
 */
public class SecurityBundleClass {
	
	/**
	 * The security bundle list
	 */
	private LinkedList<String> securityList = new LinkedList<String>();

	/**
	 * Constructor
	 */
	public SecurityBundleClass() {
		
		securityList.add("matterhorn-common-");
		securityList.add("matterhorn-dataloader-");
		securityList.add("matterhorn-db-");
		securityList.add("matterhorn-dublincore-");
		securityList.add("matterhorn-json-");
		securityList.add("matterhorn-kernel-");
		securityList.add("matterhorn-market-impl-");
		securityList.add("matterhorn-metadata-");
		securityList.add("matterhorn-metadata-api-");
		securityList.add("matterhorn-mpeg7-");
		securityList.add("matterhorn-runtime-info-ui-");
		securityList.add("matterhorn-series-service-api-");
		securityList.add("matterhorn-solr-");
		securityList.add("matterhorn-static-");
		securityList.add("matterhorn-userdirectory-jpa-");
		securityList.add("matterhorn-workflow-service-api-");
		securityList.add("matterhorn-workflow-service-impl-");
		securityList.add("matterhorn-working-file-repository-service-api-");
		securityList.add("matterhorn-working-file-repository-service-impl-");
		securityList.add("matterhorn-workspace-api-");
		securityList.add("matterhorn-workspace-impl-");
	}
	
	/**
	 * Returns security list.
	 * 
	 * @return security list
	 */
	public LinkedList<String> getSecurityList() {
		return securityList;
	}
}