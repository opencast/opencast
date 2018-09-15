/*global Opencast 
         MHAnnotationServiceDefaultDataDelegate
         MHAnnotationServiceTrimmingDataDelegate
         MHFootPrintsDataDelegate
         OpencastTrackCameraDataDelegate
         OpencastToPaellaConverter
         OpencastAccessControl
*/

function initPaellaOpencast() {
  if (!paella.opencast) {
    paella.opencast = new Opencast();
    
    paella.dataDelegates.MHAnnotationServiceDefaultDataDelegate = MHAnnotationServiceDefaultDataDelegate;
    paella.dataDelegates.MHAnnotationServiceTrimmingDataDelegate = MHAnnotationServiceTrimmingDataDelegate;
    paella.dataDelegates.MHFootPrintsDataDelegate = MHFootPrintsDataDelegate;
    paella.dataDelegates.OpencastTrackCameraDataDelegate = OpencastTrackCameraDataDelegate;
  } 
}

function loadOpencastPaella(containerId) {
  initPaellaOpencast();
  paella.load(containerId, {    
    configUrl:'/paella/config/config.json',
    loadVideo:function() {
      return new Promise((resolve, reject) => {
        paella.opencast.getEpisode()
        .then((episode) => {
          var converter = new OpencastToPaellaConverter();
          var data = converter.convertToDataJson(episode);
          if (data.streams.length < 1) {
            paella.messageBox.showError(paella.dictionary.translate('Error loading video! No video traks found'));
          }
          else {
            resolve(data);
          }
        })
        .catch(()=>{
          var oacl = new OpencastAccessControl();
          oacl.userData().then((user) => {
            if (user.isAnonymous) {
              window.location.href = oacl.getAuthenticationUrl();
            }
            else {
              var errMsg = paella.dictionary.translate('Error loading video {id}').replace(/\{id\}/g, paella.utils.parameters.get('id') || '');
              paella.messageBox.showError(errMsg);
            }
          });
        });    
      });
    }
  });
}
