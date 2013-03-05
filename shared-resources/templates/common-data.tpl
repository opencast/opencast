<ul class="oc-ui-form-list">
     <li>
       <label class="scheduler-label" for="titleField" id="titleLabel"><span class="scheduler-required-text" style="color: red;">* </span><span id="i18n_title_label">Title</span>:</label>
       <input type="text" id="title" name="title" class="oc-ui-form-field requiredField dc-metadata-field dcMetaField" maxlength="255" />
     </li>
     <li id="titleNote" class="ui-helper-hidden">
       <label class="scheduler-label">&nbsp;</label>
       <span class="scheduler-instruction-text">
         Titles of individual recordings will be appended by sequential numbers starting with 1
       </span>
     </li>
     <li>
       <label class="scheduler-label" for="creator" id="creatorLabel"><span id="i18n_presenter_label">Presenter</span>:</label>
       <input type="text" id="creator" name="creator" class="oc-ui-form-field dc-metadata-field dcMetaField"  maxlength="255"/>
     </li>
     <li id="seriesContainer">
       <label class="scheduler-label" for="seriesSelect" id="seriesLabel"><span id="i18n_series_label">Course/Series</span>:</label>
       <input type="text" class="oc-ui-form-field ui-autocomplete-input dcMetaField" name="seriesSelect" id="seriesSelect" maxlength="255" />
       <input type="hidden" class="oc-ui-form-field dc-metadata-field" id="isPartOf" name="isPartOf">
       <input type="hidden" id="series" />
     </li>
     <li class="ui-helper-clearfix">
       <label class="scheduler-label" for="license" id="licenseLabel"><span id="i18n_license_label">License</span>:</label>
       <select id="licenseField" name="license" class="oc-ui-form-field dc-metadata-field dcMetaField">
          <option value="All Rights Reserved">All Rights Reserved</option>
          <option value="Creative Commons 3.0: Attribution-NonCommercial-NoDerivs" selected="selected">Creative Commons 3.0: Attribution-NonCommercial-NoDerivs</option>
       </select>
       <input id="license" name="license" class="oc-ui-form-field dc-metadata-field dcMetaField" type="hidden" />
     </li>
</ul>