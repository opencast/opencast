
var OpencastToPaellaConverter = Class.create({
	isStreaming:function(track) {
		return /rtmps?:\/\//.test(track.url);
	},

	getStreamSource:function(track) {
		var res = new Array(0,0);
        if (track.video instanceof Object) {
		    res = track.video.resolution.split('x');
        }

		var src = track.url;
		var urlSplit = /^(rtmps?:\/\/[^\/]*\/[^\/]*)\/(.*)$/.exec(track.url);
		if (urlSplit != null) {
			var rtmp_server =  urlSplit[1];
			var rtmp_stream =  urlSplit[2];			
			src = {
				server: encodeURIComponent(rtmp_server),
				stream: encodeURIComponent(rtmp_stream)
			};
		}

		var source = {			
			src:  src,
			mimetype: track.mimetype,
			res: {w:res[0], h:res[1]},
			isLiveStream: (track.live===true)
		};

		return source;
	},

	isSupportedStreamingTrack: function(track) {
		if (/^(rtmps?:\/\/[^\/]*\/[^\/]*)\/(.*)$/.test(track.url) == true) {
			switch (track.mimetype) {
				case 'video/mp4':
				case 'video/ogg':
				case 'video/webm':
				case 'video/x-flv':
					return true;
				default:
					return false;
			}
		}
		return false;
	},
	
	convertToDataJson: function(episode) {
		var self = this;
		var opencastStreams = {};
		var opencastFrameList = {};

		var tracks = episode.mediapackage.media.track;
		var attachments = episode.mediapackage.attachments.attachment;
		if (!(tracks instanceof Array)) { tracks = [tracks]; }
		if (!(attachments instanceof Array)) { attachments = [attachments]; }		
		// Read the tracks!!
		tracks.forEach(function(currentTrack) {
			var currentStream = opencastStreams[currentTrack.type];
			if (currentStream == undefined) { currentStream = { sources:{}, preview:'' }; }
			
			
			if (self.isStreaming(currentTrack)) {
				if (self.isSupportedStreamingTrack(currentTrack)) {
					if ( !(currentStream.sources['rtmp']) || !(currentStream.sources['rtmp'] instanceof Array)){
						currentStream.sources['rtmp'] = [];
					}
					currentStream.sources['rtmp'].push(self.getStreamSource(currentTrack));
				}
			}
			else{
				var videotype = null;
				switch (currentTrack.mimetype) {
					case 'video/mp4':
					case 'video/ogg':
					case 'video/webm':
						videotype = currentTrack.mimetype.split("/")[1];
						break;
					case 'video/x-flv':
						videotype = 'flv';
						break;						
					case 'application/x-mpegURL':
						videotype = 'hls';
						break;
					case 'application/dash+xml':
						videotype = 'mpd';
						break;
						
					default:
						paella.debug.log('OpencastToPaellaConverter: MimeType ('+currentTrack.mimetype+') not recognized!');
						break;
				}
				if (videotype){
					if ( !(currentStream.sources[videotype]) || !(currentStream.sources[videotype] instanceof Array)){
						currentStream.sources[videotype] = [];
					}				
					currentStream.sources[videotype].push(self.getStreamSource(currentTrack));
				}
			}

			opencastStreams[currentTrack.type] = currentStream;
		});
		
		var duration = parseInt(episode.mediapackage.duration/1000);
		var presenter = opencastStreams["presenter/delivery"] || opencastStreams["presenter/preview"];
		var presentation = opencastStreams["presentation/delivery"] || opencastStreams["presentation/preview"];		
		var imageSource =   {type:"image/jpeg", frames:{}, count:0, duration: duration, res:{w:320, h:180}};
		var imageSourceHD = {type:"image/jpeg", frames:{}, count:0, duration: duration, res:{w:1280, h:720}};
		var blackboardSource = {type:"image/jpeg", frames:{}, count:0, duration: duration, res:{w:1280, h:720}};
		// Read the attachments
		attachments.forEach(function(currentAttachment){
			try {
				if (currentAttachment.type == "blackboard/image") {
					if (/time=T(\d+):(\d+):(\d+)/.test(currentAttachment.ref)) {
						time = parseInt(RegExp.$1)*60*60 + parseInt(RegExp.$2)*60 + parseInt(RegExp.$3);
						
						blackboardSource.frames["frame_"+time] = currentAttachment.url;
						blackboardSource.count = blackboardSource.count +1;                	
					}
				
				}
				else if (currentAttachment.type == "presentation/segment+preview+hires") {
					if (/time=T(\d+):(\d+):(\d+)/.test(currentAttachment.ref)) {
						time = parseInt(RegExp.$1)*60*60 + parseInt(RegExp.$2)*60 + parseInt(RegExp.$3);
						imageSourceHD.frames["frame_"+time] = currentAttachment.url;
						imageSourceHD.count = imageSourceHD.count +1;
			        	
			        	if (!(opencastFrameList[time])){
			            	opencastFrameList[time] = {id:'frame_'+time, mimetype:currentAttachment.mimetype, time:time, url:currentAttachment.url, thumb:currentAttachment.url};
			        	}
			        	opencastFrameList[time].url = currentAttachment.url;
					}
				}
				else if (currentAttachment.type == "presentation/segment+preview") {
					if (/time=T(\d+):(\d+):(\d+)/.test(currentAttachment.ref)) {
						var time = parseInt(RegExp.$1)*60*60 + parseInt(RegExp.$2)*60 + parseInt(RegExp.$3);
						imageSource.frames["frame_"+time] = currentAttachment.url;
						imageSource.count = imageSource.count +1;
						
			        	if (!(opencastFrameList[time])){
			            	opencastFrameList[time] = {id:'frame_'+time, mimetype:currentAttachment.mimetype, time:time, url:currentAttachment.url, thumb:currentAttachment.url};
			        	}
			        	opencastFrameList[time].thumb = currentAttachment.url;
					}
				}
				else if (currentAttachment.type == "presentation/player+preview") {
					presentation.preview = currentAttachment.url;
				}
				else if (currentAttachment.type == "presenter/player+preview") {
					presenter.preview = currentAttachment.url;
				}
			} 
			catch (err) {}
		});
		
		
		// Set the image stream
		var imagesArray = [];
		if (imageSourceHD.count > 0) { imagesArray.push(imageSourceHD); }
		if (imageSource.count > 0) { imagesArray.push(imageSource); }
		if ( (imagesArray.length > 0) && (presentation != undefined) ){
			presentation.sources.image = imagesArray; 
		}
		
		// Set the blackboard images
		var blackboardArray = [];
		if (blackboardSource.count > 0) { blackboardArray.push(blackboardSource); }		
		if ( (blackboardArray.length > 0) && (presenter != undefined) ){
			presenter.sources.image = blackboardArray;
		}				
	

		

		var data =  {
			metadata: {
				title: episode.mediapackage.title,
				duration: episode.mediapackage.duration/1000
			},
			streams: [],
			frameList: []
		};

		if (presenter) { data.streams.push(presenter); }
		if (presentation) { data.streams.push(presentation); }
		
		Object.keys(opencastFrameList).forEach(function(key, index) {
			data.frameList.push(opencastFrameList[key]);
		});
		
		return data;
	}
		
});

