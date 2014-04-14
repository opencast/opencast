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
$.mediapackageParser = function(mediapackage)
{
    var SMILTYPE_PRESENTER = "presenter/smil";
    var SMILTYPE_PRESENTATION = "presentation/smil";
    var SMIL = "";

    this.mediapackage = mediapackage;
	
    if(this.mediapackage) {
	this.start = this.mediapackage.start;
	this.id = this.mediapackage.id;
	this.duration = this.mediapackage.duration;
	this.title = this.mediapackage.title;
	this.creators = this.mediapackage.creators;
	this.license = this.mediapackage.license;
	this.media = this.mediapackage.media;
	this.metadata = this.mediapackage.metadata;
	this.attachments = this.mediapackage.attachments;
	
	this.smil = "";
	this.smil_id = -1;
	this.smil_mimetype = "";
	this.smil_tags = "";
	this.smil_type = "";
	this.smil_url = "";

	if(this.metadata.catalog) {
	    var smil_presenter = undefined;
	    var smil_presentation = undefined;
	    $.each(this.metadata.catalog, function(index, value) {
		if(value.type.indexOf(SMILTYPE_PRESENTER) != -1) {
		    smil_presenter = value;
		} else if(value.type.indexOf(SMILTYPE_PRESENTATION) != -1) {
		    smil_presentation = value;
		}
	    });
	    if(smil_presenter != undefined) {
		SMIL = smil_presenter;
	    } else if(smil_presentation != undefined) {
		SMIL = smil_presentation;
	    }
	    this.smil = SMIL;
	    this.smil_id = this.smil.id;
	    this.smil_mimetype = this.smil.mimetype;
	    this.smil_tags = this.smil.tags;
	    this.smil_type = this.smil.type;
	    this.smil_url = this.smil.url;
	}
    }
}
