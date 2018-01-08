paella.plugins.MHDescriptionPlugin  = Class.create(paella.TabBarPlugin,{
	domElement:null,
	desc: { date:'-', contributor:'-', language:'-', views:'-', serie:'-', serieId:'', presenter:'-', description:'-', title:'-', subject:'-' },
	
	
	getSubclass:function() { return "showMHDescriptionTabBar"; },
	getName:function() { return "es.upv.paella.opencast.descriptionPlugin"; },
	getTabName:function() { return paella.dictionary.translate("Description"); },
	getIndex:function() { return 10; },
	getDefaultToolTip:function() { return paella.dictionary.translate("Description"); },	
	
	
	checkEnabled:function(onSuccess) {
		var self = this;
		paella.opencast.getEpisode()
		.then(
			function(episode) {
				self._episode = episode;
				onSuccess(true);
			},
			function() { onSuccess(false); }
		);
	},	

	buildContent:function(domElement) {
		this.domElement = domElement;
		this.loadContent();
	},
			
	action:function(tab) {},
			
	loadContent:function() {
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
		this.desc.date = "n.a.";
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
	},

	insertDescription:function() {
		var divDate = document.createElement('div'); divDate.className = 'showMHDescriptionTabBarElement';
		var divContributor = document.createElement('div'); divContributor.className = 'showMHDescriptionTabBarElement';
		var divLanguage = document.createElement('div'); divLanguage.className = 'showMHDescriptionTabBarElement';
		var divViews = document.createElement('div'); divViews.className = 'showMHDescriptionTabBarElement';
		var divTitle = document.createElement('div'); divTitle.className = 'showMHDescriptionTabBarElement';
		var divSubject = document.createElement('div'); divSubject.className = 'showMHDescriptionTabBarElement';
		var divSeries = document.createElement('div'); divSeries.className = 'showMHDescriptionTabBarElement';
		var divPresenter = document.createElement('div'); divPresenter.className = 'showMHDescriptionTabBarElement';
		var divDescription = document.createElement('div'); divDescription.className = 'showMHDescriptionTabBarElement';

		divDate.innerHTML = paella.dictionary.translate("Date:")+'<span class="showMHDescriptionTabBarValue">'+this.desc.date+'</span>';
		divContributor.innerHTML = paella.dictionary.translate("Contributor:")+'<span class="showMHDescriptionTabBarValue">'+this.desc.contributor+'</span>';
		divLanguage.innerHTML = paella.dictionary.translate("Language:")+'<span class="showMHDescriptionTabBarValue">'+this.desc.language+'</span>';
		divViews.innerHTML = paella.dictionary.translate("Views:")+'<span class="showMHDescriptionTabBarValue">'+this.desc.views+'</span>';			
		divTitle.innerHTML = paella.dictionary.translate("Title:")+'<span class="showMHDescriptionTabBarValue">'+this.desc.title+'</span>';
		divSubject.innerHTML = paella.dictionary.translate("Subject:")+'<span class="showMHDescriptionTabBarValue">'+this.desc.subject+'</span>';
		divPresenter.innerHTML = paella.dictionary.translate("Presenter:")+'<span class="showMHDescriptionTabBarValue"><a tabindex="4001" href="index.html?q='+this.desc.presenter+'">'+this.desc.presenter+'</a></span>';
		divSeries.innerHTML = paella.dictionary.translate("Series:")+'<span class="showMHDescriptionTabBarValue"><a tabindex="4002" href="index.html?series='+this.desc.serieId+'">'+this.desc.serie+'</a></span>';
		divDescription.innerHTML = paella.dictionary.translate("Description:")+'<span class="showMHDescriptionTabBarValue">'+this.desc.description+'</span>';

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
	
});



paella.plugins.mhDescriptionPlugin = new paella.plugins.MHDescriptionPlugin();

