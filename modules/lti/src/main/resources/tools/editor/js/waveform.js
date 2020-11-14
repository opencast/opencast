function Waveform(opts) {
  this.audioContext = new AudioContext();
  this.oCanvas = document.createElement('canvas');
  this.buffer = {};
  this.WIDTH = 0;
  this.HEIGHT = 0;
  this.channelData = [];
  this.waveformImage = '';
  this.audioBuffer = null;

  this.aveRMS = 0;
  this.peakRMS = 0;

  this.numberSamples = 100000;
  this.waveformType = 'img';
  this.drawWaveform = this.drawCanvasWaveform;

  if (opts.width && opts.height) {
    this.setDimensions(opts.width, opts.height);
  }
  if (opts.samples) {
    this.numberSamples = opts.samples;
  }
  if (opts.type && opts.type === 'svg') {
    this.waveformType = 'svg';
    this.drawWaveform = this.delegateToWorker;
    this.worker = null;
  }
  if (opts.media) {
    this.generateWaveform(opts.media)
      .then(() => {
        this.getAudioData();
        this.drawWaveform();
        if (this.waveformType != 'svg') {
          _completeFuncs.forEach(fn => {
            fn(this.waveformImage || this.svgPath, this.waveformType);
          });
        }
      })
      .catch(e => console.log(e));
  }

  var _completeFuncs = [];
  Object.defineProperty(this, 'oncomplete', {
    get: function() {
      return _completeFuncs;
    },
    set: function(fn, opt) {
      if (typeof fn == 'function') {
        if (this.waveformImage || this.svgPath) {
          fn(this.waveformImage || this.svgPath, this.svgLength);
          return;
        }

        _completeFuncs.push(fn);
      }
    }
  });
}

Waveform.prototype = {
  constructor: Waveform,
  setDimensions: function(width, height) {
    this.oCanvas.width = width;
    this.WIDTH = width;
    this.oCanvas.height = height;
    this.HEIGHT = height;
    this.ocCtx = this.oCanvas.getContext('2d');
  },
  decodeAudioData: function(arraybuffer) {
    return new Promise((resolve, reject) => {
      new Promise((res, rej) => {
        if (arraybuffer instanceof ArrayBuffer) {
          res(arraybuffer);
        }
        else if (arraybuffer instanceof Blob) {
          let reader = new FileReader();
          reader.onload = function() {
            res(reader.result);
          }
          reader.readAsArrayBuffer(arraybuffer);
        }
      })
      .then(buffer => {
        this.audioContext.decodeAudioData(buffer)
          .then(audiobuffer => {
            this.buffer = audiobuffer;
            resolve();
          })
          .catch(e => {
            reject(e);
          })
      })
      .catch(e => {
        reject(e);
      })
    })
  },
  getAudioData: function(buffer) {
    buffer = buffer || this.buffer;
    this.channelData = this.dropSamples(buffer.getChannelData(0), this.numberSamples);
  },
  drawCanvasWaveform: function(amp) {
    amp = amp || 1;
    this.ocCtx.fillStyle = '#b7d8f9';
    this.ocCtx.fillRect(0, 0, this.WIDTH, this.HEIGHT);
    this.ocCtx.lineWidth = 1;
    this.ocCtx.strokeStyle = '#38597a';
    let sliceWidth = this.WIDTH * 1.0 / this.channelData.length;
    let x = 0;

    this.ocCtx.beginPath();
    this.ocCtx.moveTo(x, this.channelData[0] * this.HEIGHT / 128.0 / 2);

    this.channelData.forEach(sample => {
      let v = sample * amp;
      let y = this.HEIGHT * (1 + v) / 2;
      this.ocCtx.lineTo(x, y);
      this.aveRMS += sample * sample;
      this.peakRMS = Math.max(sample * sample, this.peakRMS);
      x += sliceWidth;
    });
    this.ocCtx.lineTo(this.WIDTH, this.HEIGHT/2);
    this.ocCtx.stroke();
    this.aveRMS = Math.sqrt(this.aveRMS / this.channelData.length);
    this.aveDBs = 20 * Math.log(this.aveRMS) / Math.log(10);
    this.waveformImage = this.oCanvas.toDataURL();
  },
  dropSamples: function(data, requestedLength) {
    let divider = Math.max(parseInt(data.length / requestedLength), 1);
    return data.filter((sample, i) => i % divider === 0);
  },
  generateWaveform: function(arraybuffer) {
    return this.decodeAudioData(arraybuffer);
  },
  delegateToWorker: function() {
    if (!this.worker) {
      this.worker = new Worker('js/svgworker.js');
      this.worker.addEventListener('message', this.workerCommunication.bind(this), false);
      this.worker.postMessage(this.channelData);
    }
  },
  workerCommunication: function(e) {
    switch(e.data.type) {
      case 'path':
        this.setSVGpath(e.data.path, e.data.length);
        this.worker.removeEventListener('message', this.workerCommunication.bind(this), false);
        this.worker.terminate();
        this.worker = null;
    }
  },
  setSVGpath: function(path, len) {
    this.svgPath = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    this.svgLength = len;

    this.svgPath.setAttribute('d', path);
    this.svgPath.setAttribute('vector-effect', 'non-scaling-stroke');
    this.svgPath.setAttribute('stroke-width', '0.5px');

    this.oncomplete.forEach(fn => fn(this.svgPath, this.svgLength));
  }
};
