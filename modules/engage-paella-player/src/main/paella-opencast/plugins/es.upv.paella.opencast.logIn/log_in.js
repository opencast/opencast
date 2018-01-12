/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
new (Class (paella.VideoOverlayButtonPlugin, {
	getName:function() {
		return "es.upv.paella.opencast.logIn";
	},
	getSubclass:function() { return "logIn"; },
	getAlignment:function() { return 'right'; },
	getIndex:function() {return 10;},
	getDefaultToolTip:function() { return base.dictionary.translate("Log in"); },

	checkEnabled:function(onSuccess) {
		paella.initDelegate.initParams.accessControl.userData()
		.then((userdata)=>{		
			onSuccess(userdata.isAnonymous);
		});
	},

	action:function(button) {
		window.location.href = paella.initDelegate.initParams.accessControl.getAuthenticationUrl();
	}

}))();
