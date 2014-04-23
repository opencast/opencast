var messageView = function() {
	
	this.message_state = false;
	this.message_state_cert = false;
	this.workflow_state = false;
	this.restart_state = false;
		
	this.createMessageView = function(message) {
		
		if(this.message_state == false) {
			$("#message").fadeIn("normal"); 
			this.message_state = true;
			
			$("#textMessage").text(message);
		}
	}
	
	this.closeMessageView = function() {
		
		if(this.message_state == true) {
			$("#message").fadeOut("normal");
			this.message_state = false;
		}
	}
	
	this.createMessageCertView = function(message) {
		
		if(this.message_state_cert == false) {
			$("#messageCert").fadeIn("normal"); 
			this.message_state_cert = true;
			
			$("#textMessageCert").text(message);
		}
	}
	
	this.closeMessageCertView = function() {
		
		if(this.message_state_cert == true) {
			$("#messageCert").fadeOut("normal");
			this.message_state_cert = false;
		}
	}
	
	this.createWorkflowFileView = function() {
		
		if(this.workflow_state == false) {
			$("#newWorkflowFile").fadeIn("normal"); 
			this.workflow_state = true;
		}
	}
	
	this.closeWorkflowFileView = function() {
		
		if(this.workflow_state == true) {
			$("#newWorkflowFile").fadeOut("normal");
			this.workflow_state = false;
		}
	}
	
	this.createRestartView = function(message) {
		
		if(this.restart_state == false) {
			$("#messageRestart").fadeIn("normal"); 
			this.restart_state = true;
			$("#rMessage").text(message);
			$("#rMessage").html($("#rMessage").html().replace(/\n/g,'<br/>'));
		}
	}
	
	this.closeRestartView = function() {
		
		if(this.restart_state == true) {
			$("#messageRestart").fadeOut("normal");
			this.restart_state = false;
		}
	}
}