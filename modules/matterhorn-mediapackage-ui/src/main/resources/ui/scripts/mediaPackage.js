/**
 * Data classes used for the MediaPackage Editor
 */

/**
 * MediaPackage Class
 */
MediaPackage = function(xmlMediaPackage){
	var self = this;
	
	// Instance variables
	this.attachments = new Array();
	this.catalogs = new Array();
	this.creators = new Array();
	this.duration = '';
	this.episodeCatalogs = new Array();
	this.episodeCatalog = new Catalog();
	this.episodeCatalog.flavor = "dublincore/episode";
	this.id = '';
	this.seriesCatalogs = new Array();
	this.seriesCatalog = new Catalog();
	this.seriesCatalog.flavor = "dublincore/series";
	this.seriesId = '';
	this.seriesTitle = '';
	this.start = '';
	this.tracks = new Array();
	this.xml = '';
    $.xmlns["mp"] = "http://mediapackage.opencastproject.org";

	/**
	 * Parse the given xml string to fill the mediapackage instance
	 * 
	 * @param xml the xml document as string
	 */
	this.parseXML = function(xml){
		if(!xml)throw "An XML mediapackage must be given!";
		
		self.xml = xml;
		
		// Get all catalogs
		$(self.xml).find('mp|catalog').each(function(index,element){
			var tmpCatalog = new Catalog($(element).find('mp|url').text());
			if(checkValue($(element).attr('type')))
				tmpCatalog.flavor = $(element).attr('type');
			if(checkValue($(element).attr('ref')))
				tmpCatalog.ref = $(element).attr('ref');
			if(checkValue($(element).attr('id')))
				tmpCatalog.id = $(element).attr('id');
			if(checkValue($(element).find('mimetype').text()))
				tmpCatalog.mimetype = $(element).find('mp|mimetype').text();
			tmpCatalog.xml = element;
			$(element).find('mp|tags').each(function(index,subElement){
			  if(checkValue($(subElement).text()))
			    tmpCatalog.tags.push($(subElement).text());
			});
						
			if(tmpCatalog.flavor == 'dublincore/episode') {
				self.episodeCatalogs.push(tmpCatalog);
			} else if(tmpCatalog.flavor == 'dublincore/series') {
				self.seriesCatalogs.push(tmpCatalog);
			} else {
				self.catalogs.push(tmpCatalog);
			}
		});
		
		// Set the correct episode and series catalog to edit
		var tmpCatalogs;
		if(!$.isEmptyObject(self.episodeCatalogs)) {
			tmpCatalogs = self.findCorrectCatalog(self.episodeCatalogs);
			if(tmpCatalogs != null && tmpCatalogs.length == 1) {
				self.episodeCatalog = tmpCatalogs[0];
			}
			// TODO Make multiple episode Catalogs possible
			else if(tmpCatalogs != null && tmpCatalogs.length > 1) {
				alert("Multiple episode catalogs aren't implemented yet! Do not use this plugin, it will not working properly!");
			}
		}
		if(!$.isEmptyObject(self.seriesCatalogs)) {
			tmpCatalogs = self.findCorrectCatalog(self.seriesCatalogs);
			if(tmpCatalogs != null && tmpCatalogs.length == 1) {
				self.seriesCatalog = tmpCatalogs[0];
			}
			else if(tmpCatalogs != null && tmpCatalogs.length > 1) {
				alert("Multiple series catalogs aren't implemented yet! Do not use this plugin, it will not working properly!");
			}
		}
		
		// Get all tracks
		$(self.xml).find('mp|track').each(function(index,element){
			var tmpTrack = new Track();
			tmpTrack.url = $(element).find('mp|url').text();
			var checksum = $(element).find('mp|checksum');
			tmpTrack.checksum.value = checksum.text();
			tmpTrack.checksum.type = checksum.attr('type');
			tmpTrack.duration = $(element).find('mp|duration').text();
			tmpTrack.mimetype = $(element).find('mp|mimetype').text();
			tmpTrack.flavor = $(element).attr('type');
			tmpTrack.ref = $(element).attr('ref');
			tmpTrack.id = $(element).attr('id');
			$(element).find('mp|tags').each(function(index,subElement){
				tmpTrack.tags.push($(subElement).text());
			});
			
			var audio = $(element).find('mp|audio');
			if(audio.length!=0){
				tmpTrack.audio['id'] = audio.attr('id');
				tmpTrack.audio['bitrate'] = audio.find('bitrate').text();
				tmpTrack.audio['channels'] = audio.find('channels').text();
				
				// Add device and encoder elements
				$.each({1:'device', 2:'encoder'},function(index,value){
					var tmpObj = {};
					var tmpObjElm = audio.find(value);
					if(tmpObj.length!=0){
						tmpObj['type'] = tmpObjElm.attr('type');
						tmpObj['version'] = tmpObjElm.attr('version');
						tmpObj['vendor'] = tmpObjElm.attr('vendor');
					}
					tmpTrack.audio[value] = tmpObj;		
				});
			}
			
			var video = $(element).find('mp|video');
			if(video.length!=0){
				tmpTrack.video['id'] = video.attr('id');
				tmpTrack.video['bitrate'] = video.find('mp|bitrate').text();
				tmpTrack.video['framerate'] = video.find('mp|framerate').text();
				tmpTrack.video['resolution'] = video.find('mp|resolution').text();
				tmpTrack.video['scanType'] = video.find('mp|scantype').attr('type');
				tmpTrack.video['scanOrder'] = video.find('mp|interlacing').attr('order');
				
				// Add device and encoder elements
				$.each({1:'device', 2:'encoder'},function(index,value){
					var tmpObj = {};
					var tmpObjElm = video.find(value);
					if(tmpObj.length!=0){
						tmpObj['type'] = tmpObjElm.attr('type');
						tmpObj['version'] = tmpObjElm.attr('version');
						tmpObj['vendor'] = tmpObjElm.attr('vendor');
					}
					tmpTrack.video[value] = tmpObj;		
				});
			}
			
			tmpTrack.xml = element;
			self.tracks.push(tmpTrack);
		});
		
		//Get all attachment
		$(self.xml).find('mp|attachment').each(function(index,element){
			var tmpAttachment = new Attachment();
			tmpAttachment.url = $(element).find('mp|url').text();
			var checksum = $(element).find('mp|checksum');
			tmpAttachment.checksum.value = checksum.text();
			tmpAttachment.checksum.type = checksum.attr('type');
			tmpAttachment.mimetype = $(element).find('mp|mimetype').text();
			tmpAttachment.type = $(element).attr('type');
			tmpAttachment.ref = $(element).attr('ref');
			tmpAttachment.id = $(element).attr('id');
			$(element).find('mp|tags').each(function(index,subElement){
				tmpAttachment.tags.push($(subElement).text());
			});
			
			tmpAttachment.xml = element;
			self.attachments.push(tmpAttachment);
		});
		
		// Get MediaPackage attributes
		self.id = $(self.xml).attr('id');
		self.duration = $(self.xml).attr('duration');
		self.seriesTitle = $(self.xml).find('mp|seriestitle').text();
		self.seriesId = $(self.xml).find('mp|series').text();
	};
	
	/**
	 * Sets the mediapackage id
	 */
	this.setId = function(id) {
		self.id = id;
	}
	
	/**
	 * Return generated mediapackage xml as string
	 */
	this.asString = function() {
		return xmlToString(self.toXML());
	}
	 
	
	/**
	 * Generate an XML Document with the mediaPackage
	 */
	this.toXML = function(){
		var doc = createDoc('mediapackage','http://mediapackage.opencastproject.org');
		
		// Add MediaPackage attributes
		if(checkValue(self.id))
			doc.documentElement.setAttribute('id',self.id);
		if(checkValue(self.episodeCatalog.getValue('created')))
			doc.documentElement.setAttribute('start', self.episodeCatalog.getValue('created'));
		if(checkValue(self.duration))
			doc.documentElement.setAttribute('duration',self.duration);
		
		// Add MediaPackage element
		if(checkValue(self.episodeCatalog.getValue('creator'))){
			var creators = addElementToXmlDoc('creators',doc.documentElement,doc);
			addTextElementToXmlDoc('creator',self.episodeCatalog.getValue('creator'),creators,doc);
		}
		if(checkValue(self.episodeCatalog.getValue('contributor'))){
			var contributors = addElementToXmlDoc('contributors',doc.documentElement,doc);
			addTextElementToXmlDoc('contributor',self.episodeCatalog.getValue('contributor'),contributors,doc);
		}
		if(checkValue(self.episodeCatalog.getValue('license')))
			addTextElementToXmlDoc('license',self.episodeCatalog.getValue('license'),doc.documentElement,doc);
		if(checkValue(self.seriesId))
			addTextElementToXmlDoc('series',self.seriesId,doc.documentElement,doc);
		if(checkValue(self.seriesTitle))
			addTextElementToXmlDoc('seriestitle',self.seriesTitle,doc.documentElement,doc);
		if(checkValue(self.episodeCatalog.getValue('subject')))
			addTextElementToXmlDoc('subjects',self.episodeCatalog.getValue('subject'),doc.documentElement,doc);
		if(checkValue(self.episodeCatalog.getValue('title')))
			addTextElementToXmlDoc('title',self.episodeCatalog.getValue('title'),doc.documentElement,doc);
		
		
		// Add catalogs
		var allCatalogs = cloneArray(self.catalogs);
		allCatalogs.push(self.episodeCatalog);
		allCatalogs.push(self.seriesCatalog);
		$.each(self.episodeCatalogs, function(i, cat) {
			allCatalogs.push(cat);
		});
		$.each(self.seriesCatalogs, function(i, cat) {
			allCatalogs.push(cat);
		});
		
		if(allCatalogs.length > 0){
			var metadata = addElementToXmlDoc('metadata',doc.documentElement,doc);
			$.each(allCatalogs,function(index,catalog){
				// If catalog is valid
				if(catalog.hasRealId() && checkValue(catalog.url) && !checkValue(catalog.error)){
					var catElement = addElementToXmlDoc('catalog', metadata, doc);
					if(checkValue(catalog.flavor))catElement.setAttribute('type',catalog.flavor);
					if(checkValue(catalog.id))catElement.setAttribute('id',catalog.id);
					if(checkValue(catalog.ref))catElement.setAttribute('ref',catalog.ref);
					
					addTextElementToXmlDoc('mimetype',catalog.mimetype,catElement,doc);
					addTextElementToXmlDoc('url',catalog.url,catElement,doc);
					
					if(catalog.tags.length > 0){
						var tags = addElementToXmlDoc('tags',catElement,doc);
						$.each(catalog.tags,function(index,value){
							if(checkValue(value))
								addTextElementToXmlDoc('tag', value, tags, doc);
						});
					}
				}
			});
		}
		
		// Add tracks
		if(self.tracks.length >0){
			var media = addElementToXmlDoc('media',doc.documentElement,doc);
			$.each(self.tracks,function(index,track){
				// If catalog is valid
				if(checkValue(track.id) && checkValue(track.url)){
					var trackElement = addElementToXmlDoc('track', media, doc);
					if(checkValue(track.ref))trackElement.setAttribute('ref',track.ref);
					if(checkValue(track.flavor))trackElement.setAttribute('type',track.flavor);
					if(checkValue(track.id))trackElement.setAttribute('id',track.id);
	
					addTextElementToXmlDoc('mimetype',track.mimetype,trackElement,doc);
					addTextElementToXmlDoc('url',track.url,trackElement,doc);
					if(checkValue(track.checksum.value)) {
					  var checksumElm = addTextElementToXmlDoc('checksum',track.checksum.value,trackElement,doc);
					  checksumElm.setAttribute('type',track.checksum.type);
					}
					addTextElementToXmlDoc('duration',track.duration,trackElement,doc);
					
					//Video element
					if(!$.isEmptyObject(track.video)){
						var videoElement = addElementToXmlDoc('video',trackElement,doc);
						if(checkValue(track.video.id))videoElement.setAttribute('id',track.id);
						addTextElementToXmlDoc('bitrate',track.video.bitrate,videoElement,doc);
						addTextElementToXmlDoc('framerate',track.video.framerate,videoElement,doc);
						addTextElementToXmlDoc('resolution',track.video.resolution,videoElement,doc);
						//ScanType element
						if(checkValue(track.video.scantype)){
							var scantypeElement = addElementToXmlDoc('scantype',videoElement,doc);
							scantypeElement.setAttribute('type',track.video.scantype);
						}
						//ScanOrder element
						if(checkValue(track.video.scanorder)){
							var scanorderElement = addElementToXmlDoc('interlacing',videoElement,doc);
							scanorderElement.setAttribute('order',track.video.scanorder);
						}
						// Add device and encoder elements
						$.each({1:'device', 2:'encoder'},function(index,value){
							if(checkValue(track.video[value]) && !$.isEmptyObject(track.video[value])){
								var tmpObj = track.video[value];
								var tmpElm = addElementToXmlDoc(value,videoElement,doc);
								if(checkValue(tmpObj)){
									if(checkValue(tmpObj.type))tmpElm.setAttribute('type',tmpObj.type);
									if(checkValue(tmpObj.version))tmpElm.setAttribute('version',tmpObj.version);
									if(checkValue(tmpObj.vendor))tmpElm.setAttribute('vendor',tmpObj.vendor);
								}
							}
						});
					}
					//Audio element
					if(!$.isEmptyObject(track.audio)){
						var audioElement = addElementToXmlDoc('audio',trackElement,doc);
						if(checkValue(track.audio.id))audioElement.setAttribute('id',track.id);
						if(checkValue(track.audio.bitrate))addTextElementToXmlDoc('bitrate',track.audio.bitrate,audioElement,doc);
						if(checkValue(track.audio.channels))addTextElementToXmlDoc('channels',track.audio.channels,audioElement,doc);
						// Add device and encoder elements
						$.each({1:'device', 2:'encoder'},function(index,value){
							if(checkValue(track.audio[value]) && !$.isEmptyObject(track.video[value])){
								var tmpObj = track.audio[value];
								var tmpElm = addElementToXmlDoc(value,audioElement,doc);
								if(checkValue(tmpObj)){
									if(checkValue(tmpObj.type))tmpElm.setAttribute('type',tmpObj.type);
									if(checkValue(tmpObj.version))tmpElm.setAttribute('version',tmpObj.version);
									if(checkValue(tmpObj.vendor))tmpElm.setAttribute('vendor',tmpObj.vendor);
								}
							}
						});
					}
	
					if(track.tags.length > 0){
						var tags = addElementToXmlDoc('tags',trackElement,doc);
						$.each(track.tags,function(index,value){
							if(checkValue(value))
								addTextElementToXmlDoc('tag', value, tags, doc);
						});
					}
				}
			});
		}
		
		// Add attachments
		if(self.attachments.length > 0){
			var attachments = addElementToXmlDoc('attachments',doc.documentElement,doc);
			$.each(self.attachments,function(index,attachment){
				// If attachments is valid
				if(checkValue(attachment.id) && checkValue(attachment.url)){
					var attachmentElement = addElementToXmlDoc('attachment', attachments, doc);
					if(checkValue(attachment.ref))attachmentElement.setAttribute('ref',attachment.ref);
					if(checkValue(attachment.type))attachmentElement.setAttribute('type',attachment.type);
					if(checkValue(attachment.id))attachmentElement.setAttribute('id',attachment.id);
	
					addTextElementToXmlDoc('mimetype',attachment.mimetype,attachmentElement,doc);
					addTextElementToXmlDoc('url',attachment.url,attachmentElement,doc);
					if(checkValue(attachment.checksum.value)) {
					  var checksumElm = addTextElementToXmlDoc('checksum',attachment.checksum.value,attachmentElement,doc);
					  checksumElm.setAttribute('type',attachment.checksum.type);
					}
				}
				
				if(attachment.tags.length > 0){
					var tags = addElementToXmlDoc('tags',attachmentElement,doc);
					$.each(attachment.tags,function(index,value){
						if(checkValue(value))
							addTextElementToXmlDoc('tag', value, tags, doc);
					});
				}
			});
		}
		
		return doc;
	}
	
	/**
	 * Make a deep copy of the mediapackage instance
	 * 
	 * @return a clone of the mediapackage instance
	 */
	this.clone = function(){
		var newMP = new MediaPackage();
		newMP.xml = self.xml;
		newMP.title = self.title;
		newMP.duration = self.duration;
		newMP.id = self.id;
		newMP.seriesCatalogs = cloneArray(self.seriesCatalogs);
		newMP.seriesCatalog = self.seriesCatalog.clone();
		newMP.episodeCatalogs = cloneArray(self.episodeCatalogs);
		newMP.episodeCatalog = self.episodeCatalog.clone();
		newMP.seriesId = self.seriesId;
		newMP.seriesTitle = self.seriesTitle;
		$.each(self.catalogs,function(idx,value){
			newMP.catalogs[idx]=value.clone();
		});
		$.each(self.tracks,function(idx,value){
			newMP.tracks[idx]=value.clone();
		});
		$.each(self.attachments,function(idx,value){
			newMP.attachments[idx]=value.clone();
		});
		return newMP;
	}
	
	/**
	 * Gets the index of a catalog in the mediapackage
	 */
	this.getCatalogIndex = function(catalog) {
		var index = -1;
		$.each(self.catalogs, function(idx, value){
			if(value.id == catalog.id){
				index = idx;
				return false;
			}
		});
		return index;
	}
	
	/**
	 * Adds a catalog to the mediapackage
	 */
	this.addCatalog = function(catalog) {
		self.catalogs.push(catalog);
	}
	
	/**
	 * Updates a catalog to the mediapackage
	 */
	this.changeCatalog = function(catalog) {
		var index = self.getCatalogIndex(catalog);
		if(index == -1) return;
		self.catalogs.splice(index, 1, catalog);
	}
	
	/**
	 * Deletes a catalog from the mediapackage
	 */
	this.deleteCatalog = function(catalog) {
		var index = self.getCatalogIndex(catalog);
		if(index == -1) return;
		self.catalogs.splice(index, 1);
	}
	
	/**
	 * Get the catalog with the same flavor or null if no one
	 * 
	 * @param flavor the flavor of the wanted catalog
	 * 
	 * @return the wanted catalog if present or null
	 */
	 this.getCatalogsByFlavor = function(flavor){
		 var catalogs = new Array();
		 $.each(self.catalogs, function(idx, cat) {
			 if(cat.flavor == flavor){
				 catalogs.push(cat);
			 }
		 });
		 if(catalogs.length == 0) return null;
		 else if(catalogs.length == 1) return catalogs;
		 else if(catalogs.length > 1) {
			 catalogs = self.findEditCatalogs(catalogs);
			 if(catalogs.length == 0) return null;
			 return catalogs;
		 }
		 return null;
	};
	
	/**
	 * Get the catalog with the given id or null
	 * 
	 * @param id the id of the wanted catalog
	 * 
	 * @return the wanted catalog if present or null
	 */
	this.getCatalogById = function(id) {
		var catalog = null 
		$.each(self.catalogs, function(idx, cat) {
			 if(cat.id == id){
				 catalog = cat;
				 return false;
			 }
		});
		return catalog;
	}
	
	/**
	 * Try to determine correct episode/series catalog and delete in original array if found
	 */
	this.findCorrectCatalog = function(catalogs) {
		var copyArray = new Array();
		
		$.each(catalogs, function(i, cat) {
			if(cat.ref == '') {
				copyArray.push(cat);
				return;
			}
			
			var tmp = cat.ref.split(':');
			if(tmp[0] != 'catalog') {
				copyArray.push(cat);
				return;
			}
			else if(!$.isEmptyObject(tmp[1]) && tmp[1] != '') {
				var catalogById = null;
				$.each(catalogs, function(idx, c) {
					 if(tmp[1] == c.id){
						 catalogById = c;
						 return false;
					 }
				});
				if(catalogById == null)
					copyArray.push(cat);
			} else {
				copyArray.push(cat);
			}
		});
		
		// Remove correct catalogs from array
		$.each(copyArray, function(idx, cat){
			var index = -1;
			$.each(catalogs, function(i, c) {
				 if(c.id == cat.id){
					 index = i;
					 return false;
				 }
			});
			if(index != -1)
				catalogs.splice(index, 1);
		});
		
		return copyArray;
	}
	
	/**
	 * Try to determine which catalog is used to edit if multiple catalogs with the same type are existing.
	 */
	this.findEditCatalogs = function(catalogs) {
		var copyArray = new Array();
		
		$.each(catalogs, function(index, cat) {
			if(cat.ref == '') {
				copyArray.push(cat);
				return;
			}
			
			var tmp = cat.ref.split(':');
			if(tmp[0] != 'catalog') {
				copyArray.push(cat);
				return;
			}
			else if(!$.isEmptyObject(tmp[1]) && tmp[1] != '') {
				var catalogById = self.getCatalogById(tmp[1]);
				if(catalogById == null)
					copyArray.push(cat);
			} else {
				copyArray.push(cat);
			}
		});
		return copyArray;
	}
	
	/**
	  * Update the dublincore episode catalog
	  */
	 this.updateDCEpisode = function(episodeCatalog) {
	  self.episodeCatalog = episodeCatalog;
	 }
	 
	 /**
	  * Updates the series catalog from the medipackage
	  */
	 this.updateDCSeries = function(id, title, dublincore) {
	  self.seriesId = id;
	  self.seriesTitle = title;
	  self.seriesCatalog.parseXML(dublincore);
	 }
	 
	 /**
	  * Updates the series catalog from the medipackage
	  */
	 this.updateSeriesCatalog = function(id, title, seriesCatalog) {
		 self.seriesId = id;
		 self.seriesTitle = title;
		 self.seriesCatalog = seriesCatalog;
	 }
	 
	 /**
	  * Deletes the series catalog from the mediapackage
	  */
	 this.deleteDCSeries = function() {
	  self.seriesId = '';
	  self.seriesTitle = '';
	 }
	
	if(xmlMediaPackage)
		this.parseXML(xmlMediaPackage);
	
	return this;
};

/**
 * Catalog class
 */
Catalog = function(url){
	var self = this;
	
	this.disable = false;
	this.id = '';
	this.flavor = '';
	this.namespace = '';
	this.error = '';
	this.mimetype = 'text/xml';
	this.ref = '';
	this.tags = new Array();
	if(checkValue(url))
		this.url = url;
	else
		this.url = '';
	this.values = {};
	this.xml;
	
	this.getXML = function(url){
		
	  $.ajax({
	     url: url,
	     dataType: "xml",
	     async: false,
	     success: function(data){
	      self.parseXML(data);
	     }
	  }); 
	};
	
	this.clone = function(){
		var newCatalog = new Catalog();
		newCatalog.namespace = self.namespace;
		newCatalog.flavor = self.flavor;
		newCatalog.disable = self.disable;
		newCatalog.values = clone(self.values);
		newCatalog.id = self.id;
		newCatalog.mimetype = self.mimetype;
		newCatalog.ref = self.ref;
		newCatalog.url = self.url;
		newCatalog.xml = this.xml;
		return newCatalog;
	};
	
	this.equals = function(catalog) {
		if(self.disable != catalog.disable)
			return false;
		if(self.url != catalog.url)
			return false;
		if(self.flavor != catalog.flavor)
			return false;
		if(self.namespace != catalog.namespace)
			return false;
		if(self.id != catalog.id)
			return false;
		if(self.error != catalog.error)
			return false;
		if(self.mimetype != catalog.mimetype)
			return false;
		if(self.ref != catalog.ref)
			return false;

		var valuesEquals = true;
		$.each(self.values, function(idx,values){
			var tmpValues = catalog.values[idx];
			if(tmpValues == undefined) {
			  valuesEquals = false;
			  return false;
			}
			
			$.each(values,function(subidx,value){
				var tmpValue = tmpValues[subidx];
				if(value.value != tmpValue.value)valuesEquals=false;
				if(value.lang != tmpValue.lang)valuesEquals=false;
				if(value.type != tmpValue.type)valuesEquals=false;
			});
		});
		$.each(self.tags, function(idx,value){
			if(value != catalog.tags[idx]){
				valuesEquals = false;
				return false;
			}
		});
		
		return valuesEquals;
	};
	
	this.update = function(key, value){
		var target = self.values[key];
		
		if(target == undefined){
			self.values[key] = new Array();
			self.values[key].push({
				value: value,
				lang: '',
				type: ''
			})
		}
		else{
			target[0].value = value;
		}
	}
	
	this.generateCatalog = function() {
		var dc = createCatalog(self.values);
		return xmlToString(dc);
	}
	
	this.parseXML = function(xml){
		if(!xml)throw "An XML mediapackage must be given!";
		self.xml = xml;
		var elementName = "";
		$(self.xml).find('dublincore').children().each(function(index,element){
			
			if(self.namespace=='') self.namespace = element.prefix;
			if(elementName != element.localName.toLowerCase()){
				elementName = element.localName.toLowerCase();
				self.values[elementName] = new Array();
			}
			
			var value = {
					value: $(element).text(),
					lang: $(element).attr('xml:lang'),
					type: $(element).attr('xsi:type')
			};
		   
			self.values[elementName].push(value);
		});
	};
		 
	/**
	 * Check if the catalog does not have a temporary id
	 * 
	 * @return true if the id is not temporary
	 */
	this.hasRealId = function(){
		var regEx = new RegExp("^tmp[0-9]{1,5}");
		return self.id.match(regEx) == null;
	}
	
	/**
	 * Get the value by name
	 * 
	 * @param valueName the name of the value to get
	 * @return the value with the given name or null if none
	 */
	this.getValue = function(valueName){
		var values = self.values[valueName];
		
		if(checkValue(values) && values.length > 0){
			if(checkValue(values[0].value))
				return values[0].value;
		}		
		
		return "";
	}
	
	/**
	 * Delete the value with the given name
	 * 
	 * @param valueName the name of the value to delete
	 */
	this.deleteValue = function(valueName){
		var values = self.values[valueName];
		if(checkValue(values) && values.length > 0)
				values.splice(0,1);
		if($.isEmptyObject(values))
			delete self.values[valueName];
	}
	
	if(url)
		this.getXML(url);
	
	if(!checkValue(self.id))
		self.id = "tmp"+(Math.floor(Math.random()*10001));
	
	return this;
};

/**
 * Track class
 */
Track = function(){	
	var self = this;
	
	this.audio = {};
	this.checksum = {
	    value: '',
	    type: 'md5'
	};
	this.duration = '';
	this.flavor = '';
	this.id = '';
	this.mimetype = '';
	this.ref ='';
	this.tags = new Array();
	this.url = '';
	this.video = {};
	this.xml = '';
	
	this.clone = function(){
		var newTrack = new Track();
		newTrack.audio = $.extend(true,{},self.audio);
		newTrack.checksum = $.extend(true,{},self.checksum);
		newTrack.duration = self.duration;
		newTrack.flavor = self.flavor;
		newTrack.id = self.id;
		newTrack.mimetype = self.mimetype;
		newTrack.ref = self.ref;
		newTrack.tags = cloneArray(self.tags);
		newTrack.url = self.url;
		newTrack.video = $.extend(true,{},self.video);
		newTrack.xml = self.xml;
		return newTrack;
	}
	
	return this;
};

/**
 * Attachment class
 */
Attachment = function(){	
	var self = this;
	
  this.checksum = {
      value: '',
      type: 'md5'
  };
	this.type = '';
	this.id = '';
	this.mimetype = '';
	this.ref ='';
	this.tags = new Array();
	this.url = '';
	this.xml = '';
	
	this.clone = function(){
		var newAttachment = new Attachment();
		newAttachment.checksum = $.extend(true,{},self.checksum);
		newAttachment.type = self.type;
		newAttachment.id = self.id;
		newAttachment.mimetype = self.mimetype;
		newAttachment.ref = self.ref;
		newAttachment.tags = cloneArray(self.tags);
		newAttachment.url = self.url;
		newAttachment.xml = self.xml;
		return newAttachment;
	}
	
	return this;
};

/**
 * Clone the given array (deep copy)
 * 
 * @param array the array to clone
 * @returns a clone of the given array
 */
function cloneArray(array){
	var newArray = new Array();
	$.each(array,function(index,value){
		var newValue = value;
		if(typeof(value) == 'object') {
			newValue = clone(value);
		} else if(typeof(value) == 'array') {
			newValue = cloneArray(value);
		}
		newArray.push(value);
	});
	return newArray;
}

/**
 * Clone function
 * @author Keith Devens
 * @see http://keithdevens.com/weblog/archive/2007/Jun/07/javascript.clone
 */
function clone(obj){
    if (obj == null || typeof(obj) != 'object') {
        return obj;
    } else if (obj instanceof Element) {
   		// Return a clone of an HTML eleemnt
    	return $(obj).clone()[0];
    }

    var temp = new obj.constructor(); 
    for(var key in obj)
        temp[key] = clone(obj[key]);

    return temp;
}

function xmlToString(doc) {
	  if(typeof XMLSerializer != 'undefined'){
	    var string = (new XMLSerializer()).serializeToString(doc);
	    return string.replace(/ xmlns=\"\"/g, '');
	  } else if(doc.xml) {
	    return doc.xml;
	  } else {
	    return '';
	  }
}

function createDoc(rootEl, rootNS, prefix) {
	var doc = null;
	//Create a DOM Document, methods vary between browsers, e.g. IE and Firefox
	if(document.implementation && document.implementation.createDocument) { //Firefox, Opera, Safari, Chrome, etc.
		if(checkValue(prefix))
			doc = document.implementation.createDocument(rootNS, prefix+":"+rootEl, null);
		else
			doc = document.implementation.createDocument(rootNS, rootEl, null);
		
	} else { // IE
		doc = new ActiveXObject('MSXML2.DOMDocument');
		if(checkValue(prefix))
			doc.loadXML('<'+prefix + ':' + rootEl + ' xmlns:'+prefix+'="' + rootNS + '"></' + prefix + ':' + rootEl + '>');
		else
			doc.loadXML('<' + rootEl + ' xmlns'+fullPrefix+'="' + rootNS + '"></' + rootEl + '>');
	}
	return doc;
};

function createCatalog(data) {
	var dc = createDoc('dublincore','http://www.opencastproject.org/xsd/1.0/dublincore/');
	dc.documentElement.setAttribute('xmlns:dcterms','http://purl.org/dc/terms/');
	dc.documentElement.setAttribute('xmlns:xsi','http://www.w3.org/2001/XMLSchema-instance');
	$.each(data, function(key, values) {
		if(values.length == 0) return;
		
		$.each(values, function(index,value){
			if(!checkValue(value.value))
				return;
			
			var elm = dc.createElement('dcterms:' + key);
			elm.appendChild(dc.createTextNode(value.value));
			if(checkValue(value.lang))
				elm.setAttribute('xml:lang',value.lang);
			if(checkValue(value.type))
				elm.setAttribute('xsi:type',value.type);
			dc.documentElement.appendChild(elm);
		});
	});
	return dc;
};

/**
 * Add a new text element to the given base node 
 * 
 * @param name Name of the new element to add
 * @param value Text of the new element
 * @param baseNode Node where the new element has to be added
 * @param xmlDocument XML Document 
 * @returns the new element
 */
function addTextElementToXmlDoc(name,value,baseNode,xmlDocument) {
	var element = addElementToXmlDoc(name, baseNode,xmlDocument);
	if(value==undefined||value==''||element == null)
		return null;
	element.appendChild(xmlDocument.createTextNode(value));
	
	return element;
};

/**
 * Add a new element to the given base node 
 * 
 * @param name Name of the new element to add
 * @param baseNode Node where the new element has to be added
 * @param xmlDocument XML Document 
 * @returns the new element
 */
function addElementToXmlDoc(name,baseNode,xmlDocument) {
	if(!checkValue(name)||!checkValue(baseNode)||!checkValue(xmlDocument))
		return null;
	var element = xmlDocument.createElement(name);
	baseNode.appendChild(element);
	return element;
};

function checkValue(variable){
	return variable!=null && variable!=undefined && variable!='';
}

