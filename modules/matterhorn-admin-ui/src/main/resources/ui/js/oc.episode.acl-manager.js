/**
 * @requires underscore.js for collection operation
 * @requires jquery-ui-timepicker-addon for datepicker with time feature
 */

/**
 * ACL Scheduler for the episodes
 * @type {Object}
 */
opencast.episode.aclScheduler = {

    /** Define if the aclScheduler has already been initialized */
    initialized: false,

    /** The current episode schedulers */
    episodeSchedulers: [],

    /** List of URL used by schedulers */
    URL: {
        WORKFLOWS: {
            GET:    "/workflow/definitions.json"
        },
        TRANSITIONS: {
            GET:        "/acl-manager/transitionsfor.json?done=false",
            PUT:        "/acl-manager/episode/",
            DELETE:     "/acl-manager/episode/",
            POST:       "/acl-manager/episode/"
        },
        ACL: {
          GET: "/acl-manager/acl/acls.json"
        },
        INSTANT_APPLY: "/acl-manager/apply/episode/"
    },

    /** Template for the episode schedules */
    scheduleTemplate: undefined,

    /** Template for the current acl  */
    currentTemplate: undefined,

    /** ACL id to go back to series ACL **/
    _seriesAclId: "_series",

    /**
     * Init function
     */ 
    init: function(){
        var self = this;

        // Loading schedules template
        $.ajax({
            dataType: "html",
            async: false,
            url: "templates/episodes-schedule.tpl",
            success: function(data){
                self.scheduleTemplate = data;
            }
        });

        // Loading current acl template
        $.ajax({
            dataType: "html",
            async: false,
            url: "templates/episodes-current-acl.tpl",
            success: function(data){
                self.currentTemplate = data;
            }
        });
        
        self.allSeries = [];
        self.allEpisodes = [];
        $('tr.episodeEl').each(function(index, element){
          if ($(element).find('input.seriesId').size() > 0) {
        	 self.allSeries.push($(element).find('input.seriesId').val());
          }
        	self.allEpisodes.push($(element).attr('id').substring(2));
        });

        self.aclTitle = {};
        self.episodes = {};
        self.series = {};
        self.getACL();
        self.getTransitions(undefined, false, true);

        // Initialize the workflow parameters window
        this.$workflowWindow          = $("#twf-window").dialog({autoOpen: false});
        this.$workflowConfigContainer = $("#twf-config-container");

        $("#twf-submit").click(function() {
          var newConfig = ocWorkflow.getConfiguration(self.$workflowConfigContainer);
          if(JSON.stringify(newConfig) != JSON.stringify(self.currentSchedule.workflowParams)){
            self.currentSchedule.workflowParams = newConfig;
            self.currentSchedule.toggleSaveStatus();
            self.currentSchedule.checkChanged();
          }
          self.$workflowWindow.dialog("close");
        });

        $("#twf-cancel").click(function() { 
          self.$workflowWindow.dialog("close"); 
          self.currentSchedule.setCurrentWorklowParams();
        });

        self.getACL();

        // Create all schedulers
        $('tr.episodeEl').each(function(index, element){
            self.episodeSchedulers.push(new opencast.episode.aclScheduler.EpisodeScheduler(element));
        });

        this.initialized = true;
    },

    /**
     * Call on each window refresh
     */
    refresh: function(){
        if(this.initialized)
            _.each(this.episodeSchedulers, function(scheduler){
                scheduler.refresh();
            });
        else
            this.init();
    },


    /** 
     *  Open the workflow window using the jquery.dialog plugin.
     *  @return the workflow window
     */
    openWorkflowWindow: function() {
      var self = opencast.episode.aclScheduler;
      self.$workflowWindow.dialog("option", {
        width: $(window).width() - 40,
        height: $(window).height() - 40,
        position: ["center", "center"],
        show: "scale",
        height: 250
      }).dialog("open");
      return self.$workflowWindow;
    },


    /**
     * Get all workflows definitions
     */
    getWorkflowDefinitions: function(){
        var self = this;

        if(this.workflowDefinitionsOptions)
            return this.workflowDefinitionsOptions;

        $.ajax({
          async: false,
          dataType: 'json',
          url: opencast.episode.aclScheduler.URL.WORKFLOWS.GET,
          success: function(data) {
            var options = "<option value=\"\">-- No workflow --</option>";

            $.each(data.definitions.definition,function(index,value){
                if(value.id != null && value.description != null)
                  options += "<option value=\""+value.id+"\">"+value.description+"</option>";
            });

            self.workflowDefinitionsOptions = options;
          },
          error: function(qXHR, textStatus, errorThrown){
            self.workflowDefinitionsOptions = "<option value=\"\">-- No workflow definitions --</option>";
          }
        });

        return self.workflowDefinitionsOptions;
    },

    /**
     * Change the content of the wofklow configuration window
     * @param  {jQuery Element} data Worfklow configuration panel
     */
    prepareWorkflowParamsUI: function(data) {
      var self = opencast.episode.aclScheduler;

      self.$workflowConfigContainer.detach();
      self.$workflowWindow.prepend(data);
      self.$workflowConfigContainer = self.$workflowWindow.find("#twf-config-container");
    },

    /**
     * Get the configuration panel for the given worfklow definition
     * @param  {String} wfId The workflow definition Id
     * @return {jQuery Element} The configuration panel 
     */
    getWorkflowConfigurationEl: function (wfId) {
      var self = opencast.episode.aclScheduler,
          $workflowConfiguration;
          
      $.ajax({
        url     : '/workflow/configurationPanel?definitionId=' + wfId,
        async   : false,
        success : function (data) {
          $workflowConfiguration = $(data).wrap('<div id="twf-config-container"></div>');
        }
      });

      return $workflowConfiguration.parent();
    },

    /**
     * Get all ACL
     */
    getACL: function(hasSeries, isFromSeries){
        var self         = this;
        var backToSeries = "";

        if (hasSeries && !isFromSeries) {
          backToSeries ="<option value=\""+self._seriesAclId+"\">-> back to series ACL</option>";
        } else if(self.aclOptions == "") {
          backToSeries = "<option value=\"\">-- No ACL available --</option>";
        }

        if(self.aclOptions)
            return self.aclOptions + backToSeries;
          

        $.ajax({
          async: false,
          dataType: 'json',
          url: opencast.episode.aclScheduler.URL.ACL.GET,

          success: function(data) {
            var options = "";

            $.each(ocUtils.ensureArray(data), function(index,value){
                if(value.id != null && value.name != null){
                  options += "<option value=\""+value.id+"\">"+value.name+"</option>"
                  self.aclTitle[value.id] = value.name;
                }
            });

            self.aclTitle["_series"] = "-> back to series ACL";

            self.aclOptions = options;
          },
          error: function(qXHR, textStatus, errorThrown){
            self.aclOptions = "<option value=\"\">-- No ACL available --</option>";
          }
        });

        return self.aclOptions + backToSeries;
    },

    /**
     * Get the transitions for the given id, or all if not given
     * @param  {Boolean} isSeries  Define if the transitions are related to a series or an episodes
     * @param  {Integer} id      the series id, get all if undefined
     * @param  {Boolean} refresh  Define if it has to refresh data from server
     * @return {JSON Object}         the series JSON objects
     */
    getTransitions: function(id, isSeries, refresh){
        var self = this;

        if (self.allSeries.length == 0 && self.allEpisodes.length == 0) {
          return undefined
        }

        var sources = (isSeries ? self.series : self.episodes);

        // If series in cache and refresh is false
        if(!refresh)
          return sources[id];
        
        var url = self.URL.TRANSITIONS.GET + "&episodeIds=" + self.allEpisodes.toString();

        if (self.allSeries.length > 0) {
          url = url + "&seriesIds=" + self.allSeries.toString();
        }
        
        $.ajax({
          async: false,
          dataType: 'json',
          url: url,
          success: function(data) {
            if(data.series){
              self.series = data.series;
              self.episodes = data.episodes;
              sources = (isSeries ? self.series : self.episodes)
              return sources[id];
            }
            else{
              return undefined;   
            }
          },
          error: function(qXHR, textStatus, errorThrown){
            console.warn("Cannot get transitions from series ");
            return undefined;
          }
        });
    }

}


/**
 * @class EpisodeScheduler description
 * 
 * @param {DOMElement} element 
 */
opencast.episode.aclScheduler.EpisodeScheduler = function(element){

    var self = this;

    /**
     * Toogle the visibility of the scheduler between hidden and displayed
     * @param  {Boolean} collapse Define if the scheduler schould be or not displayed, 
     *                            If nothing is given, it will simply toggle to the other visibility status.
     * @return {EpisodeSchedule} return this scheduler object
     */
    self.toggle = function(collapse){

        if(collapse){
            self.$container.hide();
            self.$toggleButton.addClass('ui-icon-triangle-1-e');
            self.$toggleButton.removeClass('ui-icon-triangle-1-s');
        }
        else{
            self.$container.show();
            self.$toggleButton.addClass('ui-icon-triangle-1-s');
            self.$toggleButton.removeClass('ui-icon-triangle-1-e');
            $("#refreshEnabled").attr('checked', false);
            $("#refreshEnabled").change();
        }

        return self;
    }

    /**
     * Insert a new episode schedule
     * @return {EpisodeSchedule} return this scheduler object
     */
    self.insertSchedule = function(arg,isSeries){
        var data;

        // if no data are given, or if arg is an event object
        if(!arg || arg.target)
          data = {id: self.id+"-SC-"+self.schedules.length};
        else
          data = arg;


        var newSchedule = new opencast.episode.aclScheduler.EpisodeSchedule(data,self,isSeries);
        self.schedules.push(newSchedule);
        self.drawSchedules();

        return self;
    }

    /**
     * Register all the listener related to this scheduler element
     * @return {EpisodeSchedule} return this schedule robject
     */
    self.registerListeners = function(){
        self.$toggleButton.click(function(){
            self.toggle(self.isCollapsed = !self.isCollapsed);
        });

        self.$container.find('button.add').click(self.insertSchedule);
    }

    /**
     * Replace the current DOM representation of this scheduler with the given element
     * @param {DOMElement} newElement the new scheduler representation
     * @return {EpisodeScheduler} return this scheduler object
     */
    self.setElement = function(newElement){
        self.$el            = $(newElement);
        self.$toggleButton  = self.$el.find('div.toggle-scheduler');
        self.$container     = self.$el.next();
        self.$schedulesContainer = self.$container.find(".schedules-container");
    }

    /**
     * Function for Underscore sortBy methods to sort all schedules by Date
     * @param  {EpisodeSchedule} schedule 
     */
    self.sortSchedules = function(schedule){
        return new Date(schedule.fromDate).getTime();
    }

    self.drawSchedules = function(){
        var listToRemove = [];
        self.$schedulesContainer.empty();

        self.$schedulesContainer.append(self.activeAcl.element.render().$el);
        self.activeAcl.element.setElement(self.$schedulesContainer.find('.current')).registerListeners();

        self.schedules = _.sortBy(self.schedules, self.sortSchedules);
        $.each(self.schedules, function(index, schedule){
            schedule.render();

            if(!schedule.toDelete)
              self.$schedulesContainer.append(schedule.$el);
            else
              listToRemove.push(index);

            schedule.setElement(self.$schedulesContainer.find('> div:last'))
                    .registerListeners();
        });

        $.each(listToRemove,function(index,value){
          self.schedules.splice(value,1);
        });
    }

    self.removeSchedule = function(schedule){
        self.schedules.splice(_.indexOf(self.schedules,schedule),1);
        self.drawSchedules();
    }

    self.getTransitions = function(){
        if(self.data = opencast.episode.aclScheduler.getTransitions(self.id)){

           if(self.data.activeAcl && self.data.activeAcl.managedAcl) {
        	   self.activeAcl         = self.data.activeAcl.managedAcl;
        	   self.activeAcl.managed = true;       
             self.activeAcl.isFromSeries = self.data.activeAcl.managedAcl.isFromSeries; 	   
           } else if(self.data.activeAcl && self.data.activeAcl.unmanagedAcl) {
        	   self.activeAcl.name  = "Unmanaged ACL";
        	   self.activeAcl.managed = false;        	   
           }

           self.activeAcl.element = new opencast.episode.aclScheduler.CurrentAcl(self.activeAcl, self);

           $.each(ocUtils.ensureArray(self.data.transitions),function(index,transition){
              self.insertSchedule(transition,false);
           });             
        }

        if(self.schedules.length == 0)
              self.drawSchedules();   
    }

    self.getSeriesTransitions = function(){
        if(!self.seriesId)
          return;

        if(self.seriesData = opencast.episode.aclScheduler.getTransitions(self.seriesId, true)){
 
              $.each(ocUtils.ensureArray(self.seriesData.transitions),function(index,transition){
                if(transition.override)
                  self.insertSchedule(transition,true);
              });             
            
        }
        else{
          console.warn("Cannot get transitions from series "+self.id);
        }     
    }   

    /**
     * Call on each window refresh
     */
    self.refresh = function(){
        self.setElement($('#'+element.id)[0]);

        self.drawSchedules();

        self.toggle(self.isCollapsed);
        self.registerListeners();
    }

    /**
     * Init function
     */
    self.init = function(){
        // Initialize variable
        self.isCollapsed    = true;

        self.setElement(element);

        self.schedules  = [];
        self.activeAcl  = {
          name: "Unmanaged ACL"
        };
        self.activeAcl.element = new opencast.episode.aclScheduler.CurrentAcl(self.activeAcl,self);
        self.id         = self.$el.attr("id").substring(2);
        self.seriesId   = self.$el.find(".seriesId").val();

        self.getTransitions();
        self.getSeriesTransitions();

        self.registerListeners();

        return self;
    }

    return this.init();
}

/**
 * @class EpisodeSchedule description
 * @param {DOMElement} element 
 */
opencast.episode.aclScheduler.EpisodeSchedule = function(value, episodeScheduler, isSeries){

    var self = this;

    self.toggleNewStatus = function(isNew){
      self.isNew = isNew;

      if(isNew)
        self.$el.find('.delete').hide();
      else
        self.$el.find('.delete').show();
    };

    self.toggleSaveStatus = function(saved){
        self.saved = saved;

        if(saved){
            self.$el.removeClass("toSave");
            self.$saveBtn.hide();
            self.$cancelBtn.hide();
        }
        else{
            self.$el.addClass("toSave");
            self.$saveBtn.show();
            self.$cancelBtn.show();
        }

        return self;
    };

    self.toggleLoadingStatus = function(loading){
        self.loading = loading;

        if (loading) {
            self.toggleSaveStatus(true);
            self.$el.addClass("loading-acl");
        } else {
            self.$el.removeClass("loading-acl");
        }

        return self;
    };

    /**
    * Generate the date pickers  
    * @return {EpisodeSchedule} return this schedule object
    */
    self.createDatePickers = function(){
        if(!self.fromDate)
            self.fromDate = new Date();

        self.$from = self.$el.find('.from');

        self.$from.datetimepicker({ 
          showOn: "both",
          buttonImage: "/img/icons/calendar.gif",
          buttonImageOnly: false,
          minDate: 0,
          onClose: function() {
            self.fromDate = new Date(self.$from.val());
            self.toggleSaveStatus(false);
            self.checkChanged();

            if(self.isCurrent && self.fromDate > new Date())
              self.episodeScheduler.currentSchedule = undefined;

            self.displayError();
            self.episodeScheduler.drawSchedules();
          }
        }).datetimepicker("setDate",self.fromDate);

        return self;
    };

    /**
     * Replace the current DOM representation of this schedule with the given element
     * @param {DOMElement} newElement the new schedule representation
     * @return {EpisodeSchedule} return this schedule object
     */
    self.setElement = function(newElement){
        self.$el        = $(newElement);
        self.$saveBtn   = self.$el.find('.save');
        self.$cancelBtn = self.$el.find('.cancel');

        return self;
    }

    /**
     * Listener for the changes happening on the wokflow selector
     * @param  {DOMEvent} event 
     */
    self.changeWorkflowListener = function(event){
        if(event.target.value == self.workflowId)
            return;

        self.workflowId = event.target.value;
        self.toggleSaveStatus(false);

        if(event.target.value == self.savedParams.workflowId) {
          self.workflowParams = self.savedParams.workflowParams;
          self.$workflowConfiguration = self.savedParams.$workflowConfiguration;
        } else {
          self.workflowParams = undefined;
          self.$workflowConfiguration = opencast.episode.aclScheduler.getWorkflowConfigurationEl(self.workflowId);
        }
        self.checkChanged();
    }

    /**
     * Listener for the changes happening on the acl selector
     * @param  {DOMEvent} event 
     */
    self.changeAclListener = function(event){
        if(event.target.value == self.aclId)
            return;

        if(event.target.value == "undefined") {
        	self.aclId = undefined;
        } else {
        	self.aclId = event.target.value;
        }
        self.toggleSaveStatus(false);
        self.checkChanged();
    }
    
    self.checkChanged = function() {
    	var changed = false;
    	changed = changed || (self.aclId != self.savedParams.aclId);
    	changed = changed || (self.fromDate.getTime() != self.savedParams.fromDate.getTime());
    	changed = changed || (self.workflowId != self.savedParams.workflowId);
    	changed = changed || !_.isEqual(self.workflowParams, self.savedParams.workflowParams);
        if(!changed)
        	self.cancel();
    }

    self.setCurrentWorklowParams = function(){
          var $el;

          self.$workflowConfiguration.find("input[type='checkbox']").removeAttr("checked");
          
          if(self.workflowParams){
            $.each(self.workflowParams,function(key, value){
              $el = self.$workflowConfiguration.find("#"+key);

              if($el.is("input") && $el.attr("type")=="checkbox") {
                if (!_.isUndefined(value)) {
                  $el.attr("checked","checked");
                } else {
                  $el.removeAttr("checked");                  
                }
              } else {
                $el.val(value);
              }
            });
          }
    }

    /**
     * Listener to open the workflow configuration panel 
     * @param  {DOMEvent} event
     */
    self.configWorkflowsListener = function(event){
      if(self.workflowId == undefined || self.workflowId == null)
        return;

      var $configContainer = opencast.episode.aclScheduler.$workflowConfigContainer;

      opencast.episode.aclScheduler.currentSchedule = self;
      opencast.episode.aclScheduler.prepareWorkflowParamsUI(self.$workflowConfiguration);
      opencast.episode.aclScheduler.openWorkflowWindow();
      self.setCurrentWorklowParams();
    }

    /**
     * Register all the listener related to this schedule element
     * @return {EpisodeSchedule} return this schedule object
     */
    self.registerListeners = function(){
        self.$el.find('select.workflows').bind('change select',self.changeWorkflowListener);
        self.$el.find('select.acl').bind('change select',self.changeAclListener);
        self.$el.find('.delete').bind('click',self.destroy);
        self.$el.find('.save').bind('click',self.save);
        self.$el.find('.cancel').bind('click',self.cancel);
        self.$el.find('.params').bind('click',self.configWorkflowsListener);

        return self;
    }

    /**
     * Draw the current element
     * @return {EpisodeSchedule} return this schedule object
     */
    self.render = function(){
        var params = {
            workflows: opencast.episode.aclScheduler.getWorkflowDefinitions(),
            acl      : opencast.episode.aclScheduler.getACL(!_.isUndefined(self.episodeScheduler.seriesId), false),
            id       : self.id
        };

        self.setElement($.jqote(opencast.episode.aclScheduler.scheduleTemplate,params));

        if(!self.aclId){
          if(self.isNew)
            self.aclId = self.$el.find('select.acl').val();
          else
            self.aclId = opencast.episode.aclScheduler._seriesAclId;
        }

        if(self.workflowId)
            self.$el.find('.workflows option[value="'+self.workflowId+'"]').attr("selected", "selected");
        if(self.aclId)
            self.$el.find('.acl option[value="'+self.aclId+'"]').attr("selected", "selected");

        if(self.isSeries){
          self.$el.find('input, select').attr("disabled","disabled");
          self.$el.addClass('seriesSchedule');
          self.$el.find('.aclLabel').html(opencast.episode.aclScheduler.aclTitle[self.aclId]);
          self.$el.find('.workflowsLabel').html(self.$el.find(".workflows option:selected").html());
        }

        self.toggleNewStatus(self.isNew);
        self.toggleSaveStatus(self.saved);

        return self.createDatePickers().displayError(self.error);
    }

    self.displayError = function(error){
      if(error)
        self.$el.find('span.error').html(error);
      else
        self.$el.find('span.error').empty();
      self.error = error;
    }

    self.cancel = function(){
      if(self.isNew){
          self.episodeScheduler.removeSchedule(self);
          delete self;
      }
      else{
        self.aclId          = self.savedParams.aclId;
        self.workflowId     = self.savedParams.workflowId;
        self.fromDate       = self.savedParams.fromDate;
        self.workflowParams = self.savedParams.workflowParams
        self.$workflowConfiguration = self.savedParams.$workflowConfiguration;
      }

      self.toggleSaveStatus(true);
      self.displayError();
      self.episodeScheduler.drawSchedules();
    }

    /**
     * Save this schedule
     */
    self.save = function(){
        var url, type, data;

        if(self.isNew){
          url   = opencast.episode.aclScheduler.URL.TRANSITIONS.POST+self.episodeScheduler.id;
          type  = "POST";
        }
        else{
          url   = opencast.episode.aclScheduler.URL.TRANSITIONS.PUT+self.id;
          type  = "PUT";
        }
        

        data = {
          applicationDate: ocUtils.toISODate(new Date(self.fromDate))
        }
        

        if(self.aclId && self.aclId != opencast.episode.aclScheduler._seriesAclId && self.aclId != ""){
          data["managedAclId"]  = self.aclId;
        }
        else if(!(self.aclId == opencast.episode.aclScheduler._seriesAclId)){
          self.displayError("Can not save a transition without ACL!");
          return;
        }

        if(self.workflowId){
          data["workflowDefinitionId"] = self.workflowId;
          self.setCurrentWorklowParams();
        }
        
        if(_.isEmpty(self.workflowParams)) {
          self.workflowParams = undefined;
        }

        if(self.workflowParams) 
          data["workflowParams"] = JSON.stringify(self.workflowParams);

        self.toggleLoadingStatus(true);

        $.ajax({
          async: true,
          url: url,
          data: data,
          type: type,
          dataType: 'json',
          success: function(data) {
            if(self.isNew){
              var oldId = self.id;
              self.id   = data.transitionId;             
              self.$el.attr("id",self.id);
              self.isNew = false;
              self.toggleNewStatus(false);
            }
            self.savedParams.aclId                  = self.aclId;
            self.savedParams.workflowId             = self.workflowId;
            self.savedParams.fromDate               = self.fromDate;
            self.savedParams.workflowParams         = self.workflowParams;
            self.savedParams.$workflowConfiguration = self.workflowConfiguration;

            self.toggleLoadingStatus(false);

            self.displayError();

            if(self.aclId)
              self.$el.find('.acl option[value="undefined"]').remove();

          },
          error: function(qXHR, textStatus, errorThrown){
            switch(qXHR.status){
              case 400:
                self.displayError("No able to save this transition, workflow definition or acl is incorrect!");
                break;
              case 409:
                self.displayError("There is already a transition with this start date!");
                break;
              default: 
                self.displayError("Not able to save the transition");
                break;
            }
          }
        });

        return self;
    }

    /**
     * Destroy this schedule 
     */
    self.destroy = function(){

        if(!self.isNew){
          $.ajax({
            async: false,
            url: opencast.episode.aclScheduler.URL.TRANSITIONS.PUT+self.id,
            type: "DELETE",
            dataType: 'json',
            success: function() {
              console.log("transition deleted");
              self.episodeScheduler.removeSchedule(self);
              delete self;
            },
            error: function(qXHR, textStatus, errorThrown){
              self.displayError("Not able to destroy the transition");
            }
          });
        } 
        
    }

    /**
     * Init function
     * @return {EpisodeSchedule} return this schedule object
     */
    self.init = function(){
        self.id    = value.transitionId;

        if(isSeries)
          self.isSeries = true;

        if(value.workflowId)
          self.workflowId = value.workflowId;

        if(value.workflowParams)
          self.workflowParams = JSON.parse(value.workflowParams);

        if(value.acl && value.acl.id)
          self.aclId = value.acl.id;

        if(value.applicationDate){
          self.fromDate = new Date(value.applicationDate);
        }
        else{
          var actualDate = new Date(); 
          self.fromDate = new Date(actualDate.getFullYear(), actualDate.getMonth(), actualDate.getDate()+1); 
        }

        self.isNew = ((value.applicationDate || value.isSeries) ? false : true);
        self.saved = ((value.applicationDate || value.isSeries) ? true  : false);
        
        if (_.isUndefined(self.workflowId)) {
        	self.workflowId = "";
        } else {
          self.$workflowConfiguration = opencast.episode.aclScheduler.getWorkflowConfigurationEl(self.workflowId);
          if (_.isUndefined(value.workflowParams)) {
            self.workflowParams = ocWorkflow.getConfiguration(self.$workflowConfiguration);
          }
          self.setCurrentWorklowParams();
        }

        self.savedParams = {
          aclId                  : self.aclId,
          workflowId             : self.workflowId,
          fromDate               : self.fromDate,
          workflowParams         : self.workflowParams,
          $workflowConfiguration : self.$workflowConfiguration
        }

        self.episodeScheduler = episodeScheduler;
        return self;
    }

    return self.init().render();
}

/**
 * @class CurrentAcl 
 * @param {Value} element 
 */
opencast.episode.aclScheduler.CurrentAcl = function(value, episodeScheduler){

    var self = this;

    self.toggleSaveStatus = function(saved){
        self.saved = saved;

        if(saved){
            self.$el.removeClass("toSave");
            self.$saveBtn.hide();
            self.$cancelBtn.hide();
        }
        else{
            self.$el.addClass("toSave");
            self.$saveBtn.show();
            self.$cancelBtn.show();
        }

        return self;
    };

    self.toggleLoadingStatus = function(loading){
        self.loading = loading;

        if (loading) {
            self.toggleSaveStatus(true);
            self.$el.addClass("loading-acl");
        } else {
            self.$el.removeClass("loading-acl");
        }

        return self;
    };

    /**
     * Replace the current DOM representation of this schedule with the given element
     * @param {DOMElement} newElement the new schedule representation
     * @return {EpisodeSchedule} return this schedule object
     */
    self.setElement = function(newElement){
        self.$el        = $(newElement);
        self.$saveBtn   = self.$el.find('.save');
        self.$cancelBtn = self.$el.find('.cancel');

        return self;
    }

    /**
     * Listener for the changes happening on the wokflow selector
     * @param  {DOMEvent} event 
     */
    self.changeWorkflowListener = function(event){
        if(event.target.value == self.workflowId)
            return;

        self.workflowId = event.target.value;
        self.toggleSaveStatus(false);
    }

    /**
     * Listener for the changes happening on the acl selector
     * @param  {DOMEvent} event 
     */
    self.changeAclListener = function(event){
        if(event.target.value == self.aclId)
            return;

        if(event.target.value == "undefined") {
        	self.aclId = undefined;
        } else {
        	self.aclId = event.target.value;
        }
        self.toggleSaveStatus(false);
        self.checkChanged();
    }
    
    self.checkChanged = function() {
    	var changed = false;
    	changed = changed || (self.aclId != self.savedParams.aclId);
        if(!changed)
        	self.cancel();
    }

    /**
     * Register all the listener related to this schedule element
     * @return {EpisodeSchedule} return this schedule object
     */
    self.registerListeners = function(){
        self.$el.find('select.acl').bind('change select',self.changeAclListener);
        self.$el.find('.cancel').bind('click',self.cancel);
        self.$el.find('.save').bind('click',self.apply);
        return self;
    }

    /**
     * Draw the current element
     * @return {EpisodeSchedule} return this schedule object
     */
    self.render = function(){


        var params = {
            workflows: opencast.episode.aclScheduler.getWorkflowDefinitions(),
            acl      : opencast.episode.aclScheduler.getACL(!_.isUndefined(self.episodeScheduler.seriesId), self.episodeScheduler.activeAcl.isFromSeries),
            id       : self.id
        };

        if(!self.aclId)
          params.acl = "<option value=\"" + self.aclId + "\">" + self.aclName + "</option>" + params.acl;

        self.setElement($.jqote(opencast.episode.aclScheduler.currentTemplate,params));

        if(self.workflowId)
            self.$el.find('.workflows option[value="'+self.workflowId+'"]').attr("selected", "selected");
        if(self.aclId)
            self.$el.find('.acl option[value="'+self.aclId+'"]').attr("selected", "selected");

        self.toggleSaveStatus(self.saved);

        return self.displayError(self.error);
    }

    self.displayError = function(error){
      if(error)
        self.$el.find('span.error').html(error);
      else
        self.$el.find('span.error').empty();
      
      self.error = error;

      return self;
    }

    self.cancel = function(){
      self.aclId          = self.savedParams.aclId;
      self.toggleSaveStatus(true);
      self.displayError();
      self.episodeScheduler.drawSchedules();
    }

    /**
     * Save this schedule
     */
    self.apply = function(){
        var url, type, data;

        url   = opencast.episode.aclScheduler.URL.INSTANT_APPLY + self.episodeScheduler.id;
        type  = "POST";

        data = {};
        
        if (self.aclId && self.aclId != opencast.episode.aclScheduler._seriesAclId && self.aclId != "") {
          data["aclId"]  = self.aclId;
        } else if(!(self.aclId == opencast.episode.aclScheduler._seriesAclId)) {
          self.displayError("Can not apply a transition without ACL!");
          return;
        }

        self.toggleLoadingStatus(true);

        $.ajax({
          async: true,
          url: url,
          data: data,
          type: type,
          dataType: 'json',
          success: function(data) {

            if (self.savedParams.aclId !== self.aclId) {
              if (self.episodeScheduler.activeAcl.isFromSeries) {
                self.episodeScheduler.activeAcl.isFromSeries = false;
              }else if (self.aclId == opencast.episode.aclScheduler._seriesAclId) {
                self.episodeScheduler.activeAcl.isFromSeries = true;
              }
            }

            self.savedParams.aclId  = self.aclId;

            self.displayError();

            if (self.aclId) {
              self.$el.find('.acl option[value="undefined"]').remove();
            }

            self.toggleLoadingStatus(false);
            self.episodeScheduler.drawSchedules();

          },
          error: function(qXHR, textStatus, errorThrown){
            switch(qXHR.status){
              case 409:
                self.displayError("There is already a transition with this start date!");
                break;
              default: 
                self.displayError("Not able to apply the transition");
                break;
            }
          }
        });

        return self;
    }

    /**
     * Init function
     * @return {EpisodeSchedule} return this schedule object
     */
    self.init = function(){
        self.id    = "currentACL"
        self.aclId   = value.id;
        self.aclName = value.name;
        self.saved = true;
        
        self.managed = value.managed;

        self.savedParams = {
          aclId           : self.aclId,
        }

        self.episodeScheduler = episodeScheduler;
        return self;
    }

    return self.init().render();
}
