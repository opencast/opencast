var MediaManager = function() {
  this.streams = {};
  this.audioDevices = [];
  this.videoDevices = [];
  this.browser = window.hasOwnProperty('InstallTrigger') ? 'firefox' : (window.hasOwnProperty('chrome') ? 'chrome' : 'other');
  this.desktopConstraints = {
     chrome: {
               audio: false,
               video: {mandatory: {
                        chromeMediaSource: 'desktop',
                        chromeMediaSourceId: null,
                        maxWidth: window.screen.width,
                        maxHeight: window.screen.height
                      }}
             },
    firefox: {
               video: {mediaSource: 'screen'}
             }
  }
  this.cameraConstraints = {audio: true, video: {exact: undefined}};
  this.micConstraints = {audio: {exact: undefined}};
 
  var self = this;

  navigator.mediaDevices.enumerateDevices()
    .then(devices => {
      devices.forEach(device => {
        if (device.kind.indexOf('audio') > -1) {
          if (device.label == 'Default') {
            device.label = device.kind.indexOf('output') > -1 ? 'Speakers' : 'Microphone';
          }
          self.audioDevices.push(device);
        }
        else if (device.kind.indexOf('video') > -1) {
          self.videoDevices.push(device);
        }
      });
      enumerateCompleteFuncs.forEach(func => {
        func([].concat(this.videoDevices, this.audioDevices).filter(device => device.kind.indexOf('input') > -1));
      });
    });

  var enumerateCompleteFuncs = [];

  Object.defineProperty(this, 'onenumeratedevices', {
    get: function() {
           return enumerateCompleteFuncs;
         },
    set: function(func) {
           if (typeof func == 'function') {
             enumerateCompleteFuncs.push(func);
           }
         }
  });
}

MediaManager.prototype = {
     constructor: MediaManager,
  activateStream: function(id, isDesktop) {
                    return new Promise(function(resolve, reject) {
                      if (this.streams.hasOwnProperty(id)) {
                        resolve(this.streams[id].stream);
                        return;
                      }

                      var constraints = this.cameraConstraints;
                      var deviceType = 'webcam';
                      constraints.video.exact = id;
                      if (isDesktop) {
                        constraints = this.desktopConstraints[this.browser];
                        deviceType = 'desktop';
                        if (this.browser == 'chrome') {
                          constraints.video.mandatory
                            .chromeMediaSourceId = id;
                        }
                      }
                      else {
                        if (this.audioDevices
                              .map(device => device.deviceId).indexOf(id) > -1) {
                          constraints = this.micConstraints;
                          constraints.audio.exact = id;
                          deviceType = 'microphone';
                        }
                      }

                      navigator.mediaDevices.getUserMedia(constraints)
                        .then(function(stream) {
                          this.streams[id] = {
                            stream: stream,
                            local: true,
                            type: deviceType
                          };
                          resolve(stream);
                          return;
                        }.bind(this))
                        .catch(err => {
                          reject(err);
                        });
                    }.bind(this));
                  },
 addRemoteStream: function(peerId, stream) {
                    if (this.streams[peerId]) {
                      //TODO: handle this
                      return;
                    }
                    this.streams[peerId] = {
                      stream: stream,
                      remote: true
                    }
                  },
      getTrackId: function(id) {
                    if (!this.streams.hasOwnProperty(id)) {
                      return null;
                    }

                    return this.streams[id].stream.id;
                  }
}
