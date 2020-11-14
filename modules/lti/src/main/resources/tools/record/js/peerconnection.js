var pc_config = {"iceServers":[
                  {urls: 'stun:stun.sipgate.net:3478'},
                ]};

var streamConstraints = {
  mandatory: {
    OfferToReceiveAudio: true,
    OfferToReceiveVideo: true
  }
}

var videoContainer = document.querySelector('#videoStreams');

if (window.hasOwnProperty('InstallTrigger')) {
  //Firefox has window.InstallTrigger (for now)
  streamConstraints = {
    offerToReceiveVideo: true,
    offerToReceiveAudio: true
  };
}

var PeerConnection = function(peerDetails, isInitiator) {
  if (typeof peerDetails == 'string') {
    this.id = peerDetails;
  }
  else {
    this.id = peerDetails.peers[0];
    this.room = peerDetails.room;
  }
  this.isCaller = isInitiator || false;
  this.candidateQueue = [];
  this.pc = new RTCPeerConnection(pc_config);
  this.noVideoElement = false;

  var self = this;

  this.pc.onicecandidate = evt => {
    if (evt.candidate) {
      details = {
                 type: 'candidate',
        sdpMLineIndex: evt.candidate.sdpMLineIndex,
               sdpMid: evt.candidate.sdpMid,
            candidate: evt.candidate.candidate
      };
      socket.emit('peerConnection', {target: self.id, details: details});
    }
  };

  this.pc.addEventListener('addstream', event => {
    this.stream = event.stream;
  });

  this.pc.oniceconnectionstatechange = e => {
    if (['connected', 'completed'].indexOf(this.pc.iceConnectionState) > -1 && !this.noVideoElement) {
      //hacky...
      if (this.stream && !this.streamElement) {
        this.streamElement = this.stream;
        var right = 0;
        var lastVid = videoContainer.querySelector('video[data-peer]:last-of-type');
        if (lastVid) {
          var dims = lastVid.getBoundingClientRect();
          var parentDims = videoContainer.getBoundingClientRect();
          right = parentDims.right - dims.left + 10;
        }
        var element = this.streamElement;
        element.style.right = right + 'px';
        videoContainer.appendChild(element);
        element.addEventListener('click', setActiveVideo, false);
      }
    }
    if (this.pc.iceConnectionState === 'connected') {
      console.log('connected');
      this.oncomplete.forEach(func => {
        func();
      });
    }
  };

  if (this.isCaller && window.localStream) {
    this.pc.addStream(window.localStream);
    this.dataChannel = this.pc.createDataChannel('channel');
    this.sendOffer();
  }
  else if (!this.isCaller) {
    this.pc.ondatachannel = e => {
      self.dataChannel = e.channel;
    };
  }

  var mediaElement = null;

  Object.defineProperty(this, 'streamElement', {
      get: function() {
             return mediaElement;
           },
      set: function(stream) {
             mediaElement = document.createElement(stream.getVideoTracks().length > 0 ? 'video' : 'audio');
             mediaElement.src = URL.createObjectURL(stream);
             mediaElement.autoplay = true;
             mediaElement.muted = true;
             mediaElement.setAttribute('data-peer', this.id);
           }
    }
  );

  var completeFuncs = [];

  Object.defineProperty(this, 'oncomplete', {
      get: function() {
             return completeFuncs;
           },
      set: function(func) {
             if (typeof func != 'function') {
               return;
             }

             completeFuncs.push(func);
           }
  });

  var _closeFuncs = [];

  Object.defineProperty(this, 'onclose', {
      get: function() {
             return _closeFuncs;
           },
      set: function(func) {
             if (typeof func != 'function') {
               return;
             }

             _closeFuncs.push(func);
           }
  });
};

PeerConnection.prototype = {
           constructor: PeerConnection,
             sendOffer: function() {
                          var self = this;
                          this.pc.createOffer(offer => {
                            self.pc.setLocalDescription(offer);
                            socket.emit('peerConnection', {target: self.id, details: offer});
                          }, self.offerFailed, streamConstraints);
                        },
           offerFailed: function(e) {
                          console.log('failed offer', e);
                        },
            sendAnswer: function() {
                          var self = this;
                          this.pc.createAnswer(offer => {
                             self.pc.setLocalDescription(offer);
                             socket.emit('peerConnection', {target: self.id, details: offer});
                          }, self.answerFailed, streamConstraints);
                        },
          answerFailed: function(e) {
                          console.log('failed answer', e);
                        },
         handleRequest: function(data) {
                          let details = data.details;
                          switch (details.type) {
                            case 'offer':
                              this.handleOffer(details);
                              break;

                            case 'answer':
                              this.handleAnswer(details);
                              break;

                            case 'candidate':
                              this.addCandidate(details);
                          }
                       },
          handleOffer: function(details) {
                         var self = this;
                         if (!this.pc.remoteDescription.sdp) {
                           this.pc.setRemoteDescription(new RTCSessionDescription(details), function() {
                               console.log('adding ice candidates after setRemoteDescription');
                               self.candidateQueue.forEach(candidate => {
                                 self.pc.addIceCandidate(candidate);
                               });
                             });
                         }
                         this.sendAnswer();
                       },
         handleAnswer: function(details) {
                         this.pc.setRemoteDescription(new RTCSessionDescription(details));
                       },
         addCandidate: function(details) {
                         let candidate = new RTCIceCandidate({sdpMLineIndex:details.sdpMLineIndex, sdpMid:details.sdpMid, candidate:details.candidate});
                         if (this.pc.hasOwnProperty('remoteDescription')) {
                           this.pc.addIceCandidate(candidate);
                         }
                         else {
                           this.candidateQueue.push(candidate);
                         }
                       },
  createStreamElement: function(stream) {
                         this.streamURL = URL.createObjectURL(stream);
                         if (stream.getVideoTracks().length === 0) {
                           this.streamElement = document.createElement('audio');
                         }
                         else {
                           this.streamElement = document.createElement('video');
                         }
                         this.streamElement.src = this.streamURL;
                       }
};

function setActiveVideo(e) {
  if (this.classList.contains('active')) {
  }

  var currentActive = this.parentNode.querySelector('video.active');
  if (currentActive) {
    let thisDims = this.getBoundingClientRect();
    currentActive.style.right = thisDims.right;
    currentActive.classList.remove('active');
  }

  var parentDims = this.getBoundingClientRect();

  this.classList.add('active');
}
