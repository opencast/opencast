
function loadOpencastPaella(containerId) {
  if (! paella.opencast) {
    paella.opencast = new Opencast();
  }
  paella.dataDelegates.MHAnnotationServiceDefaultDataDelegate = MHAnnotationServiceDefaultDataDelegate;
  paella.dataDelegates.MHAnnotationServiceTrimmingDataDelegate = MHAnnotationServiceTrimmingDataDelegate;
  paella.dataDelegates.MHAnnotationServiceVideoExportDelegate = MHAnnotationServiceVideoExportDelegate;
  paella.dataDelegates.UserDataDelegate = UserDataDelegate;
  paella.dataDelegates.MHFootPrintsDataDelegate = MHFootPrintsDataDelegate;
  paella.dataDelegates.OpencastTrackCameraDataDelegate = OpencastTrackCameraDataDelegate;
	return paella.opencast.getEpisode()
	.then(
		function(episode) {
			var converter = new OpencastToPaellaConverter();
			var data = converter.convertToDataJson(episode);
			if (data.streams.length < 1) {
				paella.messageBox.showError("Error loading video! No video tracks found");
			}
			paella.load(containerId, {data:data, configUrl:'/paella/config/config.json'});
		},
		function(){
			var oacl = new OpencastAccessControl();
			oacl.userData().then(function(user){
				if (user.isAnonymous) {
					window.location.href = oacl.getAuthenticationUrl();
				}
				else {
					paella.messageBox.showError("Error loading video " + paella.utils.parameters.get('id') || "");
				}
			});
		}
	);
}


