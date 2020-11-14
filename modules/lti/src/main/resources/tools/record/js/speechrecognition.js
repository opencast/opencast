var SpeechToText = function(audioAnalyser) {
  var Recog = (window.webkitSpeechRecognition || window.SpeechRecognition);
  this.isCapable = !!Recog;
  this.audioAnalyser = audioAnalyser;
  this.transcript = [];

  if (this.isCapable) {
    this.recog = new Recog();
    this.recog.continuous = true;
    this.recog.interimResults = true;
    this.recog.lang = 'en-ZA';
    this.isRecognising = false;
    this.recogTimestamp = null;
    this.grammarList = [];
    this.audio = new AudioContext();

    this.recog.onstart = e => {
      this.isRecognising = true;
    };
    this.recog.onend = () => {
      this.isRecognising = false;
    };
    this.recog.onresult = e => {
      let transcription = {
        timestamp: e.timeStamp,
        srResult: e.results
      };
      if (e.results['0'].isFinal) {
        this.transcript.push(transcription);
      }
      this.notifyDependencies('speechresult', transcription);
    };
  }

  var _audioTrack = null;
  Object.defineProperty(this, 'audioTrack', {
    get: function() {
      return _audioTrack;
    },
    set: function(track) {
      if (!_audioTrack && (track instanceof MediaStreamTrack || track instanceof MediaStream)) {
        _audioTrack = track;
        this.setTrack(track);
      }
    }
  });

  var micTimeout = null;
  var silenceLevel = 2;
  var isMicActive = false;

  Object.defineProperty(this, 'micActive', {
    get: function() {
      return isMicActive;
    },
    set: function(bool) {
      if (typeof bool == 'boolean') {
        isMicActive = bool;
        var fn = isMicActive ? 'start' : 'stop';
        if (isMicActive !== this.isRecognising) {
          this.recog[fn]();
        }
      }
    }
  });

  this.audioAnalyser.on('magnitude', magnitude => {
    if (magnitude >= silenceLevel) {
      clearTimeout(micTimeout);
      micTimeout = null;
      if (!isMicActive) {
        this.micActive = true;
      }
    }
    else if (magnitude < 2 && this.micActive && !micTimeout) {
      micTimeout = setTimeout(function() {
        this.micActive = false;
        clearTimeout(micTimeout);
        micTimeout = null;
      }.bind(this), 1000);
    }
  });

  var _subscriptions = {};
  Object.defineProperty(this, 'subscriptions', {
    get: function() {
      return _subscriptions;
    },
  });

  this.notifyDependencies('capability', this.isCapable);
}

SpeechToText.prototype = {
  constructor: SpeechToText,
  setLanguage: function(lang) {
    this.recog.lang = lang;
  },
  addGrammar: function(grammar) {
    this.grammarList.push(grammar);
    var speechRecogList = new (webkitSpeechGrammarList || SpeechGrammarList)();
    this.grammarList.forEach(function(grammar) {
      speechRecogList.addFromString(grammar, 1);
    });
    this.recog.grammars = speechRecogList;
  },
  setTrack: function(track) {
    setTimeout(() => {
      var source = this.audio.createMediaStreamSource(track);
      var delay = this.audio.createDelay(2.0);
      source.connect(delay);
      source.connect(this.audio.destination);
      delay.connect(this.audio.destination);
      thing = source;
      this.recog.audioTrack = source.mediaStream.getAudioTracks()[0];
    }, 2000);
  },
  on: function(ev, fn) {
    if (ev === 'capability') {
      return fn(this.isCapable);
    }

    if (!this.subscriptions.hasOwnProperty(ev)) {
      this.subscriptions[ev] = {};
    }

    var currentSubscriptions = Object.keys(this.subscriptions);

    let randString = null;
    do {
      randString = (1 + Math.random()).toString(36).substring(2, 10);
    } while (currentSubscriptions.indexOf(randString) > -1);

    this.subscriptions[ev][randString] = fn;

    return randString;
  },
  off: function(ev, token) {
    var result = false;
    if (this.subscriptions.hasOwnProperty(ev) &&
         this.subscriptions[ev].hasOwnProperty(token)) {
      delete this.subscriptions[ev][token];
      result = true;
    }
    return result;
  },
  notifyDependencies: function(ev, val) {
    if (this.subscriptions.hasOwnProperty(ev)) {
      for (var key in this.subscriptions[ev]) {
        this.subscriptions[ev][key](val);
      }
    }
  }
}
