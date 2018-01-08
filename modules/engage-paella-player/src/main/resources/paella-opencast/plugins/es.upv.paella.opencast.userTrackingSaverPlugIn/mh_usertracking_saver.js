new (Class (paella.userTracking.SaverPlugIn, {
	getName: function() { return "es.upv.paella.opencast.userTrackingSaverPlugIn"; },
	
	checkEnabled: function(onSuccess) {
		paella.ajax.get({url:'/usertracking/detailenabled'},
			function(data, contentType, returnCode) {
				if (data == 'true') {
					onSuccess(true); 					
				}
				else {
					onSuccess(false); 
				}
			},
			function(data, contentType, returnCode) {
				onSuccess(false);
			}
		);	
	},
	
	log: function(event, params) {
		paella.player.videoContainer.currentTime().then(function(ct){
			var videoCurrentTime = parseInt(ct + paella.player.videoContainer.trimStart());		
			var opencastLog = {
				_method: 'PUT',
				'id': paella.player.videoIdentifier,
				'type': undefined,
				'in': videoCurrentTime,
				'out': videoCurrentTime,
				'playing': !paella.player.videoContainer.paused()
			};
			
			switch (event) {
				case paella.events.play:
					opencastLog.type = 'PLAY';
					break;
				case paella.events.pause:
					opencastLog.type = 'PAUSE';
					break;
				case paella.events.seekTo:
				case paella.events.seekToTime:
					opencastLog.type = 'SEEK';
					break;
				case paella.events.resize:
					opencastLog.type = "RESIZE-TO-" + params.width + "x" + params.height;
					break;
				case "paella:searchService:search":
					opencastLog.type = "SEARCH-" + params;
					break;
				default:
					opencastLog.type = event;
					var opt = params;
					if (opt != undefined) {				
						if (typeof(params) == "object") {
							opt = JSON.stringify(params);
						}
						opencastLog.type = event + ';' + opt;
					}
					break;
			}
			opencastLog.type = opencastLog.type.substr(0, 128);
			//console.log(opencastLog);
			paella.ajax.get( {url: '/usertracking/', params: opencastLog});
		});		
	}
}))();
