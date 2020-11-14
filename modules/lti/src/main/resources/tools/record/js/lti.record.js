function getObj(id, arr, key) { key = key || 'id'; var o = null; $.each(arr, function (i, el) { if (el[key] == id) { o=el; return; } }); return o; };

var OCRecorder = (function($) {

  // define MY_FAV as a constant and give it the value 7
  const STATE_IDLE = 1;
  const STATE_RECORDING = 2;
  const STATE_PAUSED = 3;
  const STATE_CONFIRM = 4;
  const STATE_UPLOADING = 5;

  function Recorder(options) {
    this.settings = $.extend({
      id: 'recorder',
      tmpl : 'Recording ({{date}})',
      dateFormat: "YY-MM-DD hh:mm"
    }, options || {});
    
    this.streams = [];
    this.available = [];
    this.title = '';
    this.presenter = '';
    this.current = 1;

    this.videoData = [];
    this.videoRecorder = null;
    this.formats = [];
    this.browser = window.hasOwnProperty('InstallTrigger') ? 'firefox' : (window.hasOwnProperty('chrome') && window.chrome.hasOwnProperty('app') ? 'chrome' : 'other');

    this.cropVideos = {};
    this.posVideos = {};

    this.init();
  }

  Recorder.prototype = {
    constructor: OCRecorder,
    addGrammarList: function(grammar) {
      this.grammarLists.push(grammar);
      var speechRecogList = new (webkitSpeechGrammarList || SpeechGrammarList)();
      this.grammarLists.forEach(function(grammar) {
        speechRecogList.addFromString(grammar, 1);
      });
      this.recognition.grammars = speechRecogList;
    },
    init: function() {
      var self = this;

      self.formats = self.determineAvailableFormats();
      self.$recorder = $('#'+self.settings.id); 
      self.$title = self.$recorder.find('#'+self.settings.id +'-title');
      self.$presenter = self.$recorder.find('#'+self.settings.id +'-presenter');
      self.$desktop = this.$recorder.find('#stream-desktop');
      self.$webcam = this.$recorder.find('#stream-video');
      self.$positions = this.$recorder.find('#position-panel');
      self.$audio = this.$recorder.find('#stream-audio');
      self.$installer = this.$recorder.find('#extensionInstaller');
      self.$canvas = self.$recorder.find('#canvas');
      
      self.state = STATE_IDLE;
      self.$lblUploading  = this.$recorder.find('#lbl-uploading');
      self.$btnStart = this.$recorder.find('a[rel=btn-start]');
      self.$btnPause = this.$recorder.find('a[rel=btn-pause]');
      self.$btnStop  = this.$recorder.find('a[rel=btn-stop]');

      self.$modal  = this.$recorder.find('#uploadModal');
      self.$btnSave  = this.$recorder.find('#uploadModal-save');
      self.$btnSubmit  = this.$recorder.find('#uploadModal-submit');
      
      // JS takes control of visibility
      self.$btnPause.hide().removeClass('hidden');
      self.$lblUploading.hide().removeClass('hidden');

      self.title = self.settings.tmpl.replace(/{{date}}/i, moment().format(self.settings.dateFormat));
      self.$title.val(self.title);

      //Tools
      self.$toggleCropTool = $('#cropVideoToggle');
      self.$cropTool = $('#cropWorkspace');
      self.$applyCrop = $('#cropTool .apply');

      self.$togglePosTool = $('#positionVideoToggle');
      self.$posTool = $('#posTool');
      self.$applyPos = $('#posTool .apply');

      self.$recorder.find('div[id*=stream-] > a').on('click', function(event) {
        event.preventDefault();
        var a = $(this);

        if (a.hasClass('empty')) {

          switch(a.data('stream')) {
            case 'desktop':
              self.selectDesktopStream(a);
              break;
            case 'video':
              self.selectWebcamStream(a);
              break;
            case 'audio':
              var audioStreams = $.grep(self.available, function(e){  return e.source == 'audio'; });
              if (audioStreams.length > 1) {
                self.toggleDropDown(a.parent());
              } else if (audioStreams.length == 1) {
                var item = audioStreams[0];
                self.streams.push({id: item['id'], flavor: 'microphone', source: 'audio'});
                a.parent().find('a[data-id='+ item['id'] +'] > i').removeClass('fa-square-o').addClass('fa-check-square-o');
              }
              break;
            default: break;
          }
          
          self.validStream();
        } else {
          self.toggleDropDown(a.parent());
        }
      });

      self.$recorder.find("#stream-audio > section").on('click', 'a:not(.btn)', function(event){
        event.preventDefault();
        var a = $(this);

        self.streams = $.grep(self.streams, function(e){ return e.source != 'audio'; });
        self.streams.push({id: a.data('id'), flavor: 'microphone', source: 'audio'});

        self.$recorder.find('#stream-audio > section > a:not(.btn) > i').addClass('fa-square-o').removeClass('fa-check-square-o');
        
        a.find('i').removeClass('fa-square-o').addClass('fa-check-square-o');
        self.$recorder.find('#stream-audio > a').removeClass('empty');

        self.validStream();
      });

      self.$recorder.find('a.btn[rel^=remove-]').on('click', function(event){
        event.preventDefault();
        var a = $(this);

        switch(a.attr('rel')){
          case 'remove-desktop': 
            self.streams = $.grep(self.streams, function(e){ return e.flavor != 'desktop'; });
            self.$desktop.find(' > a').addClass('empty');
            self.closeDropDown(self);
            break;

          case 'remove-webcam': 
            self.streams = $.grep(self.streams, function(e){ return e.flavor != 'webcam'; });
            self.$webcam.find(' > a').addClass('empty');
            self.closeDropDown(self);
            Object.keys(mediaMgr.streams).some(function(mediaId) {
              if (mediaMgr.streams[mediaId].type === 'webcam') {
                var streamId = mediaMgr.streams[mediaId].stream.id;
                compositor.removeStream(streamId);               
                return;
              }
            });
            break;

          case 'remove-audio': 
            self.streams = $.grep(self.streams, function(e){ return e.source != 'audio'; });
            var el = self.$recorder.find('#stream-audio');
            el.find('section > a:not(.btn) > i').addClass('fa-square-o').removeClass('fa-check-square-o');
            el.find(' > a').addClass('empty');
            self.closeDropDown(self);
            break;

          default: break;
        }
        self.validStream();
      });

      self.$positions.on('click', '.position', function(event){
        event.preventDefault();
        var a = $(this);

        var webcam = getObj('webcam', self.streams, 'flavor');
        webcam.position = a.attr('rel');
        self.$positions.find('a.positio.deactive').removeClass('active');
        self.$positions.find('a.position[rel='+ a.attr('rel') +']').addClass('active');

        self.validStream();

        var streamId = null; 
        for (var key in mediaMgr.streams) {
          if (mediaMgr.streams[key].type === 'webcam') {
            streamId = mediaMgr.streams[key].stream.id;
          }
        }

        if (!streamId) {
          return;
        }

        var offset = {
          offsetX: webcam.position.indexOf('right') > -1 ? compositor.width : 0,
          offsetY: webcam.position.indexOf('bottom') > -1 ? compositor.height : 0
        };

        compositor.setStreamDimensions(streamId, offset);

      });

      self.$btnStart.on('click', function(event){
        event.preventDefault();
        self.startRecording();
      });

      self.$btnPause.on('click', function(event){
        event.preventDefault();
        self.pauseRecording();
      });
      
      self.$btnStop.on('click', function(event){
        event.preventDefault();
        self.stopRecording();
      });

      self.$btnSubmit.on('click', function(event){
        self.uploadRecording(self);
      });

      self.$btnSave.on('click', function(event) {
        self.$modal.find('a').each(function() {
          console.log('about to click');
          this.click();
        });
      });

      self.$recorder.find('#uploadModal button[data-dismiss=modal]').on('click', function(event){
        self.resetRecording();
      });

      // title change catch
      self.$title.on('change', function(event){
        self.title = self.$title.val();
        self.current = 1;
        self.$recorder.find('#uploadModal-title > small').html(self.title);
        self.$modal.find('a span').each(function() {
          $(this).html(self.title);
          var $parent = $(this).parent();
          $parent.attr('download', ($parent.data('type') + ' - ' + self.title + '.webm'));
        });
      });

      self.$modal.on('keyup', '#uploadModal-input-title', function(event) {
        self.$title.val($(this).val());
        self.$title.trigger('change');
      });

      // presenter change catch
      self.$presenter.on('change', function(event){
        self.presenter = self.$presenter.val();
      });

      self.$installer.on('submit', function(event) {
        event.preventDefault();
        if (self.browser == 'chrome' && chrome.webstore) {
          chrome.webstore.install(
            'https://chrome.google.com/webstore/detail/cgifnbjkakdhgbflhdomcooakkdeinhh',
            function() {
              location.reload();
            },
            function(e) {
              console.log(e);
            }
          );
        }

      
      });

      setTimeout(function() {
        //give chrome time to check if extension is installed
        if (this.browser == 'chrome' && $('#appInstalled').length > 0) {
          window.addEventListener('message', function(e) {
            if (e.data.type == 'SS_DIALOG_SUCCESS') {
              self.$desktop.find('a').attr('data-id', e.data.streamId);
              self.selectDesktopStream(self.$desktop.find('a'));
            }
          });
        }
      }.bind(this), 1000);

      self.$togglePosTool.on('change', function(e) {
        if ($(e.target).is(':checked')) {
          this.togglePositionalVideos();
        }
      }.bind(self));

      self.$toggleCropTool.on('change', function(e) {
        if (e.target.checked) {
          self.toggleVideoCrop();
        }
        else {
          self.removeVideoCrop();
        }
      }.bind(self));

      self.$cropTool.on({
        mousedown: self.startCrop.bind(self),
          mouseup: self.endCrop.bind(self),
         mouseout:  self.endCrop.bind(self)
      }, 'video');

      self.$cropTool.on({
        mousedown: self.startCropMove.bind(self),
          mouseup: self.endCropMove.bind(self),
         mouseout: self.endCropMove.bind(self)
      }, '.cropOutline');

      self.$cropTool.on({
        mousedown: self.startCropWidth.bind(self),
          mouseup: self.endCropWidth.bind(self),
         mouseout: self.endCropWidth.bind(self)
      }, '.cropOutline .expander:nth-of-type(1)');

      self.$cropTool.on({
        mousedown: self.startCropHeight.bind(self),
          mouseup: self.endCropHeight.bind(self),
         mouseout: self.endCropHeight.bind(self)
      }, '.cropOutline .expander:nth-of-type(2)');

      self.$applyCrop.on('click', self.setVideoCrop.bind(self));

      self.$posTool.on({
        mousedown: self.startMovePosVideo.bind(self),
          mouseup: self.endMovePosVideo.bind(self),
         mouseout: self.endMovePosVideo.bind(self)
      }, '.videoContainer');

      self.$posTool.on({
        mousedown: self.startResizeVideo.bind(self),
          mouseup: self.endResizeVideo.bind(self),
         mouseout: self.endResizeVideo.bind(self)
      }, '.videoContainer .resizer');

      self.$applyPos.on('click', self.setVideoPositions.bind(self));

      self.languageChoice = document.getElementById('transcription-list');
      self.languageChoice.addEventListener('click', self.setLanguage.bind(self));
    },
    setLanguage: function(e) {
      e.preventDefault();

      if (!e.target.href) {
        return;
      }

      let lang = e.target.href.substring(e.target.href.indexOf('#') + 1);
      $('label[for=chooseLang]').text('Transcript: ' + lang);
      $('#chooseLang')[0].checked = false;
      speechToText.setLanguage(lang);
    },
    updateTitle: function(newTitle, restart) {
      restart = typeof restart !== 'undefined' ? restart : false;
      if (restart) {
        this.current = 1;
      } else {
        if (this.title == newTitle) {
          this.current ++;
        } else {
          this.current = 1;
        }
      }
      this.title = newTitle;
      this.$title.val(this.title + (this.current > 1 ? ' ['+ this.current +']':''));
    },
    validStream: function() {
      var validVideo = false,
          validAudio = false,
          self = this;
        
      self.$audio.find('section > a:not(.btn) > i').addClass('fa-square-o').removeClass('fa-check-square-o');
      self.$webcam.find('section').removeClass('hasDesktop');

      $.each(self.streams, function(i, el){
        if (el.source == 'video') {
          validVideo = true;
          if (el.flavor == 'desktop') {
            self.$webcam.find('section').addClass('hasDesktop');
          }
        }
        if (el.source == 'audio') {
          validAudio = true;
          self.$audio.find('a[data-id='+ el['id'] +'] > i').removeClass('fa-square-o').addClass('fa-check-square-o');
        }
      });

      if (validVideo) {
        self.$desktop.find(' > a').removeClass('invalid');
        self.$webcam.find(' > a').removeClass('invalid');
      } else {
        self.$desktop.find(' > a').addClass('invalid');
        self.$webcam.find('> a').addClass('invalid');
      }

      if (validAudio) {
        self.$audio.find(' > a').removeClass('empty invalid');
      } else {
        self.$audio.find(' > a').addClass('empty invalid');
      }
    },
    startRecording: function() {
        var self = this;
        compositor.startRecording();
        this.state = STATE_RECORDING;
        this.$btnStart.hide();
        this.$btnPause.show().removeClass('hidden');
        this.$btnStop.removeClass('disabled');
    },
    pauseRecording: function() {
      if (this.state !== STATE_PAUSED) {
        this.state = STATE_PAUSED;
        this.$btnStart.show();
        console.log('pausing');
        this.$btnPause.hide();
        recorder.pause();
      }
      else {
        this.state = STATE_RECORDING;
        recorder.resume();
        this.$btnStart.hide();
        this.$btnPause.show();
      }
    },    
    stopRecording: function() {
      this.state = STATE_CONFIRM;
      this.$btnStart.addClass('disabled');
      this.$btnPause.addClass('disabled');
      this.$btnStop.addClass('disabled');
      
      this.$recorder.find('#uploadModal-title > small').html(this.title);
      this.$recorder.find('#uploadModal-body').html(tmpl('tmpl-upload-modal-body', this));
      compositor.stopRecording()
        .then(function() {
          this.generateVideos();
        }.bind(this));
    },
    generateVideos: function() {
      var self = this;
      var vidBlob = new Blob(compositor.recordedData, {type: 'video/webm'});
      this.vidURL = URL.createObjectURL(vidBlob);
      this.$dlVid = $('<a/>', {
                          href: self.vidURL,
                        target: '_blank',
                      download: 'Screencast - ' + self.$title.val() + '.webm',
                    }).html('Screencast - <span>' + self.$title.val() + '</span>')
                    .data('type', 'Screencast');
      $('#recorderFiles ul').append(
        $('<li/>').append(this.$dlVid)
      );
    },
    uploadRecording: function() {
      var self = this;
      self.state = STATE_UPLOADING;
      console.log(self.state);
      self.$btnStart.hide();
      self.$btnPause.hide();
      self.$btnStop.hide();
      self.$lblUploading.show();

      self.$modal.modal('hide');

      window.setTimeout(function(){ 
          console.log('done.'); 
          self.updateTitle(self.title);
          self.resetRecording(); 
      }, 2500);
    },
    resetRecording: function() {
      this.state = STATE_IDLE;
      this.$lblUploading.hide();
      this.$btnStart.removeClass('disabled').show();
      this.$btnPause.removeClass('disabled').hide();
      this.$btnStop.addClass('disabled').show();
    },
    toggleDropDown: function(el) {
      if (!el.hasClass('open')) {
        this.openDropDown(el);
      }
      else {
        this.closeDropDown(this);
      }
    },
    openDropDown: function(el) {
      var self = this;
      self.$desktop.removeClass('open');
      self.$webcam.removeClass('open');
      self.$audio.removeClass('open');

      el.addClass('open');
    },
    closeDropDown: function(self) {

      if (self) {
        self.$desktop.removeClass('open');
        self.$webcam.removeClass('open');
        self.$audio.removeClass('open');
      }
    },
    selectDesktopStream: function(a) {
      this.closeDropDown(this);

      this.streams.push({id: '1', flavor: 'desktop', source: 'video'});     
      this.$desktop.find(' > a').removeClass('empty');

      var self = this;

      var requestDesktop = new Promise(function(resolve, reject) {
        if (this.browser == 'firefox' || (this.browser == 'chrome' && a.data('id'))) {
          resolve();
          return;
        }

        if (self.browser == 'chrome') {
          if ($('#appInstalled').length > 0) {
            window.postMessage({ type: 'SS_UI_REQUEST', text: 'start', url: location.origin }, '*');
            reject("awaiting chrome");
          }
          else {
            $('#extensionModal').modal('show');
            reject("not installed");
          }
          return;
        }
        else {
          reject("unsupported browser");
          return;
        }
      }.bind(this));
      requestDesktop.then(function() {
        mediaMgr.activateStream(a.data('id'), true)
          .then(function(stream) {
            try {
              compositor.addStream(stream);
              compositor.onstreamcreated = function(compositeStream) {
                this.attachRecorder();
              }.bind(compositor);
              compositor.onaudiotrack = function(audioTrack) {
                audioAnalyser.analyse(compositor.stream);
              }
          } catch(streamErr) {
            throw streamErr;
          }
        }.bind(this))
        .catch(function(err) {
          console.log(err);
        });
        this.validStream();
      }.bind(this))
      .catch(function(err) {
        if (err != 'awaiting chrome' && err != 'not installed') {
          console.log(err);
        }
      });
    },
    selectWebcamStream: function(a) {
      this.closeDropDown(this);

      this.streams = $.grep(this.streams, function(e){ return e.flavor != 'webcam'; });
      this.streams = $.grep(this.streams, function(e){ return e.source != 'audio'; });

      var self = this;
      mediaMgr.activateStream(a.data('id'))
        .then(function(stream) {
          try {
            compositor.addStream(stream);
            compositor.onstreamcreated = function(compositeStream) {
              this.attachRecorder();
            }.bind(compositor);
            compositor.onaudiotrack = function(audioTrack) {
              audioAnalyser.analyse(compositor.stream);
              speechToText.audioTrack = compositor.stream;
            }
          } catch(streamErr) {
            throw streamErr;
          }
        }.bind(this))
        .catch(function(err) {
          console.log(err);
        });

      this.streams.push({id: '2', flavor: 'webcam', source: 'video', position: 'right-bottom'});
      this.streams.push({id: '3', flavor: 'microphone', source: 'audio'});
      
      var webcam = getObj('webcam', this.streams, 'flavor');

      this.$positions.find('a.position').removeClass('active');
      this.$positions.find('a.position[rel='+ webcam.position +']').addClass('active');

      this.$webcam.find(' > a').removeClass('empty');
      this.$audio.find(' > a').removeClass('empty');

      this.validStream();
      return true;
    },
    toggleVideoCrop: function() {
      $list = $('#cropList');

      if ($list.html()) {
        $list.empty();
      }

      var self = this;

      compositor.streams.forEach(function(stream) {
        var $vidContainer = $('<div/>', {
                              class: 'videoContainer'
                            });
        var baseVideo, croppedVideo;
        baseVideo = stream.video.cloneNode();
        croppedVideo = stream.video.cloneNode();

        var $cropOutline = $('<div/>', {
                             class: 'cropOutline'
                           })
                           .append(
                             $('<div/>', {
                               class: 'expander'
                             })
                           )
                           .append(
                             $('<div/>', {
                               class: 'expander'
                             })
                           );

        baseVideo.autoplay = croppedVideo.autoplay = true;
        baseVideo.srcObject = croppedVideo.srcObject = stream.track;

        $vidContainer.append(baseVideo);
        $vidContainer.append(croppedVideo);
        $vidContainer.append($cropOutline);
        $vidContainer.data('id', stream.track.id);
        $list.append($vidContainer);

        $vidContainer.on({
          click: self.activateCrop.bind(self)
        })
      });
    },
    clearCropWorkspace: function() {
      $('#cropWorkspace').find('video,.cropOutline').each(function() {
        $(this).off();
        $(this).remove();
      });
    },
    removeVideoCrop: function() {
    },
    activateCrop: function(e) {
      var self = e.target;
      while (self && !self.classList.contains('videoContainer')) {
        self = self.parentNode;
      }

      var dims = self.getBoundingClientRect();
      var delay = $('#cropWorkspace').hasClass('active') ? 500 : 0;
      var trackId = $(self).data('id');

      $(self).siblings('.active').removeClass('active');

      $('#cropWorkspace').removeClass('active');
      this.clearCropWorkspace();

      if (!this.cropVideos[trackId]) {
        this.cropVideos[trackId] = {};
      }

      setTimeout(function() {
        $('#cropWorkspace').addClass('setPos');
        $('#cropWorkspace').append($(self).children().clone(true, true));
        var stream = compositor.streams
                          .filter(function(stream) { return stream.track.id === trackId })[0];

        $('#cropWorkspace').find('video').each(function() {
          this.srcObject = stream.track;
        });
        $('#cropWorkspace').css({
          transform: '',
                top: dims.top + 'px',
               left: dims.left + 'px',
              width: stream.video.videoWidth + 'px',
             height: stream.video.videoHeight + 'px'
        });

        //Adjust for fluid element size
        var heightRatio = $('#cropWorkspace').height() / stream.video.videoHeight;
        var widthRatio = $('#cropWorkspace').width() / stream.video.videoWidth;
        var minRatio = Math.min(heightRatio, widthRatio);
        if (minRatio !== 1) {
          $('#cropWorkspace').css({
             width: stream.video.videoWidth * minRatio,
            height: stream.video.videoHeight * minRatio
          });
        }

        $('#cropWorkspace').removeClass('setPos');
        $('#cropWorkspace').addClass('active');
        $('#cropWorkspace').data('id', $(self).data('id'));
        if (Object.keys(this.cropVideos[trackId]).length > 4) { //check for offsetX, offsetY, width, height
          var cropDims = this.cropVideos[trackId];
          $('#cropWorkspace .cropOutline')
            .addClass('isCropped')
            .css({
                 top: cropDims.offsetY + 'px',
                left: cropDims.offsetX + 'px',
               width: cropDims.width + 'px',
              height: cropDims.height + 'px'
            });
          $('#cropWorkspace video:nth-of-type(2)')
            .css({
              clipPath: `polygon(${cropDims.offsetX}px ${cropDims.offsetY}px, ${cropDims.offsetX}px ${cropDims.offsetY + cropDims.height}px, ${cropDims.offsetX + cropDims.width}px ${cropDims.offsetY + cropDims.height}px,${cropDims.offsetX + cropDims.width}px ${cropDims.offsetY}px)`
            });
        }
      }.bind(this), delay);
    },
    startCrop: function(e) {
      e.preventDefault();
      $(e.target).addClass('isCropping');
      $(e.target).on('mousemove', this.adjustCrop.bind(this));
      $(e.target).data('start', [e.offsetX, e.offsetY]);
      $(e.target).parent().addClass('isCropping');
      $(e.target).siblings('.cropOutline').addClass('isCropped');

      var trackId = $(e.target).parent().data('id');
      if (this.cropVideos.hasOwnProperty(trackId)) {
        this.cropVideos[trackId] = {
            renderedWidth: e.target.parentNode.clientWidth,
                  offsetX: e.offsetX,
                  offsetY: e.offsetY,
                maxHeight: $('#cropWorkspace')[0].clientHeight - e.offsetY,
                 maxWidth: $('#cropWorkspace')[0].clientWidth - e.offsetX
        }
      }
    },
    adjustCrop: function(e) {
      e.preventDefault();
      var start = $(e.target).data('start');
      var current = [e.offsetX, e.offsetY];
      if (Math.abs(current[0] - start[0]) > 10 && Math.abs(current[1] - start[1]) > 10) {
        var minX = Math.min(current[0], start[0]),
            maxX = Math.max(current[0], start[0]),
            minY = Math.min(current[1], start[1]),
            maxY = Math.max(current[1], start[1]);
        $(e.target).siblings('video')
          .css({
            clipPath: `polygon(${minX}px ${minY}px, ${minX}px ${maxY}px, ${maxX}px ${maxY}px, ${maxX}px ${minY}px)`
          });
        $(e.target).siblings('.cropOutline')
          .css({
               top: minY + 'px',
              left: minX + 'px',
             width: Math.floor(maxX - minX) + 'px',
            height: Math.floor(maxY - minY) + 'px'
          });
      }
    },
    endCrop: function(e) {
      e.preventDefault();
      if (!$(e.target).hasClass('isCropping')) {
        return;
      }

      $(e.target).removeClass('isCropping');
      $(e.target).off('mousemove');
      $(e.target).parent().removeClass('isCropping');
      var streamId = $(e.target).parent().data('id');
      if (this.cropVideos.hasOwnProperty(streamId)) {
        var start = $(e.target).data('start');
        var width = Math.abs(start[0] - e.offsetX);
        var height = Math.abs(start[1] - e.offsetY);
        this.cropVideos[streamId].width = width;
        this.cropVideos[streamId].height = height;
      }
    },
    startCropMove: function(e) {
      $(e.target).addClass('isMovingCrop');
      $(e.target).data('start', [e.offsetX, e.offsetY]);
      $(e.target).data('orig', [parseInt(e.target.style.left), parseInt(e.target.style.top)]);
      $(e.target).data('clientxy', [e.clientX, e.clientY]);
      var parentPos = [parseInt(e.target.parentNode.left), parseInt(e.target.parentNode.top)];
      $(e.target).data('parentPos', parentPos);
      $(e.target).on('mousemove', this.moveCrop.bind(this));
    },
    moveCrop: function(e) {
      var cxy = $(e.target).data('clientxy');
      var parentPos = $(e.target).data('parentPos');
      var orig = $(e.target).data('orig');
      var diffX = e.clientX - cxy[0],
          diffY = e.clientY - cxy[1];
      var width = e.target.clientWidth;
      var height = e.target.clientHeight;
      var maxX = e.target.parentNode.clientWidth - width,
          maxY = e.target.parentNode.clientHeight - height;

      var left = Math.max(0, Math.min(orig[0] + diffX, maxX)),
          top = Math.max(0, Math.min(orig[1] + diffY, maxY));

      $(e.target)
        .css({
          left: left + 'px',
           top: top + 'px',
        });
      $(e.target).prev()
        .css({
          clipPath: `polygon(${left}px ${top}px, ${left}px ${top + height}px, ${left + width}px ${top + height}px, ${left + width}px ${top}px)`
        });
    },
    endCropMove: function(e) {
      if (!$(e.target).hasClass('isMovingCrop')) {
        return;
      }
      
      $(e.target).removeClass('isMovingCrop');
      $(e.target).off('mousemove');

      var trackId = $(e.target).parent().data('id');
      if (!this.cropVideos[trackId]) {
        console.log('no such track');
        return;
      }

      this.cropVideos[trackId].offsetX = parseInt(e.target.style.left);
      this.cropVideos[trackId].offsetY = parseInt(e.target.style.top);
      this.cropVideos[trackId].maxWidth = $('#cropWorkspace')[0].clientWidth - this.cropVideos[trackId].offsetX;
      this.cropVideos[trackId].maxHeight = $('#cropWorkspace')[0].clientHeight - this.cropVideos[trackId].offsetY;
    },
    setVideoCrop: function(e) {
      var allowedKeys = ['width', 'height', 'offsetX', 'offsetY'];
      var changes = {};
      try {
        for (var key in this.cropVideos) {
          var cropChanges = this.cropVideos[key];
          var streamWidth = compositor.getStreamById(key).video.videoWidth;
          var scale = streamWidth / cropChanges.renderedWidth;
          changes[key] = [
                           [cropChanges.offsetX * scale, cropChanges.offsetY * scale],
                           [(cropChanges.offsetX + cropChanges.width) * scale, (cropChanges.offsetY + cropChanges.height) * scale]
                         ];

        }
      } catch(e) {
        console.log(e);
      }
      compositor.cropStreams(changes);
    },
    startCropWidth: function(e) {
      $(e.target).addClass('resizing');
      $(e.target).on('mousemove', this.cropWidthResize.bind(this));
      $(e.target).data('start', [e.clientX]);
      e.stopImmediatePropagation();
      e.preventDefault();
    },
    cropWidthResize: function(e) {
      var id = $('#cropWorkspace').data('id');
      this.cropVideos[id].width += e.originalEvent.movementX;
      var dims = this.cropVideos[id];
      this.cropVideos[id].width = Math.max(40, Math.min(dims.width, dims.maxWidth));
      $('#cropWorkspace .cropOutline')
        .css({
          width: dims.width + 'px'
        });
      $('#cropWorkspace video:nth-of-type(2)')
        .css({
          clipPath: `polygon(${dims.offsetX}px ${dims.offsetY}px, ${dims.offsetX}px ${dims.offsetY + dims.height}px, ${dims.offsetX + dims.width}px ${dims.offsetY + dims.height}px, ${dims.offsetX + dims.width}px ${dims.offsetY}px)`
        });
    },
    endCropWidth: function(e) {
      $(e.target).removeClass('resizing');
      $(e.target).off('mousemove');
    },
    startCropHeight: function(e) {
      $(e.target).addClass('resizing');
      $(e.target).on('mousemove', this.cropHeightResize.bind(this));
      $(e.target).data('start', [e.clientY]);
      e.stopImmediatePropagation();
      e.preventDefault();
    },
    cropHeightResize: function(e) {
      var id = $('#cropWorkspace').data('id');
      this.cropVideos[id].height += e.originalEvent.movementY;
      var dims = this.cropVideos[id];
      this.cropVideos[id].height = Math.max(40, Math.min(dims.height, dims.maxHeight));
      $('#cropWorkspace .cropOutline')
        .css({
          height: dims.height + 'px'
        });
      $('#cropWorkspace video:nth-of-type(2)')
        .css({
          clipPath: `polygon(${dims.offsetX}px ${dims.offsetY}px, ${dims.offsetX}px ${dims.offsetY + dims.height}px, ${dims.offsetX + dims.width}px ${dims.offsetY + dims.height}px, ${dims.offsetX + dims.width}px ${dims.offsetY}px)`
        });
    },
    endCropHeight: function(e) {
      $(e.target).removeClass('resizing');
      $(e.target).off('mousemove');
    },
    togglePositionalVideos: function() {
      this.canvasRatio = compositor.canvasContext.canvas.width / this.$canvas[0].width;
      this.posVideos = {};
      this.$posTool.find('.posVideos').empty();
      compositor.streams.forEach(function(stream) {
        var $vidContainer = $('<div/>', {
                             class: 'videoContainer'
                           })
                           .data('id', stream.track.id)
                           .append(
                             $('<div/>', {
                               class: 'resizer'
                             })
                           );
        var $video = $('<video/>');
        $video[0].srcObject = stream.track;
        $vidContainer.append($video);
        $vidContainer.css({
             top: stream.offsetY / this.canvasRatio,
            left: stream.offsetX / this.canvasRatio,
           width: stream.width / this.canvasRatio, 
          height: stream.height / this.canvasRatio
        });
        this.posVideos[stream.track.id] = {
             top: stream.offsetY / this.canvasRatio,
            left: stream.offsetX / this.canvasRatio,
           width: stream.width / this.canvasRatio, 
          height: stream.height / this.canvasRatio
        };
        this.$posTool.find('.posVideos').append($vidContainer);
        $vidContainer.data('aspectratio', stream.cropWidth / stream.cropHeight);
      }.bind(this));
    },
    startMovePosVideo: function(e) {
      $(e.target).addClass('isMoving');
      $(e.target).data('offset', [e.clientX, e.clientY]);
      $(e.target).data('orig', [parseInt(e.target.style.left), parseInt(e.target.style.top)]);
      $(e.target).on('mousemove', this.movePosVideo.bind(this));
    },
    movePosVideo: function(e) {
      var initPos = $(e.target).data('offset');
      var origXY = $(e.target).data('orig');
      var diff = [e.clientX - initPos[0], e.clientY - initPos[1]];
      var elTop = origXY[1] + diff[1];
      var elLeft = origXY[0] + diff[0];

      elTop = Math.max(0, Math.min(elTop, this.$canvas[0].height - e.target.clientHeight));
      elLeft = Math.max(0, Math.min(elLeft, this.$canvas[0].width - e.target.clientWidth));

      $(e.target).css({
         top: elTop + 'px',
        left: elLeft + 'px'
      });
    },
    endMovePosVideo: function(e) {
      var el = e.target.tagName.toLowerCase() === 'video' ? e.target.parentNode : e.target;
      if ($(el).hasClass('isMoving')) {
        $(el).removeClass('isMoving');
        $(el).off('mousemove');
        var id = $(el).data('id');
        this.posVideos[id] = {
           height: parseInt(el.style.height),
            width: parseInt(el.style.width),
          offsetX: parseInt(el.style.left),
          offsetY: parseInt(el.style.top)
        }
      }
    },
    startResizeVideo: function(e) {
      e.preventDefault();
      e.stopImmediatePropagation();
      var parent = e.target.parentNode;
      $(parent).addClass('isResizing');
      $(e.target).data('offset', [e.pageX, e.pageY]);
      $(e.target).data('orig', [parseInt(parent.style.width), parseInt(parent.style.height)]);
      $(e.target).on('mousemove', this.resizeVideo.bind(this));
    },
    resizeVideo: function(e) {
      e.preventDefault();
      e.stopImmediatePropagation();
      var parent = e.target.parentNode;
      var origSize = $(e.target).data('orig');
      var offset = $(e.target).data('offset');
      var curPos = [parseInt(parent.style.left), parseInt(parent.style.top)];
      var newWidth = origSize[0] + (e.pageX - offset[0]);
      var newHeight = origSize[1] + (e.pageY - offset[1]);

      newHeight = Math.max(50, Math.min(newHeight, this.$canvas[0].height - curPos[1]));
      if (newWidth + curPos[0] >= this.$canvas[0].width) {
        newWidth = Math.max(50, Math.min(newWidth, this.$canvas[0].width - curPos[0]));
        newHeight = newWidth / $(parent).data('aspectratio');
      }
      else {
        newWidth = newHeight * $(parent).data('aspectratio');
      }
      $(parent).css({
         width: newWidth + 'px',
        height: newHeight + 'px'
      });
    },
    endResizeVideo: function(e) {
      e.preventDefault();
      e.stopImmediatePropagation();
      $(e.target).parent().removeClass('isResizing');
      $(e.target).off('mousemove');
      var parent = $(e.target).parent()[0];
      var id = $(parent).data('id');
      this.posVideos[id] = {
         height: parseInt(parent.style.height),
          width: parseInt(parent.style.width),
        offsetX: parseInt(parent.style.left),
        offsetY: parseInt(parent.style.top)
      };
    },
    setVideoPositions: function(e) {
      for (var id in this.posVideos) {
        for (var dimKey in this.posVideos[id]) {
          this.posVideos[id][dimKey] *=  this.canvasRatio;
        }
        compositor.setStreamDimensions(id, this.posVideos[id]);
      }
    },
    populateDevices: function(devices) {
      this.available = devices;
      devices
        .forEach(function(device, i) {
          if (device.kind.indexOf('audio') > -1) {
            this.$recorder.find("#stream-audio > section").prepend(tmpl('tmpl-audio-device', device));
          }
          else if (device.kind.indexOf('video') > -1) {
            this.$webcam.find(' > a').data('id', device.deviceId);
          }
        }.bind(this));

    },
    trackVideo: function() {
      var canvas = document.getElementById('canvas');
      var WIDTH = canvas.clientWidth;
      var HEIGHT = canvas.clientHeight;
      canvas.width = WIDTH;
      canvas.height = HEIGHT;
      compositor.attachTo(canvas);
    },
    determineAvailableFormats: function() {
      //Lump mediarecorder formats into array below for filtering
      return [
               'video/webm;codecs=vp9,opus',
               'video/webm;codecs=vp9.0,opus',
               'video/webm;codecs=avc1',
               'video/x-matroska;codecs=avc1',
               'video/webm;codecs=vp8,opus'
             ].filter(codec => MediaRecorder.isTypeSupported(codec));
    }
  }

  return Recorder;

})(jQuery);

const rafLoop = new AnimationLoop();
const mediaMgr = new MediaManager();
const compositor = new Compositor();
const audioAnalyser = new AudioAnalyser();
const speechToText = new SpeechToText(audioAnalyser);
const runRecorder = new OCRecorder({id: 'recorder'});

[compositor, audioAnalyser].forEach(component => {

  component.ondelegation('subscribe.raf', function() {
    let delFns = Array.prototype.slice.call(arguments);
    let token = rafLoop.subscribe({fn: delFns[0], scope: component});
    if (delFns[1]) {
      delFns[1](token);
    }
  });

  component.ondelegation('unsubscribe.raf', (token, cb) => {
    let success = rafLoop.unsubscribe(token);
    if (cb && typeof cb == 'function') {
      cb(success);
    }
  });
});

speechToText.on('capability', isCapable => {
  if (isCapable) {
    $('#transcription-lang').removeClass('notCapable');
  }
});

$(document).ready(function() {

  mediaMgr.onenumeratedevices = function(devices) {
    runRecorder.populateDevices(devices);
    runRecorder.trackVideo();
  }

  audioAnalyser.attachCanvas($('#audioLevel'));

  speechToText.lastUpdate = (new Date()).getTime();
  speechTimeout = null;
  speechToText.on('speechresult', speechArr => {
    clearTimeout(speechTimeout);
    var text = speechArr.srResult['0']['0'].transcript;
    var isFinal = speechArr.srResult['0'].isFinal;
    var now = (new Date()).getTime();

    if (isFinal) {
      $('#cc').text(text);
      speechToText.lastUpdate = now;
      var timeoutDelay = text.length * 100
      speechTimeout = setTimeout(function() {
        $('#cc').empty();
      }, timeoutDelay);
    }
    else {
      text = `<span>${text}</span>`;
      if (now - speechToText.lastUpdate > 1000) {
        $('#cc').html(text);
      }
    }
  });
});
