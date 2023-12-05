const DOMAIN = 'https://' + location.host;

function Editor() {
  this.fileUpload = document.getElementsByClassName('fileUpload')[0];
  this.uploadForms = document.getElementsByTagName('form');
  this.endpoints = {
    addTrack: '/ingest/addTrack',
    addPartialTrack: '/ingest/addPartialTrack',
    getMediaPackage: '/ingest/createMediaPackage',
    addMediaPackage: '/ingest/addMediaPackage',
    dummyIngest: '/ingest/ingest/dummy',
    trimIngest: '/ingest/ingest/uct-process-cutting-and-publish',
    addCatalog: '/ingest/addCatalog',
    addDCCatalog: '/ingest/addDCCatalog',
    ingest: '/ingest/ingest/ng-partial-publish',
    createSmil: '/smil/create',
    smilClip: '/smil/addClip'
  };

  this.notificationModal = document.getElementById('notificationModal');
  this.notificationToggler = document.getElementById('notifyModalToggle');

  this.offlineEnabler = document.getElementById('offlineToggle').nextElementSibling;
  this.notificationEnabler = document.getElementById('notificationsToggle').nextElementSibling;

  this.timeDisplay = document.querySelector('.videoControls span:nth-of-type(3)');

  var _isPlaying = false;
  Object.defineProperty(this, 'isPlaying', {
    get: function() {
      return _isPlaying;
    },
    set: function(state) {
      _isPlaying = state;
      if (_isPlaying) {
        this.keepTime();
      }
    }
  });

  this.seektrack = document.getElementsByClassName('seektrack')[0];
  this.seeker = this.seektrack.querySelector('.seeker');
  this.waveformWrapper = document.querySelector('.waveform');
  this.waveSeeker = document.querySelector('.waveSeeker');
  this.seekerSVG = document.querySelector('.waveSeeker svg');

  this.togglePlay = document.getElementById('isPlaying');
  this.waveWidth = this.waveSeeker.clientWidth;
  this.waveHeight = 64;
  this.strokeColour = '#38597a';
  this.waveforms = [];
  this.recorderWindow = null;

  this.waveformContainer = this.waveformWrapper.querySelector('.waveContainer');
  this.waveformImage = document.querySelector('.waveform img');
  this.waveformSVG = document.querySelector('.waveform svg:nth-of-type(1)');
  this.bgSVG = document.querySelector('.waveform svg:nth-of-type(2)');
  this.clipSVG = document.getElementById('svgClip');
  this.svgLength = 0;
  this.wavePos = this.waveSeeker.querySelector('.wavePosition');
  this.wavePosImage = this.waveSeeker.querySelector('img');
  this.waveSelectionDisplay = this.waveformWrapper.querySelector('.selection');
  this.snippingTool = document.getElementById('snip');

  this.videoSeeker = document.getElementsByClassName('seeker')[0];

  this.scaleFactor = -1/2000;
  this.scaleTally = 0;
  this.scaleOffset = 0;
  this.pointOffset = 0;
  this.scaleTranslate = 0;
  this.oldScale = 1;

  this.recorderData = {
    video: [],
    audio: []
  };
  this.recorderTarget = '';

  this.zoomLevel = 1;

  this.mediaDuration = 0;

  this.titleElement = document.querySelector('#info span:nth-child(1)');
  this.titleInput = document.querySelector('input[name=title]');

  this.dateElement = document.querySelector('#info span:nth-child(2)');
  this.dateInput = document.querySelector('input[name=date]');

  this.locationInput = document.querySelector('input[name=location]');
  this.locationElement = document.querySelector('#info span:nth-child(3)');

  var _video = document.querySelector('video');

  Object.defineProperty(this, 'video', {
    get: function() {
      return _video;
    },
    set: function(blob) {
      if (blob instanceof Blob || blob[0] instanceof Blob) {
        let mimeType = blob.type || blob[0].type;
        _video.src = URL.createObjectURL(blob);
        document.getElementsByClassName('waveLoader')[0].classList.add('loading');
      }
    }
  });

  var _segments = [];
  this.segmentTimestamps = [];
  this.segmentContainer = document.getElementById('segments');
  Object.defineProperty(this, 'cutSegment', {
    get: function() {
      return _segments;
    },
    set: function(pos) {
      if (!Array.isArray(pos) || pos.length != 2) {
        return;
      }

      this.waveSelectionDisplay.style.width = '0';

      let segmentLength = _segments.length;

      _segments.push(pos);
      _segments = _segments.sort((a, b) => a[0] - b[0])
        .reduce((collect, current, i) => {
          let lastIndex = collect.length - 1;
          if (lastIndex > -1 && current[0] <= collect[lastIndex][1]) {
            collect[lastIndex][1] = Math.max(current[1], collect[lastIndex][1]);
          }
          else {
            collect.push(current);
          }
          return collect;
        }, []);

      this.refreshSegments();
    }
  });

  Object.defineProperty(this, 'getActiveSegments', {
    get: function() {
      let cuts = this.cutSegment.map(cut => {
        return {
          start: cut[0],
          end: cut[1]
        };
      });

      let totalLength = cuts.length;
      let actives = cuts.reduce((collect, cut, i) => {
        if (cut.start > 0) {
          let start = cuts[i - 1] ? cuts[i - 1].end : 0;
          let end = cuts[i].start;
          collect.push({start: start, end: end, duration: end - start});
        }
        if (i === totalLength - 1 && cuts[i].end < this.svgLength) {
          collect.push({start: cuts[i].end, end: this.waveWidth, duration: this.waveWidth - cuts[i].end});
        }

        return collect;
      }, []).filter(segment => segment.duration > 0);

      return actives;
    }
  });

  this.removeCutSegment = function removeCutSegment(index) {
    if (index > -1 && index < _segments.length) {
      _segments = _segments.filter((segment, i) => i !== index);
      this.refreshSegments();
    }
  };

  var _title = 'Some Title';
  var _presenters = [];
  var _location = 'Over here';
  var _date = '';

  Object.defineProperty(this, 'title', {
    get: function() {
      return _title;
    },
    set: function(title) {
      if (typeof title == 'string') {
        _title = title;
        this.titleElement.innerText = title;
      }
    }
  });

  Object.defineProperty(this, 'location', {
    get: function() {
      return _location;
    },
    set: function(loc) {
      _location = loc;
      this.locationElement.innerText = loc;
    }
  });

  Object.defineProperty(this, 'date', {
    get: function() {
      return _date;
    },
    set: function(d) {
      let dateArr = d.split('-');
      this.dateElement.innerText = `${dateArr[2]} ${getMonthName(+dateArr[1])} ${dateArr[0]}`;
    }
  });

  this.detectCapabilities();
}

function getMonthName(num) {
  var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sept', 'Oct', 'Nov', 'Dec'];
  return (months[num - 1]);
}

Editor.prototype = {
  constructor: Editor,
  editFile: function(e) {
    if (e.target.files[0]) {
      let file = e.target.files[0];
      let id = utils.hash(file.name);
      let index = this.recorderData.video.push(file);
      if (!this.video.src) {
        this.video = this.recorderData.video[index - 1];
        this.video.onplaying = function() {
          this.isPlaying = true;
        }.bind(this);
        this.video.onpause = function() {
          this.isPlaying = false;
        }.bind(this);
        this.waveforms.push(new Waveform({type: 'svg', samples: 100000, media: this.recorderData.video[index - 1] }));
        this.waveforms[0].oncomplete = function(image, numSamples) {
          document.getElementsByClassName('waveLoader')[0].classList.remove('loading');
          this.waveWidth = this.waveSeeker.clientWidth;

          this.waveformSVG.setAttribute('viewBox', `0 -1 ${numSamples} 2`);
          this.waveformSVG.setAttribute('width', this.waveWidth);
          this.waveformSVG.setAttribute('height', this.waveHeight);
          this.bgSVG.setAttribute('viewBox', `0 -1 ${numSamples} 2`);
          this.bgSVG.setAttribute('width', this.waveWidth);
          this.bgSVG.setAttribute('height', this.waveHeight);
          this.seekerSVG.setAttribute('viewBox', `0 -1 ${numSamples} 2`);
          this.seekerSVG.setAttribute('width', this.waveWidth);
          this.seekerSVG.setAttribute('height', this.waveHeight);
          while (this.clipSVG.firstChild) {
            this.clipSVG.removeChild(this.clipSVG.firstChild);
          };
          this.clipSVG.appendChild(
            this.createNSelement('http://www.w3.org/2000/svg', 'rect', {x: 0, y: 0, width: this.waveWidth, height: this.waveHeight})
          );

          image.setAttribute('stroke', this.strokeColour);
          this.waveformSVG.appendChild(image);
          this.bgSVG.appendChild(image.cloneNode(true));
          this.seekerSVG.appendChild(image.cloneNode(true));
          this.svgLength = numSamples;
          this.mediaDuration = this.waveforms[0].buffer.duration;
          this.timeDisplay.textContent = this.parseVideoTime(0);
          this.waveformWrapper.setAttribute('data-starttime', this.parseVideoTime(0));
          this.waveformWrapper.setAttribute('data-endtime', this.parseVideoTime(this.video.duration || this.mediaDuration));
        }.bind(this);
      }
    }
  },
  getSegment: function(pos) {
    let segPos = [];
    this.cutSegment.some(segment => {
      if (segment[0] < pos && pos < segment[1]) {
        segPos = segment;
        return;
      }
    });

    return segPos;
  },
  adjustSegment: function(e) {
    let _this = e.target;
    let sibling = _this.parentNode.querySelector(`input[name=${_this.name === 'start' ? 'end' : 'start'}]`);
    let segment = _this.name === 'start' ? [+_this.value, +sibling.value] : [+sibling.value, +_this.value];
    segment = [
                parseInt(segment[0] * this.waveWidth / this.mediaDuration),
                parseInt(segment[1] * this.waveWidth / this.mediaDuration),
              ];
    console.log(segment);
    let id = +(_this.parentNode.getAttribute('data-id').replace('id_', ''));
    this.cutSegment[id] = segment;
    this.cutSegment = segment;
  },
  removeSegment: function(e) {
    let segmentId = +(e.target.parentNode.getAttribute('data-id').split('_'))[1];
    this.removeCutSegment(segmentId);
  },
  refreshSegments: function() {
      this.clipSVG.innerHTML = '';
      if (this.cutSegment.length > 0) {
        let cuts = this.cutSegment.map(cut => {
          return {
            start: cut[0],
            end: cut[1]
          };
        });

        let totalLength = cuts.length;
        cuts.forEach((cut, i) => {
          if (cut.start > 0) {
            let start = cuts[i - 1] ? cuts[i - 1].end : 0;
            let end = cuts[i].start;
            let rect = this.createNSelement('http://www.w3.org/2000/svg', 'rect', {x: start, y: 0, width: end - start, height: this.waveHeight});
            this.clipSVG.appendChild(rect);
          }
          if (i === totalLength - 1 && cuts[i].end < this.svgLength) {
            this.clipSVG.appendChild(
                this.createNSelement('', 'rect', {x: cuts[i].end, y: 0, width: this.waveWidth - cuts[i].end, height: this.waveHeight})
            );
          }
        });
        this.segmentTimestamps = this.cutSegment.map(segment => 
          [segment[0] * this.mediaDuration / this.waveWidth, segment[1] * this.mediaDuration / this.waveWidth]
        );
      }
      else {
        this.clipSVG.appendChild(
          this.createNSelement('http://www.w3.org/2000/svg', 'rect', {x: 0, y: 0, width: this.waveWidth, height: this.waveHeight})
        );
        this.segmentTimestamps = [];
      }


      [...this.segmentContainer.querySelectorAll('input[type=text]')]
        .forEach(input => input.removeEventListener('change', this.adjustSegment.bind(this), false));
      [...this.segmentContainer.querySelectorAll('button')]
        .forEach(button => button.removeEventListener('click', this.removeSegment.bind(this), false));

      this.segmentContainer.innerHTML = '';
      this.cutSegment.forEach((segment, i) => {
        let listItem = this.createElement('li', {data: {id: `id_${i}`}});
        let from = this.createElement('span', 'Start:');
        let start = this.createElement('input', {type: 'text', attribs: {name: 'start', value: (segment[0] / this.waveWidth * this.mediaDuration).toFixed(3)}});
        let to = this.createElement('span', 'End:');
        let end = this.createElement('input', {type: 'text', attribs: {name: 'end', value: (segment[1] / this.waveWidth * this.mediaDuration).toFixed(3)}});
        let seconds = this.createElement('span', '(in seconds)');
        let remove = this.createElement('button', {class: 'remove pull-right'});

        listItem.appendChild(from);
        listItem.appendChild(start);
        listItem.appendChild(to);
        listItem.appendChild(end);
        listItem.appendChild(seconds);
        listItem.appendChild(remove);
        this.segmentContainer.appendChild(listItem);
        start.addEventListener('change', this.adjustSegment.bind(this), false);
        end.addEventListener('change', this.adjustSegment.bind(this), false);
        remove.addEventListener('click', this.removeSegment.bind(this), false);
      });
  },
  detectCapabilities: function() {
    let isServiceWorker = 'serviceWorker' in navigator;
    let isPushManager = 'PushManager' in window;

    if (isServiceWorker) {
      this.offlineEnabler.classList.remove('unsupported');
    }
    if (isServiceWorker && isPushManager) {
      this.notificationEnabler.classList.remove('unsupported');
      this.notificationEnabler.previousElementSibling.addEventListener('change', this.registerNotifications, false);
    }
  },
  registerNotifications: function(e) {
    return navigator.serviceWorker.register('notification-worker.js')
             .then(registration => {
               console.log('done registering');
               return registration;
             })
             .catch(err => console.log('unable to register service worker', err));
  },
  startSeekDrag: function(e) {
    e.preventDefault();
    e.stopImmediatePropagation();
  },
  dragSeeker: function(e) {
    e.preventDefault();
    e.stopImmediatePropagation();
  },
  stopSeekDrag: function(e) {
    e.preventDefault();
    e.stopImmediatePropagation();
  },
  seekTo: function(e) {
    this.video.currentTime = this.mediaDuration * (e.layerX || e.detail.layerX) / this.waveWidth;
    this.seeker.style.transform = `translateX(${e.layerX || e.detail.layerX}px)`;
    this.timeDisplay.textContent = this.parseVideoTime(this.video.currentTime);
  },
  recvMessage: function(e) {
    if (typeof e.data === 'string') {
      let msg = e.data;
      switch(msg) {

        case 'recorder':
          e.source.postMessage('editor', DOMAIN);
          this.recorderWindow = e.source;
          clearTimeout(this.reload);
          this.reload = null;
          break;

        case 'video':
        case 'audio':
          this.recorderTarget = msg;
          break;

        case 'ping':
          e.source.postMessage('pong', DOMAIN);
          break;
      }
    }
    else if (Array.isArray(e.data)) {
      if (e.data[0] instanceof Blob && this.recorderTarget) {
        this.recorderData[this.recorderTarget] = this.recorderData[this.recorderTarget].concat(e.data);
        if (this.recorderTarget == 'video') {
          this.video = e.data;
          this.video.onloadedmetadata = function() {
            console.log(this)
          }
          this.video.onplaying = function() {
            console.log('i am playing');
          };
          this.video.onpause = function() {
            console.log('i am paused');
          };
        }
      }
    }
    else if (e.data instanceof Blob) {
      if (this.recorderTarget === 'video') {
        this.video = e.data;
      }
      else if (this.recorderTarget === 'audio') {
        this.waveforms.push(
          new Waveform({type: 'svg', width: this.waveWidth, height: this.waveHeight, media: e.data})
        );
        this.waveforms[0].oncomplete = function(image) {
          document.getElementsByClassName('waveLoader')[0].classList.remove('loading');
          this.waveWidth = this.waveSeeker.clientWidth;

          this.waveformSVG.setAttribute('viewBox', `0 -1 ${numSamples} 2`);
          this.waveformSVG.setAttribute('width', this.waveWidth);
          this.waveformSVG.setAttribute('height', this.waveHeight);
          this.bgSVG.setAttribute('viewBox', `0 -1 ${numSamples} 2`);
          this.bgSVG.setAttribute('width', this.waveWidth);
          this.bgSVG.setAttribute('height', this.waveHeight);
          while (this.clipSVG.firstChild) {
            this.clipSVG.removeChild(this.clipSVG.firstChild);
          };
          this.clipSVG.appendChild(
            this.createNSelement('http://www.w3.org/2000/svg', 'rect', {x: 0, y: 0, width: this.waveWidth, height: this.waveHeight})
          );

          image.setAttribute('stroke', this.strokeColour);
          this.waveformSVG.appendChild(image);
          this.bgSVG.appendChild(image.cloneNode(true));
          this.svgLength = numSamples;
          this.mediaDuration = this.waveforms[0].buffer.duration;
        }.bind(this);
      }
    }
  },
  togglePlayPause: function(e) {
    if (e.target.checked) {
      this.video.play();
      this.checkSegmentTime = setInterval(this.seekVideoTime.bind(this), 16);
    }
    else {
      this.video.pause();
      clearInterval(this.checkSegmentTime);
    }
  },
  seekVideoTime: function() {
    let curTime = this.video.currentTime;
    this.segmentTimestamps.some(segment => {
      if (curTime > segment[0] && curTime < segment[1] && !this.video.paused) {
        clearInterval(this.checkSegmentTime);
        this.video.pause();
        this.video.currentTime = segment[1];
        let restart = setTimeout(function() {
          if (this.video.paused) {
            this.video.play();
            this.checkSegmentTime = setInterval(this.seekVideoTime.bind(this), 16);
          }
        }.bind(this), 50);
     //   restart;
        return;
      }
    });
    this.seeker.style.transform = `translateX(${this.video.currentTime / this.mediaDuration * this.waveWidth}px)`;
  },
  videoStopped: function(e) {
    this.togglePlay.checked = false;
  },
  addEventListeners: function() {
    this.fileUpload.addEventListener('change', this.editFile.bind(this), false);
    [...this.uploadForms].forEach(form => form.addEventListener('submit', this.uploadEdits.bind(this), false));
    this.seeker.addEventListener('pointerdown', this.startSeekDrag.bind(this), false);
    this.seeker.addEventListener('pointermove', this.dragSeeker.bind(this), false);
    this.seeker.addEventListener('pointerup', this.stopSeekDrag.bind(this), false);
    this.seektrack.addEventListener('click', this.seekTo.bind(this), false);
    this.bgSVG.addEventListener('click', this.seekTo.bind(this), false);
    window.addEventListener('message', this.recvMessage.bind(this), false);
    this.togglePlay.addEventListener('change', this.togglePlayPause.bind(this), false);
    this.waveformSVG.addEventListener('pointerdown', this.startWaveSelection.bind(this), false);
    this.waveformSVG.addEventListener('pointermove', this.waveSelection.bind(this), false);
    this.waveformSVG.addEventListener('pointerup', this.endWaveSelection.bind(this), false);
    this.waveformSVG.addEventListener('pointerout', this.endWaveSelection.bind(this), false);
    this.waveformSVG.addEventListener('drag', this.preventDefault, false);
    this.waveformSVG.addEventListener('dragstart', this.preventDefault, false);
    this.waveformSVG.addEventListener('dragover', this.preventDefault, false);
    this.waveformSVG.addEventListener('dragmove', this.preventDefault, false);
    this.waveformSVG.addEventListener('dragend', this.preventDefault, false);
    this.waveformSVG.addEventListener('drop', this.preventDefault, false);
    this.waveformWrapper.addEventListener('mousewheel', this.scaleWaveform.bind(this), false);
    this.waveSeeker.addEventListener('pointerdown', this.startMoveWavePos.bind(this), false);
    this.waveSeeker.addEventListener('pointermove', this.moveWavePos.bind(this), false);
    this.waveSeeker.addEventListener('pointerup', this.endMoveWavePos.bind(this), false);
    this.waveSeeker.addEventListener('pointerout', this.endMoveWavePos.bind(this), false);
    this.video.addEventListener('ended', this.videoStopped.bind(this), false);
    this.waveSelectionDisplay.addEventListener('mousedown', function(e) {
      e.preventDefault();
      let layerX = parseInt(e.target.style.left) + e.layerX;
      this.waveformSVG.dispatchEvent(new CustomEvent('pointerdown', {detail: {layerX: layerX}}));
    }.bind(this), false);
    this.snippingTool.addEventListener('mousedown', this.preventDefault, false);
    this.snippingTool.addEventListener('click', this.snipSelection.bind(this), false);
    this.titleInput.addEventListener('change', this.setTitle.bind(this), false);
    this.locationInput.addEventListener('change', this.setLocation.bind(this), false);
    this.dateInput.addEventListener('change', this.setDate.bind(this), false);
    this.titleInput.addEventListener('input', this.setTitle.bind(this), false);
    this.locationInput.addEventListener('input', this.setLocation.bind(this), false);

    this.video.addEventListener('ended', function() {
      setTimeout(function() {
        this.seeker.style.transform = 'translate(0, 0)';
      }.bind(this), 300);
    }.bind(this), false);
  },
  preventDefault: function(e) {
    e.preventDefault();
    e.stopImmediatePropagation();
  },
  startWaveSelection: function(e) {
    e.preventDefault();
    e.layerX = e.layerX || e.detail.layerX;
    this.startedSelecting = true;
    this.selectionStart = e.layerX;
    this.selectionEnd = e.layerX;
    this.anchor = e.layerX;
    this.waveSelectionDisplay.style.left = e.layerX + 'px';
    this.waveSelectionDisplay.style.width = '0';
  },
  waveSelection: function(e) {
    if (this.startedSelecting) {
      e.preventDefault();
      let min = Math.min(e.layerX, this.selectionStart);
      let max = Math.max(e.layerX, this.selectionEnd);
      (e.layerX > this.anchor ? max = e.layerX : min = e.layerX)
      this.selectionStart = min;
      this.selectionEnd = max;
      this.waveSelectionDisplay.style.left = min + 'px';
      this.waveSelectionDisplay.style.width = this.selectionEnd - this.selectionStart + 'px';
    }
  },
  endWaveSelection: function(e) {
    e.preventDefault();
    e.stopImmediatePropagation();
    if (this.selectionStart === this.selectionEnd && this.startedSelecting) {
      this.seektrack.dispatchEvent(new CustomEvent('click', {detail: {layerX: this.selectionStart}}));
    }
    if (this.startedSelecting && (e.type === 'pointerup' || e.relatedTarget === document || !e.relatedTarget.classList.contains('selection')
        || !e.relatedTarget.id == 'snip')) {
      this.startedSelecting = false;
      if (e.layerX <= 0) {
        this.selectionStart = 0;
        this.waveSelectionDisplay.style.left = '0px';
        this.waveSelectionDisplay.style.width = this.selectionEnd + 'px';
      }
      if (e.layerX >= this.waveWidth) {
        this.selectionEnd = this.waveWidth;
        this.waveSelectionDisplay.style.width = this.selectionEnd - this.selectionStart + 'px';
      }
      if (e.relatedTarget && e.relatedTarget.tagName && e.relatedTarget.tagName.toLowerCase() == 'img') {
        let segment = this.getSegment(e.layerX);
        if (segment.length === 2) {
          if (this.selectionEnd < e.layerX) {
            this.selectionEnd = segment[0] + 1;
          }
          else {
            this.selectionStart = segment[1] - 1;
            this.waveSelectionDisplay.style.left = this.selectionStart + 'px';
          }
          this.waveSelectionDisplay.style.width = this.selectionEnd - this.selectionStart + 'px';
        }
      }
    }
  },
  scaleWaveform: function(e) {
    e.preventDefault();
    this.scaleTally = Math.max(this.scaleTally + e.deltaY, 0);
    this.pointOffset = e.layerX;
    this.applyWaveformScale();
  },
  applyWaveformScale: function() {
 //   let scaleFactor = 50 - 49 * Math.exp(this.scaleFactor * this.scaleTally);
    let scaleFactor = 50 - 49 * Math.exp(this.scaleFactor * this.scaleTally);
    let scaleDiff = scaleFactor - this.oldScale;
    let offsetX = (this.pointOffset) * (scaleDiff - 1);
    offsetX = Math.max(0, Math.min(offsetX, this.waveWidth * (scaleFactor - 1)));
    this.waveformContainer.style.transform = `scaleX(${scaleFactor})`;// translateX(${offsetX}px)`;
    this.wavePos.style.width = Math.min(this.waveWidth / scaleFactor, this.waveWidth) + 'px';
    this.waveOffset = Math.max(0, (this.waveWidth - this.waveWidth / scaleFactor) / 2);
    this.wavePos.style.left = this.waveOffset + 'px';
    this.oldScale = scaleFactor;
    this.scaleOffset = offsetX;
    let offsetTime = this.waveOffset / this.waveWidth * (this.video.duration || this.mediaDuration);
    this.waveformWrapper.setAttribute('data-starttime', this.parseVideoTime(offsetTime));
    this.waveformWrapper.setAttribute('data-endtime', this.parseVideoTime(offsetTime + (this.video.duration || this.mediaDuration) / scaleFactor));
  },
  startMoveWavePos: function(e) {
    e.preventDefault();
    if (e.layerX > this.waveOffset && e.layerX < (this.waveOffset + this.waveWidth / this.oldScale)) {
      this.movingPos = true;
      this.posStart = e.layerX;
    }
  },
  moveWavePos: function(e) {
    if (this.movingPos) {
      let posWidth = this.wavePos.clientWidth;
      let newX = this.waveOffset + e.layerX - this.posStart;
      let maxX = this.waveWidth - posWidth - 4;
      newX = Math.max(0, Math.min(maxX, newX));
      let perc = newX / maxX;
      let transformX = (perc - 0.5) * maxX >> 0;
      this.wavePos.style.left = newX + 'px';
      this.waveformSVG.style.transform = `translateX(${-transformX}px)`;
      this.bgSVG.style.transform = `translateX(${-transformX}px)`;
    }
  },
  endMoveWavePos: function(e) {
    if (this.movingPos) {
      let newX = this.waveOffset + e.layerX - this.posStart;
      this.waveOffset = Math.max(0, Math.min(this.waveWidth - this.wavePos.clientWidth - 4, newX));
      this.movingPos = false;
    }
  },
  snipSelection: function(e) {
    let offset = parseInt(e.target.parentNode.style.left);
    let width = e.target.parentNode.clientWidth;
    this.cutSegment = [offset, offset + width];
  },
  setTitle: function(e) {
    this.title = e.target.value;
  },
  setLocation: function(e) {
    this.location = e.target.value;
  },
  setDate: function(e) {
    this.date = e.target.value;
  },
  createElement: function(type, text, opts) {
    var el = document.createElement(type);
    if (typeof text == 'string') {
      el.innerText = text;
    }
    else if (typeof text == 'object') {
      opts = text;
    }

    if (typeof opts != 'object') {
      return el;
    }

    if (opts.id) {
      el.id = opts.id || '';
    }
    el.className = opts.className || opts.class || opts.classes || '';
    el.type = opts.type || '';

    if (opts.data) {
      for (var key in opts.data) {
        el.setAttribute(`data-${key}`, opts.data[key]);
      }
    }
    if (opts.attribs) {
      for (var key in opts.attribs) {
        el.setAttribute(key, opts.attribs[key]);
      }
    }

    if (opts.listeners) {
      for (var key in opts.listeners) {
        el.addEventListener(key, opts.listeners[key], false);
      }
    }

    return el;
  },
  createNSelement: function(ns, type, attribs) {
    var ns = ns === null ? null : (ns || 'http://www.w3.org/2000/svg');
    var el = document.createElementNS(ns, type);
    for (var key in attribs) {
      if (key == 'text') {
        el.textContent = attribs[key];
      }
      else {
        el.setAttributeNS(null, key, attribs[key]);
      }
    }
    return el;
  },
  uploadEdits: function(e) {
    e.preventDefault();
    this.uploadNotification();
    this.notificationToggler.checked = true;
    const mpRes = async () => {

      /* **********************
      *  Get media package id *
      ************************/
      this.notificationModal.querySelector('p:nth-of-type(1)').classList.add('active');
      const mp = await utils.xhr(this.endpoints.getMediaPackage);
      if (mp.status > 399) {
        this.notificationModal.querySelector('p:nth-of-type(1)').className = 'fail';
        this.notificationModal.querySelector('p:nth-of-type(1) span:last-child').textContent = 'Workspace creation failed';
        return;
      }
      const mpId = mp.responseXML.querySelector('mediapackage').getAttribute('id')
      this.notificationModal.querySelector('p:nth-of-type(1)').className = 'complete';
      this.notificationModal.querySelector('p:nth-of-type(1) span:last-child').textContent = 'Workspace created';

      /* **************
      *  Upload Track *
      ****************/
      this.notificationModal.querySelector('p:nth-of-type(2)').classList.add('active');
      const Tfd = new FormData();
      Tfd.append('mediaPackage', mp.responseText);
      Tfd.append('flavor', 'presenter/source');
      Tfd.append('BODY', this.getVideo());
      const tResponse = await utils.xhr(this.endpoints.addTrack, {type: 'POST', data: Tfd, progress: this.notificationModal.querySelector('progress')});
      if (tResponse.status > 399) {
        this.notificationModal.querySelector('p:nth-of-type(2)').className = 'fail';
        this.notificationModal.querySelector('p:nth-of-type(2) .progressContainer').setAttribute('data-title', 'Upload failed');
        return;
      }
      const trackXML = tResponse.responseXML.querySelector('track');
      const trackId = trackXML.getAttribute('id');
      const trackURL = trackXML.querySelector('track url').textContent;
      this.notificationModal.querySelector('p:nth-of-type(2)').className = 'complete';
      this.notificationModal.querySelector('p:nth-of-type(2) .progressContainer').setAttribute('data-title', 'File uploaded');

      /* ***************
      *  Add Metadata  *
      *****************/
      this.notificationModal.querySelector('p:nth-of-type(3)').classList.add('active');
      trackXML.setAttribute('xmlns', 'http://mediapackage.opencastproject.org');
      const DCfd = new FormData();
      const core = this.getDCepisodeFile(true);
      DCfd.append('mediaPackage', tResponse.responseText);
      DCfd.append('dublinCore', core);
      const dcResponse = await utils.xhr(this.endpoints.addDCCatalog, {type: 'POST', data: DCfd});
      if (dcResponse.status > 399) {
        this.notificationModal.querySelector('p:nth-of-type(3)').className = 'fail';
        this.notificationModal.querySelector('p:nth-of-type(3) span:last-child').textContent = 'Could not include metadata';
        return;
      }
      this.notificationModal.querySelector('p:nth-of-type(3)').className = 'complete';
      this.notificationModal.querySelector('p:nth-of-type(3) span:last-child').textContent = 'Metadata saved';

      /* ***************************
      *  Upload SMIL/editing info  *
      *****************************/
      this.notificationModal.querySelector('p:nth-of-type(4)').classList.add('active');
      let smilFile = this.getSmilFile({mediaPackageId: mpId, trackUrl: trackURL});
      let smilForm = new FormData();
      smilForm.append('flavor', 'smil/cutting');
      smilForm.append('mediaPackage', dcResponse.responseText);
      smilForm.append('BODY', smilFile);
      let smilResponse = await utils.xhr(this.endpoints.addCatalog, {type: 'POST', data: smilForm});
      if (smilResponse.status > 399) {
        this.notificationModal.querySelector('p:nth-of-type(4)').className = 'fail';
        this.notificationModal.querySelector('p:nth-of-type(4) span:last-child').textContent = 'Editing information not saved';
        return;
      }
      this.notificationModal.querySelector('p:nth-of-type(4)').className = 'complete';
      this.notificationModal.querySelector('p:nth-of-type(4) span:last-child').textContent = 'Editing information saved';

      /* **********************
      *  Initiate processing  *
      ************************/
      this.notificationModal.querySelector('p:nth-of-type(5)').classList.add('active');
      const trimFD = new FormData();
      trimFD.append('mediaPackage', smilResponse.responseText);
      const trim = await utils.xhr(this.endpoints.trimIngest, {type: 'POST', data: trimFD});
      if (trim.status > 399) {
        this.notificationModal.querySelector('p:nth-of-type(5)').className = 'fail';
        this.notificationModal.querySelector('p:nth-of-type(5) span:last-child').textContent = 'Server unable to process media';
        return;
      }
      this.notificationModal.querySelector('p:nth-of-type(5)').className = 'complete';
      this.notificationModal.querySelector('p:nth-of-type(5) span:last-child').textContent = 'Currently processing on server';

      this.notificationModal.classList.remove('noclose');
    };

    mpRes();
  },
  getVideo: function() {
    if (this.fileUpload.querySelector('input').files[0]) {
      return this.fileUpload.querySelector('input').files[0];
    }
  },
  getSmilCutInfo: function(params) {
      if (typeof params != 'object' || !params.mediaPackageId || !params.trackUrl) {
        throw new Error('need info for smil cutting');
      }

      let smil = this.createNSelement('http://www.w3.org/ns/SMIL', 'smil', {version: '3.0', baseProfile: 'Language'});
      let smilBody = document.createElement('body');
      let smilHead = document.createElement('head');

      smilHead.setAttribute('xml:id', `h-${this.generateHexString()}`);

      let metaMP = this.createElement('meta', {attribs: {name: 'media-package-id', content: params.mediaPackageId}});
      metaMP.setAttribute('xml:id', `meta-${this.generateHexString()}`);
      let metaDuration = this.createElement('meta', {attribs: {name: 'track-duration', content: `${this.mediaDuration * 1000 >> 0}ms`}});
      metaDuration.setAttribute('xml:id', `meta-${this.generateHexString()}`);

      let groupIds = [`pg-${this.generateHexString()}`];
      let pGroup = this.createElement('paramGroup', {attribs: {'xml:id': groupIds[0]}});

      let p1 = this.createElement('param', {attribs: {valuetype: 'data', name: 'track-id', value: params.trackId || (params.trackUrl.split('/'))[6]}});
      let p2 = this.createElement('param', {attribs: {valuetype: 'data', name: 'track-src', value: params.trackUrl}});
      let p3 = this.createElement('param', {attribs: {valuetype: 'data', name: 'track-flavor', value: 'presenter/source'}});

      p1.setAttribute('xml:id', `param-${this.generateHexString()}`);
      p2.setAttribute('xml:id', `param-${this.generateHexString()}`);
      p3.setAttribute('xml:id', `param-${this.generateHexString()}`);

      pGroup.appendChild(p1);
      pGroup.appendChild(p2);
      pGroup.appendChild(p3);

      smilHead.appendChild(metaMP);
      smilHead.appendChild(metaDuration);
      smilHead.appendChild(pGroup);

      if (this.cutSegment.length > 0) {
        this.getActiveSegments
          .map(segment => { return {begin: segment.start / this.waveWidth * this.mediaDuration * 1000 >> 0, end: segment.end / this.waveWidth * this.mediaDuration * 1000 >> 0} })
          .forEach(segment => {
            let smilPar = this.createElement('par');
            let clip = this.createElement('video', {attribs: {clipBegin: `${segment.begin}ms`, clipEnd: `${segment.end}ms`, src: params.trackUrl, paramGroup: groupIds[0]}});
            clip.setAttribute('xml:id', `param-${this.generateHexString()}`);
            smilPar.setAttribute('xml:id', `par-${this.generateHexString()}`);
            smilPar.appendChild(clip)
            smilBody.appendChild(smilPar);
          });
      }
      else {
        let smilPar = this.createElement('par');
        let clip = this.createElement('video', {attribs: {clipBegin: '0ms', clipEnd: `${this.mediaDuration * 1000 >> 0}ms`, src: params.trackUrl, paramGroup: groupIds[0]}});
        clip.setAttribute('xml:id', `param-${this.generateHexString()}`);
        smilPar.setAttribute('xml:id', `par-${this.generateHexString()}`);
        smilPar.appendChild(clip);
        smilBody.appendChild(smilPar);
      }

      smil.appendChild(smilHead);
      smil.appendChild(smilBody);
      smilBody.setAttribute('xml:id', `b-${this.generateHexString()}`);
      smil.setAttribute('xmlns', 'http://www.w3.org/ns/SMIL');
      smil.setAttribute('xml:id', `s-${this.generateHexString()}`);
      smil.setAttribute('xmlns:oc', "http://smil.opencastproject.org");
      let text = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' + (new XMLSerializer()).serializeToString(smil);
      text = text.replace(/clipbegin/g, 'clipBegin').replace(/clipend/g, 'clipEnd').replace(/paramgroup/g, 'paramGroup').replace(/class=""/g, '');

      return text;
  },
  getSmil: function(trackURL) {
    if (asFile) {
      return getSmileFile(trackURL);
    }
    let smil = this.createNSelement('http://www.w3.org/ns/SMIL', 'smil', {version: '3.0'});
    let smilBody = this.createNSelement(null, 'body');
      let sumDuration = this.mediaDuration - this.segmentTimestamps.reduce((total, seg) => total += seg[1] - seg[0], 0);
      console.log(this.mediaDuration, sumDuration);
      let smilPar = this.createNSelement(null, 'par', {dur: (sumDuration * 1000 >> 0) + 'ms'});
      let smilSeq = this.createNSelement(null, 'seq');

      if (this.cutSegment.length > 0) {
        this.getActiveSegments
          .map(segment => { return {begin: segment.start / this.waveWidth * this.mediaDuration * 1000 >> 0, dur: segment.duration / this.waveWidth * this.mediaDuration * 1000 >> 0} })
          .forEach(segment => 
            smilSeq.appendChild(
              this.createNSelement(null, 'video', {begin: `${segment.begin}ms`, dur: `${segment.dur}ms`, src: trackUrl})
            )
          );
      }
      else {
        smilSeq.appendChild(
          this.createNSelement(null, 'video', {begin: `0ms`, dur: `${this.mediaDuration * 1000 >> 0}ms`, src: trackUrl})
        );
      }

      smilPar.appendChild(smilSeq);
      smilBody.appendChild(smilPar);
      smil.appendChild(
        this.createNSelement(null, 'head')
      );
      smil.appendChild(smilBody);
      smil.setAttribute('xmlns', 'http://www.w3.org/ns/SMIL');

      let text = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' + (new XMLSerializer()).serializeToString(smil);
      return text;
  },
  getSmilFile: function(params) {
      let file = new File([new Blob([this.getSmilCutInfo(params)], {type: 'text/xml'})], 'smil.xml');
      return file;
  },
  generateHexString: function() {
    let str = (Math.random() + 1).toString(16).substring(2,10) + '-' + (Math.random() + 1).toString(16).substring(2,6) + '-' + 
              (Math.random() + 1).toString(16).substring(2,6) + '-' + (Math.random() + 1).toString(16).substring(2,6) + '-' + 
              (Math.random() + 1).toString(16).substring(2,14);

    return str;
  },
  getDCepisodeFile: function(asText) {
    let dc = this.createNSelement('http://www.opencastproject.org/xsd/1.0/dublincore/', 'dublincore');

    let title = document.createElement('dcterms:title');
    title.textContent = this.title;
    //let title = this.createNSelement(null, 'dcterms:title', {text: this.title});
    this.dcTitle = title;
    title.removeAttributeNS(null, 'xmlns:dcterms');
    dc.appendChild(title);

    dc.setAttribute('xmlns:dcterms', 'http://purl.org/dc/terms/');
    dc.setAttribute('xmlns:xsi', 'http://www.w3.org/2001/XMLSchema-instance')

    let text = '<?xml version="1.0" encoding="UTF-8" standalone="no"?>' + (new XMLSerializer()).serializeToString(dc);

    if (asText) return text.replace(/>/g, ">\n");

    let file = new File([new Blob([text], {type: 'text/xml'})], 'catalog.xml');
    return file;
  },
  getTime: function() {
    return (new Date()).toISOString();
  },
  parseVideoTime: function(totalsecs) {
    let wholesecs = totalsecs >> 0;
    let hours = (wholesecs / 3600) >> 0;
    let minutes = ((wholesecs - hours * 3600) / 60) >> 0;
    let seconds = wholesecs - hours * 3600 - minutes * 60;
    let millis = (totalsecs % 1).toFixed(3);
    return `${hours < 10 ? '0' : ''}${hours}:${minutes < 10 ? '0' : ''}${minutes}:${seconds < 10 ? '0' : ''}${seconds}${(millis + '').substring(1)}`;
  },
  keepTime: function(timestamp) {
    this.timeDisplay.textContent = this.parseVideoTime(this.video.currentTime);
    if (this.isPlaying) {
      window.requestAnimationFrame(this.keepTime.bind(this));
    }
  },
  uploadNotification: function() {
    this.notificationModal.classList.toggle('noclose');
    this.notificationModal.querySelector('h2').textContent = 'Saving to server';
    let modalContent = this.notificationModal.querySelector('.modalContent');
    if (modalContent.querySelector('.btn-warning')) {
      modalContent.querySelector('.btn-warning').removeEventListener('click', this.uploadEdits.bind(this), false);
    }
    modalContent.innerHTML = '';

    ['Creating workspace', 'Uploading file', 'Saving metadata', 'Saving editing information', 'Begin processing on server'].forEach((step, j) => {
      let stepContainer = this.createElement('p');
      for (let i = 0; i < 3; i++) {
        stepContainer.appendChild(
          this.createElement('span', {class: 'progress'})
        );
      }
      if (j !== 1) {
        stepContainer.appendChild(
          this.createElement('span', step)
        );
      }
      else {
        let progressContainer = this.createElement('span', {class: 'progressContainer', data: {title: 'Uploading file'}});
        progressContainer.appendChild(
          this.createElement('progress')
        );
        stepContainer.appendChild(progressContainer);
        progressContainer.querySelector('progress').value = '0';
        progressContainer.querySelector('progress').min = '0';
        progressContainer.querySelector('progress').max = '100';
      }
      modalContent.appendChild(stepContainer);
    });
    let success = this.createElement('p', {class: 'success result'});
    let failed = this.createElement('p', {class: 'failed result'});

    let successLabel = this.createElement('label', 'OK', {attribs: {for: 'notifyModalToggle'}, class: 'btn btn-success'});
    let failLabel = this.createElement('label', 'Cancel', {attribs: {for: 'notifyModalToggle'}, class: 'btn btn-failure'});
    let retryLabel = this.createElement('button', 'Retry', {attribs: {for: 'notifyModalToggle'}, class: 'btn btn-warning'});

    retryLabel.addEventListener('click', this.uploadEdits.bind(this), false);

    success.appendChild(successLabel);
    failed.appendChild(retryLabel);
    failed.appendChild(failLabel);

    modalContent.appendChild(success);
    modalContent.appendChild(failed);
  }
}

var editor = new Editor();
var utils = new Utils();
editor.addEventListeners();

if (!localStorage.getItem('reopen')) {
  editor.reload = setTimeout(function() {
    let random = (Math.random() + 1).toString(36).substring(2,7);
    localStorage.setItem('reopen', random);
    window.close();
  }, 2000);
}

