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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * This class represents a template loader.
 * 
 * @author Leonid Oldenburger
 */
public final class TemplateLoader {

	/**
	 * constructor
	 */
	public TemplateLoader() { }
	
    /**
     * Returns the template (HTML) as string.
     * 
     * @return template as string 
     */
	public String readTemplateFile(final String templateFile) {
		
		InputStream templateStream = this.getClass().getResourceAsStream(templateFile);
	      
		if (templateStream != null) {
			try {
				String str = IOUtils.toString(templateStream, "UTF-8");
				switch (str.charAt(0)) { 
					case 0xFEFF: // UTF-16/UTF-32, big-endian
					case 0xFFFE: // UTF-16, little-endian
					case 0xEFBB: // UTF-8
					return str.substring(1);
					default:
					break;
	          }
	          return str;
			} catch (IOException e) { 
			} finally {
	    	  IOUtils.closeQuietly(templateStream);
	      	}
		}
	  // no template file
	  return "";
	}
}