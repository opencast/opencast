var requestHandler = function() {
	
	// send plug-in state
	this.sendPostRequestWithPluginData = function(id, pluginName, pluginVersion, pluginState) {		
		
		var id = id;
		var pluginName = pluginName;
		var pluginVersion = pluginVersion;
		var pluginState = pluginState;
		
		// Browserkompatibles Request-Objekt erzeugen:
		r = null;
	 
		if (window.XMLHttpRequest) {
			r = new XMLHttpRequest();
		}
		else if (window.ActiveXObject) {
			try {
				r = new ActiveXObject('Msxml2.XMLHTTP');
			} catch(e1) {
				try {
					r = new ActiveXObject('Microsoft.XMLHTTP');
				} catch(e2) {
					//document.getElementById('activatorHandler').innerHTML = 
					alert("Request nicht möglich.");
				}
			}		
		}
	 
		// Wenn Request-Objekt vorhanden, dann Anfrage senden:
		if (r != null) {
			// HTTP-POST
			r.open('POST', '/config/', true);
			r.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
			r.onreadystatechange = function () {
				if (r.readyState==4 && r.status==200) {
					installationComplete(r.responseText);
				}
			};
			r.send('id=' + id + '&plugin_name=' + pluginName + '&plugin_version=' + pluginVersion + '&plugin_state=' + pluginState);
		}
	}
	
	// upload zip request
	this.sendPostRequestWithZippedPluginData = function() {
		
		// Browserkompatibles Request-Objekt erzeugen:
		r = new XMLHttpRequest();
		
		var file = document.getElementById("sampleFile");
		
        var sampleFile = file.files[0];
        
        var formdata = new FormData();
        formdata.append("sampleFile", sampleFile);
        
		// Wenn Request-Objekt vorhanden, dann Anfrage senden:
		if(r != null) {
			// HTTP-POST
			r.open('POST', '/config#/upload', true);
			r.setRequestHeader("plugin_state", $("input:radio:checked").val());
			r.send(formdata);
	        r.onload = function(e) {

	            if (this.status == 200) {
	            	window.location.reload();
	            }
	        };
		}
	}
	
	// workflow requests 
	this.sendXMLFileAsRequest = function() {
		
		var txt = editor.getValue();
		
		if (this.validateXML(txt)) {
		} else {
			return "XML is not valid!";
		}
		
		var xml = this.stringToXML(txt);
		var fileName = $('#workflow_selection :selected').text();

		var id = $(xml).find('id').text();
		
		if (id == "" || id == "undefined") {
			return "ID is undefined!";
		}
		
		var workflow = ""; 
		
		$.each(workflow_data, function(i, item) {
		 	if (item.id == id) {
		 		workflow = item.name;
		 		return false;
		 	} 
		});
		
		if (workflow != "") {
			if (workflow != fileName) {
				return "ID already in use!"; 
			}
		}
				
		// Browserkompatibles Request-Objekt erzeugen:
		r = null;
	 
		if(window.XMLHttpRequest) {
			r = new XMLHttpRequest();
		} else if(window.ActiveXObject) {
			try {
				r = new ActiveXObject('Msxml2.XMLHTTP');
			} catch(e1) {
				try {
					r = new ActiveXObject('Microsoft.XMLHTTP');
				} catch(e2) { }
			}		
		}
	 
		// Wenn Request-Objekt vorhanden, dann Anfrage senden:
		if(r != null) {
			r.open("POST", '/config#/editor/', true);  
			r.setRequestHeader("Content-Type", "text/xml");
			r.setRequestHeader("FileName", fileName);
			r.send(xml);  
		}
		
		return "ok";
	}
	
	this.stringToXML = function(oString) {
		//code for IE
		if (window.ActiveXObject) {
			var oXML = new ActiveXObject("Microsoft.XMLDOM"); 
			oXML.loadXML(oString);
			return oXML;
		}
		// code for Chrome, Safari, Firefox, Opera, etc.
		else {
			return (new DOMParser()).parseFromString(oString, "text/xml");
		}
	}
	
	this.sendPostRequestWithWorkflowFileName = function() {	
		
		var workflowFile = $('#workflow_selection :selected').text();
		
		if (workflowFile == "" || workflowFile == null) return;
		
		// Browserkompatibles Request-Objekt erzeugen:
		r = null;
	 
		if(window.XMLHttpRequest) {
			r = new XMLHttpRequest();
		} else if(window.ActiveXObject) {
			try {
				r = new ActiveXObject('Msxml2.XMLHTTP');
			} catch(e1) {
				try {
					r = new ActiveXObject('Microsoft.XMLHTTP');
				} catch(e2) { }
			}		
		}
	 
		// Wenn Request-Objekt vorhanden, dann Anfrage senden:
		if(r != null) {
			// HTTP-POST
			r.open('POST', '/config#/editor/', true);
			r.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
			r.send('workflow_file=' + workflowFile);
			r.onreadystatechange = function () {
			
				if (r.readyState==4 && r.status==200) {
					
					editor.markClean();
					editor.setValue(r.responseText);
		        }
   			};
		}
	}
	
	this.validateXML = function(txt) {
		// code for IE
		if (window.ActiveXObject) {
			
		  var xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
		  xmlDoc.async="false";
		  xmlDoc.loadXML(txt);
		
		  if(xmlDoc.parseError.errorCode!=0) {
		    return false;
		  } else {
		    return true;
		  }
		// code for Mozilla, Firefox, Opera, etc.
		} else if (document.implementation.createDocument) {
			
			var parser=new DOMParser();
			var xmlDoc=parser.parseFromString(txt,"text/xml");
		
			if (xmlDoc.getElementsByTagName("parsererror").length>0) {
			    return false;
			} else {
			    return true;
			}
		} else {
			 return false;
		}
	}
	
	this.sendNewWorkflowFile = function(wFile) {
		// Browserkompatibles Request-Objekt erzeugen:
		r = null;
	 
		if(window.XMLHttpRequest) {
			r = new XMLHttpRequest();
		} else if(window.ActiveXObject) {
			try {
				r = new ActiveXObject('Msxml2.XMLHTTP');
			} catch(e1) {
				try {
					r = new ActiveXObject('Microsoft.XMLHTTP');
				} catch(e2) { }
			}		
		}
		
		// Wenn Request-Objekt vorhanden, dann Anfrage senden:
		if(r != null) {
			// HTTP-POST
			r.open('POST', '/config#/editor/', true);
			r.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
			r.send('new_workflow_file=' + wFile);
			r.onreadystatechange = function () {
			
				if (r.readyState==4 && r.status==200) {}
   			};
		}
		$('#workflow_selection').append($('<option></option>').val(wFile+".xml").text(wFile+".xml"));
		$('#workflow_selection').val(wFile+".xml");
		editor.markClean();
		editor.setValue("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
						+ "<definition xmlns=\"http://workflow.opencastproject.org\">\n"
						+ "<id>" + wFile + "</id>\n"
						+ "<operations>\n"
						+ "<operation>"
						+ "</operation>\n"
						+ "</operations>\n"
						+ "</definition>\n");
		
		message.closeWorkflowFileView();
	}
	
	this.deleteWorkflowFile = function() {
		
		var workflowFile = $('#workflow_selection :selected').text();
		
		// Browserkompatibles Request-Objekt erzeugen:
		r = null;
		
		if(window.XMLHttpRequest) {
			r = new XMLHttpRequest();
		} else if(window.ActiveXObject) {
			try {
				r = new ActiveXObject('Msxml2.XMLHTTP');
			} catch(e1) {
				try {
					r = new ActiveXObject('Microsoft.XMLHTTP');
				} catch(e2) { }
			}		
		}
		
		// Wenn Request-Objekt vorhanden, dann Anfrage senden:
		if(r != null) {
			// HTTP-POST
			r.open('POST', '/config#/editor/', true);
			r.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
			r.send('delete_workflow_file=' + workflowFile);
			r.onreadystatechange = function () {
			
				if (r.readyState==4 && r.status==200) {
				}
			};
			$("#workflow_selection option:selected").remove();
		}
	}
	// end workflow
}
