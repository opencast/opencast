
class MHAnnotationServiceDefaultDataDelegate extends paella.DataDelegate {
  constructor() {
    super();
  }

  read(context,params,onSuccess) {
    var episodeId = params.id;
    paella.ajax.get({url: '/annotation/annotations.json', params: {episode: episodeId, type: 'paella/'+context}},
      function(data, contentType, returnCode) {
        var annotations = data.annotations.annotation;
        if (!(annotations instanceof Array)) { annotations = [annotations]; }
        if (annotations.length > 0) {
          if (annotations[0] && annotations[0].value !== undefined) {
            var value = annotations[0].value;
            try {
              value = JSON.parse(value);
            }
            catch(err) {/**/}
            if (onSuccess) onSuccess(value, true);
          }
          else{
            if (onSuccess) onSuccess(undefined, false);
          }
        }
        else {
          if (onSuccess) onSuccess(undefined, false);
        }
      },
      function(data, contentType, returnCode) { onSuccess(undefined, false); }
    );
  }

  write(context,params,value,onSuccess) {
    var thisClass = this;
    var episodeId = params.id;
    if (typeof(value)=='object') value = JSON.stringify(value);

    paella.ajax.get({url: '/annotation/annotations.json', params: {episode: episodeId, type: 'paella/'+context}},
      function(data, contentType, returnCode) {
        var annotations = data.annotations.annotation;
        if (annotations == undefined) {annotations = [];}
        if (!(annotations instanceof Array)) { annotations = [annotations]; }

        if (annotations.length == 0 ) {
          paella.ajax.put({ url: '/annotation/',
            params: {
              episode: episodeId,
              type: 'paella/' + context,
              value: value,
              'in': 0
            }},
          function(data, contentType, returnCode) { onSuccess({}, true); },
          function(data, contentType, returnCode) { onSuccess({}, false); }
          );
        }
        else if (annotations.length == 1 ) {
          var annotationId = annotations[0].annotationId;
          paella.ajax.put({ url: '/annotation/'+ annotationId, params: { value: value }},
            function(data, contentType, returnCode) { onSuccess({}, true); },
            function(data, contentType, returnCode) { onSuccess({}, false); }
          );
        }
        else if (annotations.length > 1 ) {
          thisClass.remove(context, params, function(notUsed, removeOk){
            if (removeOk){
              thisClass.write(context, params, value, onSuccess);
            }
            else{
              onSuccess({}, false);
            }
          });
        }
      },
      function(data, contentType, returnCode) { onSuccess({}, false); }
    );
  }

  remove(context,params,onSuccess) {
    var episodeId = params.id;

    paella.ajax.get({url: '/annotation/annotations.json', params: {episode: episodeId, type: 'paella/'+context}},
      function(data, contentType, returnCode) {
        var annotations = data.annotations.annotation;
        if(annotations) {
          if (!(annotations instanceof Array)) { annotations = [annotations]; }
          var asyncLoader = new paella.AsyncLoader();
          for ( var i=0; i< annotations.length; ++i) {
            var annotationId = data.annotations.annotation.annotationId;
            asyncLoader.addCallback(new paella.JSONCallback({url:'/annotation/'+annotationId}, 'DELETE'));
          }
          asyncLoader.load(function(){ if (onSuccess) { onSuccess({}, true); } }, function() { onSuccess({}, false); });
        }
        else {
          if (onSuccess) { onSuccess({}, true); }
        }
      },
      function(data, contentType, returnCode) { if (onSuccess) { onSuccess({}, false); } }
    );
  }
}

class MHAnnotationServiceTrimmingDataDelegate extends MHAnnotationServiceDefaultDataDelegate {
  constructor(){
    super();
  }

  read(context,params,onSuccess) {
    super.read(context, params, function(data,success) {
      if (success){
        if (data.trimming) {
          if (onSuccess) { onSuccess(data.trimming, success); }
        }
        else{
          if (onSuccess) { onSuccess(data, success); }
        }
      }
      else {
        if (onSuccess) { onSuccess(data, success); }
      }
    });
  }

  write(context,params,value,onSuccess) {
    super.write(context, params, {trimming: value}, onSuccess);
  }
}


class MHFootPrintsDataDelegate extends paella.DataDelegate {
  constructor(){
    super();
  }

  read(context,params,onSuccess) {
    var episodeId = params.id;

    paella.ajax.get({url: '/usertracking/footprint.json', params: {id: episodeId}},
      function(data, contentType, returnCode) {
        if ((returnCode == 200) && (contentType == 'application/json')) {
          var footPrintsData = data.footprints.footprint;
          if (data.footprints.total == '1'){
            footPrintsData = [footPrintsData];
          }
          if (onSuccess) { onSuccess(footPrintsData, true); }
        }
        else{
          if (onSuccess) { onSuccess({}, false); }
        }
      },
      function(data, contentType, returnCode) {
        if (onSuccess) { onSuccess({}, false); }
      }
    );
  }

  write(context,params,value,onSuccess) {    
    var episodeId = params.id;
    paella.ajax.get({url: '/usertracking/', params: {
      _method: 'PUT',
      id: episodeId,
      type:'FOOTPRINT',
      in:value.in,
      out:value.out }
    },
    function(data, contentType, returnCode) {
      var ret = false;
      if (returnCode == 201) { ret = true; }
      if (onSuccess) { onSuccess({}, ret); }
    },
    function(data, contentType, returnCode) {
      if (onSuccess) { onSuccess({}, false); }
    }
    );
  }
}

class OpencastTrackCameraDataDelegate extends paella.DataDelegate {

  read(context,params,onSuccess) {
    let attachments = paella.opencast._episode.mediapackage.attachments.attachment;
    let trackhdUrl;
    if (attachments === undefined) {
      return;
    }
    for (let i = 0; i < attachments.length; i++) {
      if (attachments[i].type.indexOf('trackhd') > 0) {
        trackhdUrl = attachments[i].url;
      }
    }
    if (trackhdUrl) {
      paella.utils.ajax.get({ url:trackhdUrl },
        (data) => {
          if (typeof(data)=='string') {
            try {
              data = JSON.parse(data);
            }
            catch(err) {/**/}
          }
          data.positions.sort((a,b) => {
            return a.time-b.time;
          });
          onSuccess(data);
        },
        () => onSuccess(null) );
    }
    else {
      onSuccess(null);
    }
  }
}
