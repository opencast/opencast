	var request = new requestHandler();
	var message = new messageView();
	var optionHandler; 
	
	$(function() {
		$('#messageRestart').load('/ui-files/templates/restartMessage.tpl');
		$('#message').load('/ui-files/templates/messageOption.tpl');
		$('#messageCert').load('/ui-files/templates/messageCertificate.tpl');
		$('#newWorkflowFile').load('/ui-files/templates/createWorkflowFile.tpl');
		
		initWorkflows();
		request.sendPostRequestWithWorkflowFileName();
	});
	
	function initWorkflows() {
			$.each(workflow_data, function(i,j){ 
      		   $('#workflow_selection').append($('<option></option>').val(j.name).text(j.name));
    		});
			
//			$('#workflowsTree').fileTree({owner: "workflow"}, function(file) {
//				console.log(file);
//			});
			
	}
	
	function clearTextField() {
		editor.markClean();
		editor.setValue("");
	}
	
	function handleNewWorkflowCreation() {
		message.createWorkflowFileView();
		$('#fileState').text("minimum 3 chars!");

		jQuery('#workflowInputFile').keyup(function () { 
			
			this.value = this.value.replace(/[^a-z0-9\-]/g,'');
			
			if(this.value.length < 3) {
				$('#fileState').text("minimum 3 chars!");
			} else if(this.value.length > 50) {
				$('#fileState').text("maximum 50 chars!");
			} else {
				$('#fileState').text("");
			
			var v = this.value;
					
				var wD = ""; 
				$.each(workflow_data, function(i, item) {
					var tmp = v + ".xml";
				 	if (item.name == tmp) {
				 		wD = item.name;
				 		return false;
				 	} 
				});
				if (wD != "") {
					$('#fileState').text("name in use!");
				}
			}
		});

	}
	
	function saveNewWorkflowFile() {

		var value = $('#workflowInputFile').val();
		
		if(value.length < 3) {
		} else if(value.length > 50) {
		} else {
			var wD = ""; 
			$.each(workflow_data, function(i, item) {
				var tmp = value + ".xml";
				
			 	if (item.name == tmp) {
			 		wD = item.name;
			 		return false;
			 	} 
			});
			
			if (wD == "") {
				request.sendNewWorkflowFile(value);
			} 
		}
	}
	
	function handleSaveWorkflowVerification() {
		optionHandler = "save";
		message.createMessageView("Save this file?");
	}
	
	function handleXMLRequest() {
		
		if (optionHandler == "save") {
			var info = request.sendXMLFileAsRequest();
			message.closeMessageView();
			
			if (info != "ok") {
				message.createMessageCertView(info);
			}
		} else if (optionHandler == "delete") {
			request.deleteWorkflowFile();
			message.closeMessageView();
			request.sendPostRequestWithWorkflowFileName();
		}
		optionHandler = "";
	}
	
	function deleteWorkflowFile() {
		optionHandler = "delete";
		message.createMessageView("Delete " + $('#workflow_selection :selected').text() + " File?");
	}
	
	function installationComplete() { }