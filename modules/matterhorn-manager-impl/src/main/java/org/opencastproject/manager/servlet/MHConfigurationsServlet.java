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

package org.opencastproject.manager.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencastproject.manager.api.PluginManagerConstants;
import org.opencastproject.manager.core.TemplateLoader;
import org.opencastproject.manager.core.TemplateWrapperFilter;
import org.opencastproject.manager.system.MHManagerActivator;
import org.opencastproject.manager.system.Restart;
import org.opencastproject.manager.system.configeditor.Config;
import org.opencastproject.manager.system.workflow.WorkflowManager;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * This class implements the HttpServlet.
 * 
 * @author Leonid Oldenburger
 */
public class MHConfigurationsServlet extends HttpServlet {
	
	/**
	 * Serial
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The bundle context
	 */
	private BundleContext bundleContext;
	
	/**
	 * The workflow's Manager
	 */
	private WorkflowManager workflowManager;
	
	/**
	 * The template
	 */
	private String template = "";
	
	/**
	 * The template loader
	 */
	private TemplateLoader loadedTemplate;
	
	private static final Logger logger = LoggerFactory.getLogger(MHManagerActivator.class);

	/**
	 * Class constructor
	 * 
	 * @param bundleContext
	 */
	public MHConfigurationsServlet(BundleContext bundleContext) {
		
		this.bundleContext = bundleContext;
		
		this.workflowManager = new WorkflowManager(this.bundleContext);
		
		this.loadedTemplate = new TemplateLoader();

		this.template = loadedTemplate.readTemplateFile("/ui-files/index.html");
	}
  
	/**
	 * Handle get requests and responses
	 * 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String uri = request.getRequestURI().replaceFirst("/config", "");
		
		if ("".equals(uri)) {
		
			response.setCharacterEncoding("utf-8");
			response.setContentType("text/html");
	      
			// variablen initialisieren
			HashMap<String, String> vars = new HashMap<String, String>(); 
			
			// create workflow vars
			vars = workflowManager.createWorkflowVars(vars);
	      
			request.setAttribute("template_var", vars); 
	
			response = new TemplateWrapperFilter(response, request);
			response.getWriter().println(template);

		}
		
		// REST
		else if (uri.startsWith("/rest")) {
	    	
	    	String path = uri.replaceFirst("/rest", "");
	    	
	    	String data = "";
	    	
	    	if (("").equals(path)) {
	    		
	    		data = Config.getConfigPathsAsJsonTree(null, "");
	    	
	    	}
	    	else {
	    		
	    		try {
	    	
	    			data = (new Config(null, path)).toJSON();
	    			
	    		} catch (JSONException e) {
	    			
					e.printStackTrace();
				
	    		}
	    	}
	    	
	    	response.setCharacterEncoding("utf-8");
	    	response.setContentType("application/json");
		    
	    	response.getWriter().println(data);
			
	    	
	    }
	    
	    else {
	    	response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	    }
	    
		
	}
  
	/**
	 * Handle post requests and responses
	 * 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		if (request.getRequestURI().startsWith("/config/rest")) {
			
			String path = request.getRequestURI().replaceFirst("/config/rest", "");
			
			// read request body and parse params as JSON
		  	StringBuffer body = new StringBuffer();
		  	String line;
		  	BufferedReader br = request.getReader();
		  	
		  	while ((line = br.readLine()) != null) {
		  		body.append(line);
		  	}
		  	
		  	try {
		  		
		  		JSONArray jsonArray = new JSONArray(body.toString());
		  		JSONObject json;
		  		
		  		Config config = new Config(null, path);
		  		
		  		for (int i = 0; i < jsonArray.length(); i++) {
		  			
		  			// [{"key":"profile.flash.http.name","value":"flash download","enabled":true,"prevKey":"prevKeyName"}]
		  			json = jsonArray.getJSONObject(i);
		  			
		  			config.addProperty(json.getString("key"), json.getString("value"), json.getBoolean("enabled"), json.getString("prevKey"));
		  			
		  		}
		  		
		  		config.save();
		  		logger.info("the config file " + config.getRelPath() + " was changed");
		  
		  	} catch (IOException e) {
				
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			
			} catch (JSONException e) {
				
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			
			}
		}
		
		else {
			try {
				// handle workflow operations
				workflowManager.handleWorkflowOperations(request, response);
			} catch (TransformerException e) { 
			} catch (ParserConfigurationException e) { 
			} catch (SAXException e) { }
			
	    	// handle restart
			String pluginState = request.getParameter(PluginManagerConstants.PLUGIN_STATE);
			
			if (pluginState != null) {
			
				if (pluginState.equals(PluginManagerConstants.RESTART_SYSTEM)) {

					Restart restart = new Restart(bundleContext);
					restart.restart();
				}
			}	
		}
	}
	
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
	  	String path = req.getRequestURI().replaceFirst("/config/rest", "");
	  	
	  	// read request body and parse params as JSON
	  	StringBuffer body = new StringBuffer();
	  	String line;
	  	BufferedReader br = req.getReader();
	  	
	  	while ((line = br.readLine()) != null) {
	  		body.append(line);
	  	}
	  	
	  	try {
	  		
	  		JSONArray jsonArray = new JSONArray(body.toString());
	  		JSONObject json;
	  		
	  		Config config = new Config(null, path);
	  		
	  		for (int i = 0; i < jsonArray.length(); i++) {
	  			
	  			// [{"key":"profile.flash.http.name","value":"flash download","enabled":true}]
	  			json = jsonArray.getJSONObject(i);
	  			
	  			config.updateProperty(json.getString("key"), json.getString("value"), json.getBoolean("enabled"));
	  			
	  		}
	  		
	  		config.save();
	  		logger.info("the config file " + config.getRelPath() + " was changed");
	  
	  	} catch (IOException e) {
			
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		
		} catch (JSONException e) {
			
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		
		}
	  	
	}
}