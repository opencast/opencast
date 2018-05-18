
class MHAnnotationServiceDefaultDataDelegate extends paella.DataDelegate {
  constructor() {
    super();
  }

  getName() { return "es.upv.paella.opencast.MHAnnotationServiceDefaultDataDelegate"; }

  read(context,params,onSuccess) {
    var episodeId = params.id;
    paella.ajax.get({url: '/annotation/annotations.json', params: {episode: episodeId, type: "paella/"+context}},
      function(data, contentType, returnCode) {
        var annotations = data.annotations.annotation;
        if (!(annotations instanceof Array)) { annotations = [annotations]; }
        if (annotations.length > 0) {
          if (annotations[0] && annotations[0].value !== undefined) {
            var value = annotations[0].value;
            try {
              value = JSON.parse(value);
            }
            catch(err) {}
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

    paella.ajax.get({url: '/annotation/annotations.json', params: {episode: episodeId, type: "paella/"+context}},
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

    paella.ajax.get({url: '/annotation/annotations.json', params: {episode: episodeId, type: "paella/"+context}},
      function(data, contentType, returnCode) {
        var annotations = data.annotations.annotation;
        if(annotations) {
          if (!(annotations instanceof Array)) { annotations = [annotations]; }
          var asyncLoader = new paella.AsyncLoader();
          for ( var i=0; i< annotations.length; ++i) {
            var annotationId = data.annotations.annotation.annotationId;
            asyncLoader.addCallback(new paella.JSONCallback({url:'/annotation/'+annotationId}, "DELETE"));
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

  getName() { return "es.upv.paella.opencast.MHAnnotationServiceTrimmingDataDelegate"; }

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

class MHAnnotationServiceVideoExportDelegate extends MHAnnotationServiceDefaultDataDelegate {
  constructor(){
    super();
  }

  getName() { return "es.upv.paella.opencast.MHAnnotationServiceVideoExportDelegate"; }

  read(context, params, onSuccess) {
    var ret = {};

    super.read(context, params, function(data, success) {
      if (success){
        ret.trackItems = data.trackItems;
        ret.metadata = data.metadata;

        this.super.read(context+"#sent", params, function(dataSent, successSent) {
          if (successSent){
            ret.sent = dataSent.sent;
          }
          this.super.read(context+"#inprogress", params, function(dataInProgress, successInProgress) {
            if (successInProgress) {
              ret.inprogress = dataInProgress.inprogress;
            }

            if (onSuccess) { onSuccess(ret, true); }
          });
        });
      }
      else {
        if (onSuccess) { onSuccess({}, false); }
      }
    });
  }

  write(context, params, value, onSuccess) {
    var thisClass = this;

    var valInprogress = { inprogress: value.inprogres };
    var valSent = { sent: value.sent };
    var val = { trackItems:value.trackItems, metadata: value.metadata };
    if (val.trackItems.length > 0) {
      super.write(context, params, val, function(data, success) {
        if (success) {
          if (valSent.sent) {
            thisClass.remove(context+"#inprogress", params, function(data, success){
              this.super.write(context+"#sent", params, valSent, function(dataSent, successSent) {
                if (successSent) {
                  if (onSuccess) { onSuccess({}, true); }
                }
                else { if (onSuccess) { onSuccess({}, false); } }
              });
            });
          }
          else {
            //if (onSuccess) { onSuccess({}, true); }
            thisClass.remove(context+"#sent", params, function(data, success){
              if (onSuccess) { onSuccess({}, success); }
            });
          }
        }
        else { if (onSuccess) { onSuccess({}, false); } }
      });
    }
    else {
      this.remove(context, params, function(data, success){
        if (onSuccess) { onSuccess({}, success); }
      });
    }
  }

  remove(context, params, onSuccess) {

    super.remove(context, params, function(data, success) {
      if (success) {
        this.super.remove(context+"#sent", params, function(dataSent, successSent) {
          if (successSent) {
            this.super.remove(context+"#inprogress", params, function(dataInProgress, successInProgress) {
              if (successInProgress) {
                if (onSuccess) { onSuccess({}, true); }
              }
              else { if (onSuccess) { onSuccess({}, false); } }
            });
          }
          else { if (onSuccess) { onSuccess({}, false); } }
        });
      }
      else { if (onSuccess) { onSuccess({}, false); } }
    });
  }
}


class UserDataDelegate extends paella.DataDelegate {
  constructor(){
    super();
  }

  getName() { return "es.upv.paella.opencast.UserDataDelegate"; }

  initialize() {
  }

  read(context, params, onSuccess) {
    var value = {
    userName: params.username,
    name: params.username,
    lastname: '',
    avatar:"plugins/silhouette32.png"
  };

      if (typeof(onSuccess)=='function') { onSuccess(value,true); }
  }
}

class MHFootPrintsDataDelegate extends paella.DataDelegate {
  constructor(){
    super();
  }

  getName() { return "es.upv.paella.opencast.MHFootPrintsDataDelegate"; }

  read(context,params,onSuccess) {
    var episodeId = params.id;

    paella.ajax.get({url: '/usertracking/footprint.json', params: {id: episodeId}},
      function(data, contentType, returnCode) {
        if ((returnCode == 200) && (contentType == 'application/json')) {
          var footPrintsData = data.footprints.footprint;
          if (data.footprints.total == "1"){
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
    var thisClass = this;
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

  getName() { return "es.upv.paella.opencast.OpencastTrackCameraDataDelegate"; }

  read(context,params,onSuccess) {
    let attachments = paella.opencast._episode.mediapackage.attachments.attachment;
    let trackhdUrl;
    if (attachments === undefined) {
      return;
    }
    for (let i = 0; i < attachments.length; i++) {
      if (attachments[i].type.indexOf("trackhd") > 0) {
        trackhdUrl = attachments[i].url;
      }
    }
    if (trackhdUrl) {
      paella.utils.ajax.get({ url:trackhdUrl },
        (data) => {
          if (typeof(data)=="string") {
            try {
              data = JSON.parse(data);
            }
            catch(err) {}
          }
          data.positions.sort((a,b) => {
            return a.time-b.time;
          })
          onSuccess(data)
        },
        () => onSuccess(null) );
    }
    else {
      onSuccess(null);
    }
  }
};
