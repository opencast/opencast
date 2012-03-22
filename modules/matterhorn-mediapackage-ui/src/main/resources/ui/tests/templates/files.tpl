        <div class="form-box-head ui-widget-header ui-corner-top oc-ui-cursor unfoldable-header">
                  <div class="ui-icon unfoldable-icon ui-icon-triangle-1-e"></div>
                  <div id="i18n_upload_title">File upload</div>
                  <div class="clear"></div>
         </div>
        <div class="form-box-content ui-widget-content ui-corner-bottom">

          <input type="hidden" name="track" id="track" class="requiredField" value="">

          <div style="width:100px; margin-left: auto; margin-right: auto; margin-bottom:10px;">
            <input class="uploadType-single uploadtype-select" type="radio" name="singlMultiSwitch" id="singleUploadRadio" value="single" checked="checked"/>
            <label for="singleUploadRadio" class="lbl_radio">Single File</label>
            <br />
            <input class="uploadType-multi uploadtype-select" type="radio" name="singlMultiSwitch" id="multiUploadRadio" value="multi" />
            <label for="multiUploadRadio" class="lbl_radio">Multiple Files</label>
          </div>

          <div id="uploadContainerSingle">
            <ul class="oc-ui-form-list upload-widget">
              <li class="ui-helper-clearfix" id="regularFileSelection">
                <label class="scheduler-label"><span id="i18n_file_location">File Location</span>:</label>
                <input type="radio" class="file-source-select" name="fileSourceSingle" id="fileSourceSingleA" value="local" checked="true">
                <label for="fileSourceSingleA" class="lbl_radio">Local hard drive</label>
                &nbsp;&nbsp;

                <input type="radio" class="file-source-select" name="fileSourceSingle" id="fileSourceSingleB" value="inbox">
                <label for="fileSourceSingleB" class="lbl_radio">Designated inbox on server</label>
              </li>
              <!-- field: Media File -->
              <li class="ui-helper-clearfix">
                <label class="scheduler-label"><span class="color-red">* </span><span id="i18n_upload_file">Media File</span>:</label>
                <iframe class="uploadForm-container" frameborder="0" scrolling="no" src="../ingest/filechooser-local.html" class="uploadForm-container"></iframe>

              </li>
              <li class="ui-helper-clearfix">
                <label class="scheduler-label"><span id="i18n_upload_flavor">Media Characteristics</span>:</label>
                <input type="checkbox" class="flavor-presentation-checkbox" id="containsSlides">
                <input type="hidden" class="track-flavor" value="presenter/source">
                <label for="containsSlides" style="text-align: left; width: 300px;">
                  Contains discrete images/slides/scenes.
                </label>
              </li>

              <li class="ui-helper-clearfix">
                <label class="scheduler-label">&nbsp;</label>
                <span style="font-size:80%;color:gray;">(Analysis and processing will occur to enable slide thumbnail navigation and possibly</span>
              </li>
              <li class="ui-helper-clearfix">
                <label class="scheduler-label">&nbsp;</label>
                <span style="font-size:80%;color:gray;">in-video text search)</span>
              </li>

            </ul>

          </div>

          <!-- MULTI FILE UPLOAD -->
          <div id="uploadContainerMulti" style="display:none;">

            <fieldset class="upload-widget">
              <legend>File with segmentable video</legend>
              <div class="informationText">
                Contains discrete images/slides/scenes (example: VGA output of presentation). Audio in this file
                will be ignored if File with non-segmentable video contains audio.
              </div>
              <ul class="oc-ui-form-list">
                <li class="ui-helper-clearfix" id="regularFileSelection">
                  <label class="scheduler-label"><span class="i18n_file_location">File Location</span>:</label>
                  <input type="radio" class="file-source-select"  name="fileSourcePresentation" id="fileSourcePresentationA" value="local" checked="true">
                  <label for="fileSourcePresentationA" class="lbl_radio">Local hard drive</label>
                  &nbsp;&nbsp;

                  <input type="radio" class="file-source-select" name="fileSourcePresentation" id="fileSourcePresentationB" value="inbox">
                  <label for="fileSourcePresentationB" class="lbl_radio">Designated inbox on server</label>
                </li>
                <!-- field: Media File -->
                <li class="ui-helper-clearfix" id="regularFileChooser">
                  <label class="scheduler-label"><span class="i18n_upload_file">Media File</span>:</label>
                  <iframe class="uploadForm-container" frameborder="0" scrolling="no" src="../ingest/filechooser-local.html" class="uploadForm-container"></iframe>

                  <input type="hidden" class="track-flavor" value="presentation/source">
                </li>
              </ul>
            </fieldset>

            <fieldset class="upload-widget">
              <legend>File with non-segmentable video</legend>
              <div class="informationText">
                Has no clear changes that could identify new segments (example: video of the presenter).
              </div>

              <ul class="oc-ui-form-list">
                <li class="ui-helper-clearfix" id="regularFileSelection">
                  <label class="scheduler-label"><span class="i18n_file_location">File Location</span>:</label>
                  <input type="radio" class="file-source-select" name="fileSourcePresenter" id="fileSourcePresenterA" value="local" checked="true">
                  <label for="fileSourcePresenterA" class="lbl_radio">Local hard drive</label>
                  &nbsp;&nbsp;
                  <input type="radio" class="file-source-select" name="fileSourcePresenter" id="fileSourcePresenterB" value="inbox">

                  <label for="fileSourcePresenterB" class="lbl_radio">Designated inbox on server</label>
                </li>
                <!-- field: Media File -->
                <li class="ui-helper-clearfix" id="regularFileChooserMultiPresenter">
                  <label class="scheduler-label"><span class="i18n_upload_file">Media File</span>:</label>
                  <iframe class="uploadForm-container" frameborder="0" scrolling="no" src="../ingest/filechooser-local.html" class="uploadForm-container"></iframe>
                  <input type="hidden" class="track-flavor" value="presenter/source">

                </li>
              </ul>
            </fieldset>

            <fieldset class="upload-widget">
              <legend>Audio-only file</legend>
              <div class="informationText">
                This file's audio will only be used if no other files contain audio.
              </div>
              <ul class="oc-ui-form-list">
                <li class="ui-helper-clearfix" id="regularFileSelection">

                  <label class="scheduler-label"><span class="i18n_file_location">File Location</span>:</label>
                  <input type="radio" class="file-source-select" name="fileSourceAudio" id="fileSourceAudioA" value="local" checked="true">
                  <label for="fileSourceAudioA" class="lbl_radio">Local hard drive</label>
                  &nbsp;&nbsp;
                  <input type="radio" class="file-source-select" name="fileSourceAudio" id="fileSourceAudioB" value="inbox">
                  <label for="fileSourceAudioB" class="lbl_radio">Designated inbox on server</label>
                </li>

                <!-- field: Media File -->
                <li class="ui-helper-clearfix" id="regularFileChooserAudioOnly">
                  <label class="scheduler-label"><span class="i18n_upload_file">Media File</span>:</label>
                  <iframe class="uploadForm-container" frameborder="0" scrolling="no" src="../ingest/filechooser-local.html" class="uploadForm-container"></iframe>
                  <input type="hidden" class="track-flavor" value="presenter-audio/source">
                </li>
              </ul>

            </fieldset>
            
            <fieldset class="upload-widget">
              <legend>Captions file</legend>
              <div class="informationText">
                Upload a captions file in dfxp format.
              </div>
              <ul class="oc-ui-form-list">
                <li class="ui-helper-clearfix" id="regularFileSelection">

                  <label class="scheduler-label"><span class="i18n_file_location">File Location</span>:</label>
                  <input type="radio" class="file-source-select" name="fileSourceCaption" id="fileSourceAudioA" value="local" checked="true">
                  <label for="fileSourceCaptionA" class="lbl_radio">Local hard drive</label>
                  &nbsp;&nbsp;
                  <input type="radio" class="file-source-select" name="fileSourceCaption" id="fileSourceAudioB" value="inbox">
                  <label for="fileSourceCaptionB" class="lbl_radio">Designated inbox on server</label>
                </li>

                <!-- field: Media File -->
                <li class="ui-helper-clearfix" id="regularFileChooserCaptions">
                  <label class="scheduler-label"><span class="i18n_upload_file">Media File</span>:</label>
                  <iframe class="uploadForm-container" frameborder="0" scrolling="no" src="../ingest/filechooser-local.html?elementType=catalog" class="uploadForm-container"></iframe>
                  <input type="hidden" class="track-flavor" value="captions/timedtext">
                </li>
              </ul>

            </fieldset>

          </div>
          <!-- END OF MULTI FILE UPLOAD -->
        </div>