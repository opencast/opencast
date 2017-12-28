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
