/**
 * Copyright 2009-2013 The Regents of the University of California Licensed
 * under the Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
$.workflowParser = function(workflow) {
    this.workflow = workflow;
		
    if(workflow.workflow) {
	this.state = this.workflow.workflow.state;
	this.id = this.workflow.workflow.id;
	this.template = this.workflow.workflow.template;
	this.title = this.workflow.workflow.title;
	this.description = this.workflow.workflow.description;
	this.creator = this.workflow.workflow.creator;
	this.organization = this.workflow.workflow.organization;
	this.mediapackage = this.workflow.workflow.mediapackage;
	this.operations = this.workflow.workflow.operations;
	this.targetSmilFlavor = "episode/smil";
	if(this.operations && this.operations.operation) {
	    for(var i = 0; i < this.operations.operation.length; ++i) {
		var op = this.operations.operation[i];
		if(op.id == "editor") {
		    if(op.configurations && op.configurations.configuration) {
			for(var j = 0; j < op.configurations.configuration.length; ++j) {
			    var config = op.configurations.configuration[j];
			    if(config.key == "target-smil-flavor") {
				this.targetSmilFlavor = config.$;
			    }
			}
		    }
		}
	    }
	}
	this.configurations = this.workflow.workflow.configurations;
	this.errors = this.workflow.workflow.errors;
    }
}
