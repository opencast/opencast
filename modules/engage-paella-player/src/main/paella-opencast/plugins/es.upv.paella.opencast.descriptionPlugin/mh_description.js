/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
paella.addPlugin(function() {
  return class MHDescriptionPlugin extends paella.TabBarPlugin {
    get domElement() { return this._domElement; }
    set domElement(v) { this._domElement = v; }
    get desc() { return this._desc; }
    set desc(v) { this._desc = v; }

    constructor() {
      super();
      this._desc = { date:'', contributor:'', language:'', views:'', serie:'', serieId:'', presenter:'', description:'', title:'', subject:'' };
    }

    getSubclass() { return 'showMHDescriptionTabBar'; }
    getName() { return 'es.upv.paella.opencast.descriptionPlugin'; }
    getTabName() { return paella.dictionary.translate('Description'); }
    getIndex() { return 10; }
    getDefaultToolTip() { return paella.dictionary.translate('Description'); }


    checkEnabled(onSuccess) {
      var self = this;
      paella.opencast.getEpisode()
      .then(
        function(episode) {
          self._episode = episode;
          onSuccess(true);
        },
        function() { onSuccess(false); }
      );
    }

    buildContent(domElement) {
      this.domElement = domElement;
      this.loadContent();
    }

    action(tab) {}

    loadContent() {
      var thisClass = this;

      if (thisClass._episode.dcTitle) { this.desc.title = thisClass._episode.dcTitle; }
      if (thisClass._episode.dcCreator) { this.desc.presenter = thisClass._episode.dcCreator; }
      if (thisClass._episode.dcContributor) { this.desc.contributor = thisClass._episode.dcContributor; }
      if (thisClass._episode.dcDescription) { this.desc.description = thisClass._episode.dcDescription; }
      if (thisClass._episode.dcLanguage) { this.desc.language = thisClass._episode.dcLanguage; }
      if (thisClass._episode.dcSubject) { this.desc.subject = thisClass._episode.dcSubject; }
      if (thisClass._episode.mediapackage.series) {
        this.desc.serie = thisClass._episode.mediapackage.seriestitle;
        this.desc.serieId = thisClass._episode.mediapackage.series;
      }
      this.desc.date = 'n.a.';
      var dcCreated = thisClass._episode.dcCreated;
      if (dcCreated) {
        var sd = new Date();
        sd.setFullYear(parseInt(dcCreated.substring(0, 4), 10));
        sd.setMonth(parseInt(dcCreated.substring(5, 7), 10) - 1);
        sd.setDate(parseInt(dcCreated.substring(8, 10), 10));
        sd.setHours(parseInt(dcCreated.substring(11, 13), 10));
        sd.setMinutes(parseInt(dcCreated.substring(14, 16), 10));
        sd.setSeconds(parseInt(dcCreated.substring(17, 19), 10));
        this.desc.date = sd.toLocaleString();
      }

      paella.ajax.get({url:'/usertracking/stats.json', params:{id:thisClass._episode.id}},
        function(data, contentType, returnCode) {
          thisClass.desc.views = data.stats.views;
          thisClass.insertDescription();
        },
        function(data, contentType, returnCode) {
          thisClass.insertDescription();
        }
      );
    }

    insertDescription() {
      var divDate = document.createElement('div'); divDate.className = 'showMHDescriptionTabBarElement';
      var divContributor = document.createElement('div'); divContributor.className = 'showMHDescriptionTabBarElement';
      var divLanguage = document.createElement('div'); divLanguage.className = 'showMHDescriptionTabBarElement';
      var divViews = document.createElement('div'); divViews.className = 'showMHDescriptionTabBarElement';
      var divTitle = document.createElement('div'); divTitle.className = 'showMHDescriptionTabBarElement';
      var divSubject = document.createElement('div'); divSubject.className = 'showMHDescriptionTabBarElement';
      var divSeries = document.createElement('div'); divSeries.className = 'showMHDescriptionTabBarElement';
      var divPresenter = document.createElement('div'); divPresenter.className = 'showMHDescriptionTabBarElement';
      var divDescription = document.createElement('div'); divDescription.className = 'showMHDescriptionTabBarElement';

      divDate.innerHTML = paella.dictionary.translate('Date')+': <span class="showMHDescriptionTabBarValue">'+this.desc.date+'</span>';
      divContributor.innerHTML = paella.dictionary.translate('Contributor')+': <span class="showMHDescriptionTabBarValue">'+this.desc.contributor+'</span>';
      divLanguage.innerHTML = paella.dictionary.translate('Language')+': <span class="showMHDescriptionTabBarValue">'+this.desc.language+'</span>';
      divViews.innerHTML = paella.dictionary.translate('Views')+': <span class="showMHDescriptionTabBarValue">'+this.desc.views+'</span>';
      divTitle.innerHTML = paella.dictionary.translate('Title')+': <span class="showMHDescriptionTabBarValue">'+this.desc.title+'</span>';
      divSubject.innerHTML = paella.dictionary.translate('Subject')+': <span class="showMHDescriptionTabBarValue">'+this.desc.subject+'</span>';
      if (this.desc.presenter == '') {
        divPresenter.innerHTML = paella.dictionary.translate('Presenter')+': <span class="showMHDescriptionTabBarValue"></span>';
      }
      else {
        divPresenter.innerHTML = paella.dictionary.translate('Presenter')+': <span class="showMHDescriptionTabBarValue"><a tabindex="4001" href="/engage/ui/index.html?q='+this.desc.presenter+'">'+this.desc.presenter+'</a></span>';
      }
      if (this.desc.serieId == '') {
        divSeries.innerHTML = paella.dictionary.translate('Series')+': <span class="showMHDescriptionTabBarValue"></span>';
      }
      else {
        divSeries.innerHTML = paella.dictionary.translate('Series')+': <span class="showMHDescriptionTabBarValue"><a tabindex="4002" href="/engage/ui/index.html?epFrom='+this.desc.serieId+'">'+this.desc.serie+'</a></span>';
      }
      divDescription.innerHTML = paella.dictionary.translate('Description')+': <span class="showMHDescriptionTabBarValue">'+this.desc.description+'</span>';

      //---------------------------//
      var divLeft = document.createElement('div');
      divLeft.className = 'showMHDescriptionTabBarLeft';

      divLeft.appendChild(divTitle);
      divLeft.appendChild(divPresenter);
      divLeft.appendChild(divSeries);
      divLeft.appendChild(divDate);
      divLeft.appendChild(divViews);

      //---------------------------//
      var divRight = document.createElement('div');
      divRight.className = 'showMHDescriptionTabBarRight';

      divRight.appendChild(divContributor);
      divRight.appendChild(divSubject);
      divRight.appendChild(divLanguage);
      divRight.appendChild(divDescription);


      this.domElement.appendChild(divLeft);
      this.domElement.appendChild(divRight);
    }
  };
});

