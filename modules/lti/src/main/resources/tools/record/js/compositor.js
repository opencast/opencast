var Compositor = function() {
  this.browser = window.hasOwnProperty('InstallTrigger') ? 'firefox' : (window.hasOwnProperty('chrome') ? 'chrome' : 'other');
  this.streams = [];
  this.streamVideo = document.createElement('video');
  this.streamVideo.autoplay = true;
  this.streamLayers = [];
  this.drawing = false;
  this.attachedCanvas = null;
  this.playResult = false;
  this.width = 1280;
  this.height = 720;

  this.recorder;
  this.recordedData = [];
  this.codecs = ['video/webm; codecs="vp8,opus"'];

  if (this.browser == 'firefox') {
    this.audioRecorders = [];
  }
  this.addAudioTrack = this.browser == 'firefox' ? this.mozAddAudioTrack : this.blinkAddAudioTrack;

  this.init();

  /***"Private" member***/
  var _canvas = document.createElement('canvas');
  _canvas.width = this.width;
  _canvas.height = this.height;
  var _ctx = _canvas.getContext('2d');

  Object.defineProperty(this, 'dimensions', {
    get: function() {
      return {width: _canvas.width, height: _canvas.height};
    },
    set: function(dims) {
      if (dims.width) {
        this.width = _canvas.width = dims.width;
      }
      if (dims.height) {
        this.height = _canvas.height = dims.height;
      }
    }
  });

  Object.defineProperty(this, 'canvasContext', {
    get: function() {
           return _ctx;
         }
  });

  /***Event triggering***/
  var _result = null;
  var _streamCreatedFuncs = [];

  Object.defineProperty(this, 'stream', {
    get: function() {
           return _result;
         },
    set: function(stream) {
           if (stream instanceof MediaStream) {
             _result = stream;
             _streamCreatedFuncs.forEach(function(fn) {
               fn(stream);
             });
           }
           else {
             console.log('incompatible stream for compositor');
           }
         }
  });

  Object.defineProperty(this, 'onstreamcreated', {
    get: function() {
           return null;
         },
    set: function(fn) {
           if (typeof fn == 'function') {
             if (_result) {
               fn(_result);
             }
             else {
               _streamCreatedFuncs.push(fn);
             }
           }
         }
  });

  var _audioCache = [];

  Object.defineProperty(this, 'cacheTrack', {
    get: function() {
           return _audioCache;
         },
    set: function(track) {
           if (!(track instanceof MediaStreamTrack) && !(track instanceof MediaStream)) {
             return;
           }

           if (this.stream) {
             this.addAudioTrack(track);
           }
           else {
             _audioCache.push(track);
           }
         }
  });

  var _audioAddedFuncs = [];

  Object.defineProperty(this, 'onaudiotrack', {
    set: function(fn) {
      if (fn && typeof fn == 'function') {
        _audioAddedFuncs.push(fn);
      }
    }
  });

  Object.defineProperty(this, 'audioTrack', {
    set: function(track) {
           _audioAddedFuncs.forEach(fn => {
             fn(track);
           });
         }
  });

  var _delegations = {};
  Object.defineProperty(this, 'delegations', {
    get: function() {
      return _delegations
    }
  });

  this.rafTokens = {
    draw: null,
    display: null
  };
}

Compositor.prototype = {
  constructor: Compositor,
  init: function() {
    this.codecs = [
                    'video/webm;codecs="vp9,opus"',
                    'video/webm;codecs="vp9.0,opus"',
                    'video/webm;codecs="avc1"',
                    'video/x-matroska;codecs="avc1"',
                    'video/webm;codecs="vp8,opus"'
                  ].filter(codec => MediaRecorder.isTypeSupported(codec));
  },
        addStream: function(stream, opts) {
                     var streamIndex = this.streams.map(curStream => curStream.track.id).indexOf(stream.id);
                     if (streamIndex  > -1) {
                       this.streams[streamIndex].attached = true;
                       return;
                     }

                     var self = this;
                         var video = document.createElement('video');
                         video.autoplay = true;
                         video.muted = true;
                         video.onloadedmetadata = function() {
                         //supposing center of canvas is origin, get vector to centre of each separate stream displayed on canvas...
                         //...and calculate angles from origin to each stream...
                         //...thereafter choose an empty corner for new stream...
                         var height = self.height/3;
                         var width = this.videoWidth/this.videoHeight * height;
                         var offsetX = 0;
                         var offsetY = 0;

                         var currentAngles = self.streams
                                               .filter(stream => self.getDistance(
                                                                   stream.offsetX + stream.width/2 - self.width/2,
                                                                   stream.offsetY + stream.height/2 - self.height/2
                                                                 ) > 10)         //filter out streams centred close to origin
                                               .map(stream => [
                                                               (stream.offsetX + stream.width/2 - self.width/2),
                                                               (stream.offsetY + stream.height/2 - self.height/2)
                                                              ]
                                               )
                                               .map(coords => {
                                                 var quadAngle = Math.atan(coords[1]/coords[0])*180/Math.PI;
                                                 if (coords[1] < 0 || coords[0] < 0) {
                                                   quadAngle = 180 + quadAngle * ( coords[1] < 0 ? 1 : -1);
                                                   if (coords[0] > 0) {
                                                     quadAngle += 180;
                                                   }
                                                 }
                                                 return quadAngle;
                                               });

                         var availableAngles = [45, 135, 315, 225].filter((quadrant, i) => {
                                                 var available = true;
                                                 currentAngles.some(angle => {
                                                   if (Math.abs(angle - quadrant) < 90) {
                                                     available = false;
                                                     return;
                                                   }
                                                 });
                                                 return available;
                                               });

                         if (self.streams.length > 4) {
                           throw new Error('max streams');
                         }
                         else if (self.streams.length > 0 && availableAngles.length >0) {
                           var chosenRadian = availableAngles[0]*Math.PI/180;
                           offsetX = Math.cos(chosenRadian) >= 0 ? self.width - width : 0;
                           offsetY = Math.sin(chosenRadian) >= 0 ? self.height - height : 0;
                         }
                         else if (availableAngles.length > 0) {
                           var widthRatio = this.videoWidth / self.width;
                           var heightRatio = this.videoHeight / self.height;
                           var ratio = Math.max(widthRatio, heightRatio, 1);
                           width = Math.floor(this.videoWidth/ratio);
                           height = Math.floor(this.videoHeight/ratio);
                           offsetX = Math.abs(Math.floor(self.width - this.videoWidth/ratio)/2);
                           offsetY = Math.abs(Math.floor(self.height - this.videoHeight/ratio)/2);
                         }

                         var index = self.streams.push({
                                          track: stream,
                                          video: video,
                                          width: width,
                                      cropWidth: this.videoWidth,
                                         height: height,
                                     cropHeight: this.videoHeight,
                                        offsetX: offsetX,
                                    cropOffsetX: 0,
                                        offsetY: offsetY,
                                    cropOffsetY: 0,
                                       attached: true
                                     });
                         self.streamLayers.push(index - 1);
                         self.startDraw();
                         setTimeout(() => {
                           video.addEventListener('resize', function(e) {
                             var widthRatio = this.videoWidth / self.width;
                             var heightRatio = this.videoHeight / self.height;
                             var ratio = Math.max(widthRatio, heightRatio, 1);
                             self.streams[index - 1].cropWidth = this.videoWidth;
                             self.streams[index - 1].cropHeight = this.videoHeight;
                             self.streams[index - 1].width = Math.floor(this.videoWidth/ratio);
                             self.streams[index - 1].height = Math.floor(this.videoHeight/ratio);
                             self.streams[index - 1].cropOffsetX = 0;
                             self.streams[index - 1].offsetX = Math.abs(Math.floor(self.width - this.videoWidth/ratio)/2);
                             self.streams[index - 1].cropOffsetY = 0;
                             self.streams[index - 1].offsetY = Math.abs(Math.floor(self.height - this.videoHeight/ratio)/2);
                           }, false);
                         }, 1000);

                         if (stream.getAudioTracks().length > 0) {
                           self.cacheTrack = stream.getAudioTracks()[0];
                         }
                       }
                       video.srcObject = stream;
                   },
    removeStream: function(id, isHardRemove) {
                    //TODO: fix this good and proper. need to decrease the index in streamLayer after filtering streams
                    var streamIndex = this.streams.map(stream => stream.track.id).indexOf(id);
                    if (streamIndex === -1) {
                      return;
                    }

                    this.streams[streamIndex].attached = false;

                    if (isHardRemove) {                    
                      var layerIndex = this.streamLayers.indexOf(streamIndex);
                      if (streamIndex === -1) {
                        return;
                      }
            
                      this.streamLayers.splice(layerIndex, 1);
                      this.streamLayers = this.streamLayers
                                            .map(index => index - (index > streamIndex ? 1 : 0));
                      this.streams = this.streams.filter(stream => stream.track.id !== id);
                    }
                    setTimeout(function() {
                      this.clearCanvas();
                    }.bind(this), 50);
                  },
   blinkAddAudioTrack: function(track) {
                         if (this.stream.getAudioTracks().length > 0) {
                           this.stream.removeTrack(this.stream.getAudioTracks()[0]);
                         }
                         this.stream.addTrack(track instanceof MediaStream ? track.getAudioTracks()[0] : track);
                         this.audioTrack = track;
                       },
     mozAddAudioTrack: function(track) {
                         const audContext = new AudioContext();
                         const source = audContext.createMediaStreamSource(track);
                         const dest = audContext.createMediaStreamDestination();
                         source.connect(dest);

                         const recorder = new MediaRecorder(dest.stream, {mimeType: 'audio/ogg; codecs=opus'});
                         let recData = [];

                         recorder.ondataavailable = function(e) {
                           if (e.data.size > 0) {
                             recData.push(e.data);
                           }
                         };

                         this.audioRecorders.push({
                           recorder: recorder,
                               data: recData
                         });
                         this.audioTrack = track;
                       },
    getStreamById: function(id) {
                     var stream = this.streams
                                    .filter(stream => stream.track.id === id);

                     return stream[0] || null;
                   },
   attachRecorder: function() {
                     if (!this.stream) {
                       console.log('stream undefined. Please choose something to record');
                       return;
                     }

                     if (this.recorder) {
                       return;
                     }

                     this.recorder = new MediaRecorder(this.stream, {mimeType: this.codecs[0]});

                     this.ondataavailable = function(e) {
                       if (e.data.size > 0) {
                         this.recData.push(e.data);
                       }
                     }
                   },
         attachTo: function(canvas) {
                     if (this.attachedCanvas === canvas) {
                       return;
                     }

                     this.attachedCanvas = canvas;
                     this.attachedContext = this.attachedCanvas.getContext('2d');
                     if (this.stream) {
                       this.playResult = true;
                       this.display();
                     }
                   },
        startDraw: function() {
                     if (!this.drawing) {
                       this.drawing = true;
                       this.delegate('subscribe.raf', this.draw, function logDrawToken(token) {
                         this.rafTokens.draw = token;
                       }.bind(this));
                       this.stream = this.canvasContext.canvas.captureStream(30);
                       this.streamVideo.srcObject = this.stream;
                       this.cacheTrack.forEach(track => {
                         this.addAudioTrack(track);
                       });
                       this.stream.muted = true;
                       if (this.attachedCanvas) {
                         this.playResult = true;
                         this.delegate('subscribe.raf', this.display, function logDisplayToken(token) {
                           this.rafTokens.display = token;
                         }.bind(this));
                       }
                     }
                   },
             draw: function(timestamp) {
                     this.streamLayers.forEach(function(index) {
                       var stream = this.streams[index] || null;
                       if (stream.attached) {
                         this.canvasContext.drawImage(stream.video, stream.cropOffsetX, stream.cropOffsetY, stream.cropWidth, stream.cropHeight,
                                                      stream.offsetX, stream.offsetY, stream.width, stream.height);
                       }
                     }.bind(this));
                   },
          display: function(timestamp) {
                     this.attachedContext.drawImage(this.streamVideo, 0, 0, this.width, this.height, 0, 0, this.attachedCanvas.width, this.attachedCanvas.height);
                   },
      clearCanvas: function() {
                     this.canvasContext.fillRect(0, 0, this.width, this.height);
                   },
      getDistance: function(x, y) {
                     return Math.sqrt(x**2 + y**2);
                   },
    setStreamDimensions: function(id, dims) {
                           var streamIndex = this.streams
                                               .map(stream => stream.track.id)
                                               .indexOf(id);

                           if (streamIndex == -1) {
                             return;
                           }

                           var streamWidth = dims.width || this.streams[streamIndex].width;
                           var streamHeight = dims.height || this.streams[streamIndex].height;

                           if (dims.offsetX) {
                             dims.offsetX = Math.max(0, Math.min(dims.offsetX, this.width - streamWidth));
                           }
                           if (dims.offsetY) {
                             dims.offsetY = Math.max(0, Math.min(dims.offsetY, this.height - streamHeight));
                           }

                           var allowedDims = ['width', 'height', 'offsetX', 'offsetY'];
                           for (var key in dims) {
                             if (allowedDims.indexOf(key) > -1) {
                               this.streams[streamIndex][key] = dims[key];
                             }
                           }
                           this.clearCanvas();
                         },
       cropStreams: function(obj) {
                      var streamIds = this.streams.map(stream => stream.track.id);
                      for (var key in obj) {
                        var streamIndex = streamIds.indexOf(key);
                        if (streamIndex === -1) {
                          continue;
                        }
                        var newCropWidth = Math.abs(obj[key][1][0] - obj[key][0][0]);
                        var newCropHeight = Math.abs(obj[key][1][1] - obj[key][0][1]);
                        this.streams[streamIndex].cropOffsetX = Math.min(obj[key][0][0], obj[key][1][0]);
                        this.streams[streamIndex].cropOffsetY = Math.min(obj[key][0][1], obj[key][1][1]);
                        this.streams[streamIndex].cropWidth = newCropWidth;
                        this.streams[streamIndex].cropHeight = newCropHeight;
                        this.streams[streamIndex].height = this.streams[streamIndex].width * newCropHeight / newCropWidth;
                        var maxX = this.width - this.streams[streamIndex].width;
                        var maxY = this.height - this.streams[streamIndex].height;
                        this.streams[streamIndex].offsetX = Math.max(0, Math.min(
                                                              this.streams[streamIndex].offsetX,
                                                              maxX
                                                            ));
                        this.streams[streamIndex].offsetY = Math.max(0, Math.min(
                                                              this.streams[streamIndex].offsetY,
                                                              maxY
                                                            ));
                      }
                      this.clearCanvas();
                    },
    startRecording: function() {
                      let compStream = this.stream;
                      this.recorder = new MediaRecorder(compStream, {mimeType: 'video/webm'});

                      this.recorder.ondataavailable = function(e) {
                        if (e.data.size > 0) {
                          this.recordedData.push(e.data);
                        }
                      }.bind(this);
                      this.recorder.onstop = function() {
                      }
                      this.recorder.onerror = function(e) {
                        console.log('error', e);
                      }
                      this.recorder.start(1000);
                    },
    pauseRecording: function() {
                      this.recorder.pause();
                      this.audioRecorders.forEach(audRec => {
                        audRec.recorder.pause();
                      });
                    },
   resumeRecording: function() {
                      this.recorder.resume();
                      this.audioRecorders.forEach(audRec => {
                        audRec.recorder.resume();
                      });
                    },
     stopRecording: function() {
                      this.recorder.stop();
                      return new Promise(resolve => {
                        setTimeout(() => resolve(), 2000);
                      });
                    },
       clearCanvas: function() {
                      setTimeout(function() {
                        this.canvasContext.fillRect(0, 0, this.width, this.height);
                      }.bind(this), 50);
                    },
          delegate: function() {
                      let args = Array.prototype.slice.call(arguments);
                      if (this.delegations.hasOwnProperty(args[0])) {
                        this.delegations[args[0]].forEach(fn => {
                          fn.apply(this, args.slice(1));
                        });
                      }
                    },
      ondelegation: function(type, fn) {
                      if (!this.delegations.hasOwnProperty(type)) {
                        this.delegations[type] = [];
                      }

                      this.delegations[type].push(fn);
                    }
}
