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

package org.opencastproject.manager.system;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleContext;

import org.opencastproject.manager.api.PluginManagerConstants;

/**
 * This Class restarts the framework.
 * 
 * @author Leonid Oldenburger
 */
public class Restart {

	/**
	 * The bundle context
	 */
	private BundleContext bundleContext;

	/**
	 * Constructor
	 * 
	 * @param bundleContext
	 */
	public Restart(BundleContext bundleContext) {
		
		this.bundleContext = bundleContext;
	}
	
	/**
	 * Restarts the framework.
	 * 
	 * @throws IOException
	 */
	public void restart() throws IOException {
	            
		FileUtils.cleanDirectory(new File(PluginManagerConstants.FELIX_CACHE_PATH)); 
		
		final Bundle systemBundle = bundleContext.getBundle(0);

        Thread t = new Thread("Stopper") {
        	
            public void run() {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException ie) { }

                // stopping bundle 0 (system bundle) stops the framework
                try {
                    if (false) {
                        systemBundle.update();
                    } else {
                        systemBundle.stop();
                    }
                } catch (BundleException be) { }
            }
        };
        
        t.start();
	}
}