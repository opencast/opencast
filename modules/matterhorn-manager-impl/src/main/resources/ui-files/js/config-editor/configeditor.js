/**
 *  Copyright 2009 The Regents of the University of California
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

	$(function() {	
		$('#messageRestart').load('/ui-files/templates/restartMessage.tpl');
		initData();
	});

	
	function initData() {
		// file tree clicked
		$('#configsTree').fileTree({owner: "config"}, function(file) {
			if ((this.changedProperties && this.changedProperties.length != 0) || (this.newProperties && this.newProperties.length != 0)) {
				
				var thisClass = this;
				
				$("#dialog-reload").attr("title", "The config was changed!");
				$("#dialog-message").text("The config file was changed.");
				
				$("#dialog-reload").dialog({
					resizable: false,
					height:140,
					modal: true,
					buttons: {
							"Save": function() {
								thisClass.saveConfig();
								$(this).dialog("close");
								thisClass.loadConfig(file);
							},
					
							"Discard changes": function() {
								$(this).dialog("close");
								thisClass.loadConfig(file);
							},
					
							Cancel: function() {
								$(this).dialog("close");
							}
						}
					});
			}
			else {
				
				this.loadConfig(file);
			
			}
        });
	}
	
	
	function loadConfig(file) {
		
		this.configName = file;
		
		if (!file) {
		
			this.configName = this.configPath;
		
		}
		
		var thisClass = this;
		
		$.ajax({
				url:'/config/rest' + this.configName,
				complete: function(data) {
					thisClass.configJSON = JSON.parse(data.responseText); 
					thisClass.buildConfig();
				}
		});
	}
	
	function updateConfig() {
		$(".highlight").removeAttr("class");
	}

	function saveConfig() {
		
		var properties = $.unique(this.changedProperties);
		
		if (properties.length == 0 && this.newProperties.length == 0) {
			
			alert("Nothing to save!");
			return false;
		
		}
		
		var thisClass = this;
		
		// add new properties
		if (this.newProperties.length != 0) {
		
			$.ajax({
				type: 'POST',
				dataType:'JSON',
				contentType: 'application/json',
				url:'/config/rest' + this.configName, 
				data: JSON.stringify(thisClass.newProperties),
				success: function(data, textStatus, xhr) {
					if (xhr.status == 200) {
					    //console.log("putted new prop");
						thisClass.newProperties = [];
						thisClass.updateConfig();
					}
					else {
						alert("Failed to save the config!");
					}
				}
			});
		}
		
		// update changed properties
		if (properties.length != 0) {
			var json = [];
			var property;
			var id;
			
			for (p in properties) {
				
				property = {};
				
				key = properties[p];
				id = key.replace(/\./g, '_');
				
				property.key = key;
				property.value = $('#' + id + '-value').val();
				property.enabled = $('#' + id + '-enabled').is(':checked');
				
				json.push(property);
			
			}
			
			$.ajax({
				
				type: 'PUT',
				dataType:'JSON',
				contentType: 'application/json',
				url:'/config/rest' + this.configName, 
				data: JSON.stringify(json),
				success: function(data, textStatus, xhr) {
				    if (xhr.status == 200) {
						//console.log("putted updated prop");
						thisClass.changedProperties = [];
						thisClass.updateConfig();
					}
					else {
						alert("Failed to save the config!");
					}
				}
			});
		}
	}

	function buildConfig() {

		this.configPath = this.configJSON["path"];
		this.changedProperties = [];
		this.newProperties = [];
		var thisClass = this;

		var lines = this.configJSON["lines"];
		
		$("#i18n_config_title").text(this.configPath);
		$('#config-body').text("");
		
		$('#config-title').show();
		$('#config-body').show();

		var commentDiv = null;
		var propertiesDiv = null;
		var propertiesTable = null;

		// build and fill config
		for (key in lines) {
			
			var line = lines[key];
			
			// comment string
			if ($.type(line) === "string") {
				
				// make comment span
				if (commentDiv == null) {
					
					commentDiv = $('<div class="config-box-content ui-widget ui-state-hover ui-corner-all comment-div"/>');
					commentDiv.appendTo('#config-body');
					
					$('#config-body').append("<br>");
					
					// add new property function
					if(propertiesDiv != null) {
						propertiesDiv = null;	
					}
				}
				
				// for each comment line add <p> to comment span
				var par = $('<p />');
				par.text(line);
				par.appendTo(commentDiv);				

			}
			// property
			else {
			
				// make properties div
				if (propertiesDiv == null) {
					
					propertiesDiv = $('<div class="config-box-content ui-widget-content ui-corner-all properties-div" />');
					propertiesDiv.appendTo('#config-body');				
					propertiesTable = $('<table />');
					propertiesTable.appendTo(propertiesDiv);
						
					$('#config-body').append("<br>");
					commentDiv = null;

				}

				// make table row
				var tr = $('<tr id="' + line["key"] + '" />');
				var id = line["key"].replace(/\./g, '_');
				
				// property
				var name_td = $('<td />');
				var label = $('<label />');
				label.text(line["key"]);
				label.appendTo(name_td);
				name_td.appendTo(tr);

				// value
				var value_td = $('<td />');
				var input = $('<input type="text" value="' + line["value"] + '" id="' + id + '-value"/>');
				input.appendTo(value_td);
				value_td.appendTo(tr);
				
				if (line["enabled"] != true) {
					input.attr('disabled','disabled');
				}

				// checkbox
				var enabled_td = $('<td />');
				var checked = line["enabled"] == true ? "checked" : "" ; 
				var input = $('<input type="checkbox" ' + checked + ' id="' + id + '-enabled" class="enabled_checkbox"/>');
				
				input.appendTo(enabled_td);
				enabled_td.appendTo(tr);

				tr.appendTo(propertiesTable);
				
			}
		}
		
		$("#config-save-btn").removeAttr('disabled');
		$("#mh-restart-btn").removeAttr('disabled');
		
		// make a new property field
		$(".properties-div").each(function(index, element){
			addNewPropertieFields($(this));
		});
		
		// hide new prop fields
		if(!$("#mode_checkbox").is(':checked')) {
			$('.new_property_row').hide();
		}
		
		// add listener to mode checkbox
		$("#mode_checkbox").live('change', function(){
			
			// expert mode
		    if($(this).is(':checked')) {
		    	// show add new property fields
		    	$('.new_property_row').show();
		    }
		    // normal mode
		    else {
		    	// hide add new property fields
		    	$('.new_property_row').hide();
		    }
		   
		});
		
		// hide comments
		if($("#hide_comments_checkbox").is(':checked')) {
			$('.comment-div').hide();
		}
		
		// add listener to hide comments checkbox
		$("#hide_comments_checkbox").live('change', function(){
			
		    if($(this).is(':checked')) {
		    	$('.comment-div').hide();
		    }
		    else {
		    	$('.comment-div').show();
		    }
		   
		});
		
		// add listeners to property's checkbox inputs
		$('.enabled_checkbox').live('change', function(){
			
			var parent = $(this).parent().parent();
			var property = parent.attr('id');
			
			thisClass.changedProperties.push(property);
			
			parent.attr("class", "highlight");
			
		    if($(this).is(':checked')) {
		    
		    	$('#' + property.replace(/\./g, '_') + '-value').removeAttr('disabled');
		    }
		    else {
		    	$('#' + property.replace(/\./g, '_') + '-value').attr('disabled', 'disabled');
		    }
		   
		});
		
		
		// add listeners to value's text fields
		$('input:text').live('change', function(){
			
			var parent = $(this).parent().parent();
			var property = parent.attr('id');
			
			//console.log(property);
			
			if (property) {
				
				thisClass.changedProperties.push(property);
				parent.attr("class", "highlight");	
				
			}
			
			
		});
	}
	
	
	function addNewPropertieFields(propertiesDiv) {
		
		var tr = $('<tr class="new_property_row"/>');
		
		// property
		var name_td = $('<td />');
		var name_input = $('<input type="text" />');
		name_input.appendTo(name_td);
		name_td.appendTo(tr);

		// value
		var value_td = $('<td />');
		var value_input = $('<input type="text" />');
		value_input.appendTo(value_td);
		value_td.appendTo(tr);
		
		// button
		var button_td = $('<td />');
		// last property key as button id
		var button = $('<button>add</button>');		
		var thisClass = this;
		
		button.click(function() {
			
			addNewProperty(propertiesDiv, name_input.val(), value_input.val(), thisClass);
		
		});
		
		button.appendTo(button_td);
		button_td.appendTo(tr);
		propertiesDiv.attr("prevProp", propertiesDiv.find("table tr:last-child").attr("id"));
		
		tr.appendTo(propertiesDiv.find("table"));

	}
	
	
	function addNewProperty(propertiesDiv, propName, value, thisClass) {

		// make table row
		var tr = $('<tr id="' + propName + '" />');
		var id = propName.replace(/\./g, '_');
		
		// property
		var name_td = $('<td />');
		var label = $('<label />');
		label.text(propName);
		label.appendTo(name_td);
		name_td.appendTo(tr);

		// value
		var value_td = $('<td />');
		var input = $('<input type="text" value="' + value + '" id="' + id + '-value"/>');
		input.appendTo(value_td);
		value_td.appendTo(tr);

		// checkbox
		var enabled_td = $('<td />'); 
		var input = $('<input type="checkbox" ' + "checked" + ' id="' + id + '-enabled"/>');
		
		input.appendTo(enabled_td);
		tr.attr("class", "highlight");
		enabled_td.appendTo(tr);

		propertiesDiv.find("table tr:last-child").before(tr);
		
		// add to newProperties (as json)
		var property = {};
		property.key = propName;
		property.value = value;
		property.enabled = true;
		property.prevKey = propertiesDiv.attr("prevProp");
		
		thisClass.newProperties.push(property);
		
		propertiesDiv.attr("prevProp", propName);
		
	}