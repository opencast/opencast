/**
 * @requires underscore.js for collection operation
 * @requires jquery-ui-timepicker-addon for datepicker with time feature
 */

if(!opencast.series)
  opencast.series = {};


/**
 * ACL Scheduler for the series
 * @type {Object}
 */
opencast.series.aclScheduler = {

    /** Define if the aclScheduler has already been initialized */
    initialized: false,

    /** The current series schedulers */
    seriesSchedulers: [],

    /** List of URL used by schedulers */
    URL: {
        WORKFLOWS: {
            GET:    "/workflow/definitions.json"
        },
        TRANSITIONS: {
            GET:    "/acl-manager/transitionsfor.json?done=false",
            PUT:    "/acl-manager/series/",
            DELETE: "/acl-manager/series/",
            POST:   "/acl-manager/series/"
        },
        ACL: {
          GET: "/acl-manager/acl/acls.json"
        },
        INSTANT_APPLY: "/acl-manager/apply/series/"
    },

    /** Template for the series schedules */
    scheduleTemplate: undefined,

    /** Template for the current acl  */
    currentTemplate: undefined,

    /**
     * Init function
     */ 
    init: function(){
        var self = this;

        // Loading scheduels template
        $.ajax({
            dataType: "html",
            async: false,
            url: "templates/series-schedule.tpl",
            success: function(data){
                self.scheduleTemplate = data;
            }
        });

        // Loading current acl template
        $.ajax({
            dataType: "html",
            async: false,
            url: "templates/series-current-acl.tpl",
            success: function(data){
                self.currentTemplate = data;
            }
        });
        
        self.allSeries = [];
        $('tr.seriesEl').each(function(index, element){
        	self.allSeries.push($(element).attr('id').substring(2));
        });
        
        self.aclTitle = {};
        self.series = {};
        self.getACL();
        self.getTransitions(undefined, true);

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

        // Create all schedulers
        $('tr.seriesEl').each(function(index, element){
            self.seriesSchedulers
      .push(new opencast.series.aclScheduler.SeriesScheduler(element));
        });


        this.initialized = true;
    },

    /**
     * Call on each window refresh
     */
    refresh: function(){
        if(this.initialized)
            _.each(this.seriesSchedulers
        , function(scheduler){
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
      var self = opencast.series.aclScheduler;
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
          url: opencast.series.aclScheduler.URL.WORKFLOWS.GET,
          success: function(data) {
            var options = "<option value=\"\">-- No workflow --</option>";

            $.each(data.definitions.definition,function(index,value){
                if(value.id != null && value.description != null)
                  options += "<option value=\""+value.id+"\">"+value.description+"</option>"
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
      var self = opencast.series.aclScheduler;

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
      var self = opencast.series.aclScheduler,
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
    getACL: function(){
        var self = this;

        if(self.aclOptions)
            return self.aclOptions;

        $.ajax({
          async: false,
          dataType: 'json',
          url: opencast.series.aclScheduler.URL.ACL.GET,

          success: function(data) {
            var options = "";

            $.each(ocUtils.ensureArray(data),function(index,value){
                if(value.id != null && value.name != null){
                  options += "<option value=\""+value.id+"\">"+value.name+"</option>"
                  self.aclTitle[value.id] = value.name;
                }
            });

            if(options == "")
              options = "<option value=\"\">-- No ACL available --</option>";

            self.aclOptions = options;
          },
          error: function(qXHR, textStatus, errorThrown){
            self.aclOptions = "<option value=\"\">-- No ACL available --</option>";
          }
        });

        return self.aclOptions;
    },

    /**
     * Get the transitions for the given id, or all if not given
     * @param  {Integer} id      the series id, get all if undefined
     * @param  {Boolean} refresh  Define if it has to refresh data from server
     * @return {JSON Object}         the series JSON objects
     */
    getTransitions: function(id, refresh){
        var self = this;

        if (self.allSeries.length == 0) {
          return undefined;
        }

        // If series in cache and refresh is false
        if(!refresh)
          return self.series[id];

        var url = self.URL.TRANSITIONS.GET + "&seriesIds=" + self.allSeries.toString();
        
        $.ajax({
          async: false,
          dataType: 'json',
          url: url,
          success: function(data) {
            if(data.series){
              self.series = data.series;
              return self.series[id];
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
 * @class SeriesScheduler description
 * 
 * @param {DOMElement} element 
 */
opencast.series.aclScheduler.SeriesScheduler = function(element){

    var self = this;

    /**
     * Toogle the visibility of the scheduler between hidden and displayed
     * @param  {Boolean} collapse Define if the scheduler schould be or not displayed, 
     *                            If nothing is given, it will simply toggle to the other visibility status.
     * @return {SeriesSchedule} return this scheduler object
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
        }

        return self;
    }

    /**
     * Insert a new series schedule
     * @return {SeriesSchedule} return this scheduler object
     */
    self.insertSchedule = function(arg){
        var data;

        // if no data are given, or if arg is an event object
        if(!arg || arg.target)
          data = {id: self.id+"-SC-"+self.schedules.length};
        else
          data = arg;


        var newSchedule = new opencast.series.aclScheduler.SeriesSchedule(data,self);
        self.schedules.push(newSchedule);
        self.drawSchedules();

        return self;
    }

    /**
     * Register all the listener related to this scheduler element
     * @return {SeriesSchedule} return this schedule robject
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
     * @return {SeriesScheduler} return this scheduler object
     */
    self.setElement = function(newElement){
        self.$el            = $(newElement);
        self.$toggleButton  = self.$el.find('div.toggle-scheduler');
        self.$container     = self.$el.next();
        self.$schedulesContainer = self.$container.find(".schedules-container");
    }

    /**
     * Function for Underscore sortBy methods to sort all schedules by Date
     * @param  {SeriesSchedule} schedule 
     */
    self.sortSchedules = function(schedule){
        return new Date(schedule.fromDate).getTime();
    }

    /**
     * Check if the ACL schedule is the one currently used
     * @param  {SeriesSchedule} schedule the schedule to test
     * @param  {Date} limit    the time limit
     * @return {Boolean}          true if the given schedule is currently used.
     */
    self.checkCurrentSchedule = function(schedule, limit){
      if(!limit)
        var limit = new Date();

      if((!self.currentSchedule || self.currentSchedule.fromDate <= schedule.fromDate) && 
          schedule.fromDate < limit){
        self.currentSchedule = schedule;
        return true
      }
      else{
        return false;
      }
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
        if(self.data = opencast.series.aclScheduler.getTransitions(self.id)) {

             if(self.data.activeAcl && self.data.activeAcl.managedAcl) {
            	 self.activeAcl     = self.data.activeAcl.managedAcl;
            	 self.activeAcl.managed = true;
             } else if(self.data.activeAcl && self.data.activeAcl.unmanagedAcl) {
            	 self.activeAcl.name  = "Unmanaged ACL";
            	 self.activeAcl.managed = false;
             }
             self.activeAcl.element = new opencast.series.aclScheduler.CurrentAcl(self.activeAcl, self);  

              $.each(ocUtils.ensureArray(self.data.transitions),function(index,transition){
                  self.insertSchedule(transition);
              });           
        }
        
        if(self.schedules.length == 0)
            self.drawSchedules();   
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

        self.activeAcl.element = new opencast.series.aclScheduler.CurrentAcl(self.activeAcl,self);
        self.id         = self.$el.attr("id").substring(2);

        self.getTransitions();

        self.registerListeners();

        return self;
    }

    return this.init();
}

/**
 * @class SeriesSchedule description
 * @param {DOMElement} element 
 */
opencast.series.aclScheduler.SeriesSchedule = function(value, seriesScheduler){

    var self = this;

    self.toggleNewStatus = function(isNew){
      self.isNew = isNew;

      if(isNew)
        self.$el.find('.delete').hide();
      else
        self.$el.find('.delete').show();

      return self;
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
    * @return {SeriesSchedule} return this schedule object
    */
    self.createDatePickers = function(){
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
              self.seriesScheduler.currentSchedule = undefined;

            self.displayError();
            self.seriesScheduler.drawSchedules();
          }
        }).datetimepicker("setDate",self.fromDate);

        return self;
    };

    /**
     * Replace the current DOM representation of this schedule with the given element
     * @param {DOMElement} newElement the new schedule representation
     * @return {SeriesSchedule} return this schedule object
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
          self.$workflowConfiguration = opencast.series.aclScheduler.getWorkflowConfigurationEl(self.workflowId);
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
    	changed = changed || (self.override != self.savedParams.override);
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
          };

    }

    /**
     * Listener to open the workflow configuration panel 
     * @param  {DOMEvent} event
     */
    self.configWorkflowsListener = function(event){
      if(self.workflowId == undefined || self.workflowId == null)
        return;

      var $configContainer = opencast.series.aclScheduler.$workflowConfigContainer;

      opencast.series.aclScheduler.currentSchedule = self;
      opencast.series.aclScheduler.prepareWorkflowParamsUI(self.$workflowConfiguration);
      opencast.series.aclScheduler.openWorkflowWindow();
      self.setCurrentWorklowParams();
    }


    /**
     * Register all the listener related to this schedule element
     * @return {SeriesSchedule} return this schedule object
     */
    self.registerListeners = function(){
        self.$el.find('select.workflows').bind('change select',self.changeWorkflowListener);
        self.$el.find('select.acl').bind('change select',self.changeAclListener);
        self.$el.find('.delete').bind('click',self.destroy);
        self.$el.find('.save').bind('click',self.save);
        self.$el.find('.cancel').bind('click',self.cancel);
        self.$el.find('.params').bind('click',self.configWorkflowsListener);
        self.$el.find('.override').bind('click',function(event){
          self.override = ($(event.target).attr("checked") == "checked");
          self.toggleSaveStatus(false);
          self.checkChanged();
        });

        return self;
    }

    /**
     * Draw the current element
     * @return {SeriesSchedule} return this schedule object
     */
    self.render = function(){
        var params = {
            workflows: opencast.series.aclScheduler.getWorkflowDefinitions(),
            acl      : opencast.series.aclScheduler.getACL(),
            id       : self.id
        };

        self.setElement($.jqote(opencast.series.aclScheduler.scheduleTemplate,params));

        if(!self.aclId){
          if(self.isNew)
            self.aclId = self.$el.find('select.acl').val();
          else
            self.aclId = "_series";
        }

        if(self.workflowId)
            self.$el.find('.workflows option[value="'+self.workflowId+'"]').attr("selected", "selected");
        if(self.aclId)
            self.$el.find('.acl option[value="'+self.aclId+'"]').attr("selected", "selected");
        if(self.override)
          self.$el.find('.override').attr("checked","checked");

        self.toggleSaveStatus(self.saved);
        self.toggleNewStatus(self.isNew);

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
          self.seriesScheduler.removeSchedule(self);
          delete self;
      }
      else{
        self.aclId          = self.savedParams.aclId;
        self.workflowId     = self.savedParams.workflowId;
        self.fromDate       = self.savedParams.fromDate;
        self.workflowParams = self.savedParams.workflowParams
        self.override       = self.savedParams.override;
        self.$workflowConfiguration = self.savedParams.$workflowConfiguration;
      }

      self.displayError();
      self.toggleSaveStatus(true);
      self.seriesScheduler.drawSchedules();
    }

    /**
     * Save this schedule
     */
    self.save = function(){
        var url, type, data;

        if(self.isNew){
          url   = opencast.series.aclScheduler.URL.TRANSITIONS.POST+self.seriesScheduler.id;
          type  = "POST";
        }
        else{
          url   = opencast.series.aclScheduler.URL.TRANSITIONS.PUT+self.id;
          type  = "PUT";
        }

        data = {
          applicationDate: ocUtils.toISODate(new Date(self.fromDate)),
          override       : self.$el.find('.override').attr("checked") == "checked"
        }

        if(self.aclId && self.aclId != ""){
          data["managedAclId"]   = self.aclId;
        }
        else{
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

            self.savedParams.aclId          = self.aclId;
            self.savedParams.workflowId     = self.workflowId;
            self.savedParams.fromDate       = self.fromDate;
            self.savedParams.workflowParams = self.workflowParams;
            self.savedParams.override       = self.override;
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
            url: opencast.series.aclScheduler.URL.TRANSITIONS.PUT+self.id,
            type: "DELETE",
            dataType: 'json',
            success: function() {
              self.seriesScheduler.removeSchedule(self);
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
     * @return {SeriesSchedule} return this schedule object
     */
    self.init = function(){
        self.id    = value.transitionId;
        if (self.id === undefined) {
            self.id = value.id;
        }
        self.override = value.override;

        if(value.acl && value.acl.id)
          self.aclId = value.acl.id;

        if(value.workflowId)
          self.workflowId = value.workflowId;

        if(value.workflowParams)
          self.workflowParams = JSON.parse(value.workflowParams);

        if(value.applicationDate){
          self.fromDate = new Date(value.applicationDate);
        }
        else{
          var actualDate = new Date(); 
          self.fromDate = new Date(actualDate.getFullYear(), actualDate.getMonth(), actualDate.getDate()+1); 
        }

        self.isNew = (value.applicationDate ? false : true);
        self.saved = (value.applicationDate ? true  : false);
        
        if(self.override == undefined)
        	self.override = false;
        
         if (_.isUndefined(self.workflowId)) {
          self.workflowId = "";
        } else {
          self.$workflowConfiguration = opencast.series.aclScheduler.getWorkflowConfigurationEl(self.workflowId);
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
          override               : self.override,
          $workflowConfiguration : self.$workflowConfiguration
        }

        self.seriesScheduler = seriesScheduler;
        return self;
    }

    return self.init().render();
}

/**
 * @class CurrentAcl 
 * @param {Value} element 
 */
opencast.series.aclScheduler.CurrentAcl = function(value, seriesScheduler){

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
     * @return {SeriesSchedule} return this schedule object
     */
    self.setElement = function(newElement){
        self.$el        = $(newElement);
        self.$saveBtn   = self.$el.find('.save');
        self.$cancelBtn = self.$el.find('.cancel');

        return self;
    };

    /**
     * Listener for the changes happening on the wokflow selector
     * @param  {DOMEvent} event 
     */
    self.changeWorkflowListener = function(event){
        if(event.target.value == self.workflowId)
            return;

        self.workflowId = event.target.value;
        self.toggleSaveStatus(false);
    };

    /**
     * Listener for the changes happening on the acl selector
     * @param  {DOMEvent} event 
     */
    self.changeAclListener = function(event){
        if(event.target.value == self.aclId)
            return;
        
        if(event.target.value == "undefined") {
        	self.aclId = undefined;
        	self.$el.find('.override').attr('checked', false);
        	self.override = false;
        	self.$el.find('.override').attr('disabled', true);
        } else {
        	self.aclId = event.target.value;
        	self.$el.find('.override').attr('disabled', false);
        }
        self.toggleSaveStatus(false);
        self.checkChanged();
    }
    
    self.checkChanged = function() {
    	var changed = false;
    	changed = changed || (self.aclId != self.savedParams.aclId);
    	changed = changed || (self.override != self.savedParams.override);
        if(!changed)
        	self.cancel();
    }

    /**
     * Register all the listener related to this schedule element
     * @return {SeriesSchedule} return this schedule object
     */
    self.registerListeners = function(){
        self.$el.find('select.acl').bind('change select',self.changeAclListener);
        self.$el.find('.cancel').bind('click',self.cancel);
        self.$el.find('.save').bind('click',self.apply);
        self.$el.find('.override').bind('click',function(event){
          self.override = ($(event.target).attr("checked") == "checked");
          self.toggleSaveStatus(false);
          self.checkChanged();
        });

        return self;
    }

    /**
     * Draw the current element
     * @return {SeriesSchedule} return this schedule object
     */
    self.render = function(){
        var params = {
            workflows: opencast.series.aclScheduler.getWorkflowDefinitions(),
            acl      : opencast.series.aclScheduler.getACL(),
            id       : self.id
        };

        if(!self.aclId)
          params.acl = "<option value=\"" + self.aclId + "\">" + self.aclName + "</option>" + params.acl;

        self.setElement($.jqote(opencast.series.aclScheduler.currentTemplate,params));

        if(self.workflowId)
            self.$el.find('.workflows option[value="'+self.workflowId+'"]').attr("selected", "selected");
        if(self.aclId)
            self.$el.find('.acl option[value="'+self.aclId+'"]').attr("selected", "selected");
        if(self.managed) {
        	if(self.override)
        		self.$el.find('.override').attr("checked","checked");
        } else {
        	self.$el.find('.override').attr("disabled", true);
        }

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
      self.override       = self.savedParams.override;
      self.toggleSaveStatus(true);
      self.displayError();
      self.seriesScheduler.drawSchedules();
    }

    /**
     * Save this schedule
     */
    self.apply = function(){
        var url, type, data;

        url   = opencast.series.aclScheduler.URL.INSTANT_APPLY + self.seriesScheduler.id;
        type  = "POST";
        
        data = {
          override: self.override
        };
        
        if(self.aclId && self.aclId != ""){
          data["aclId"]  = self.aclId;
        }
        else{
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

            self.savedParams.aclId          = self.aclId;
            self.savedParams.override       = self.override;
            self.toggleLoadingStatus(false);

            self.displayError();

            if(self.aclId)
              self.$el.find('.acl option[value="undefined"]').remove();

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
     * @return {SeriesSchedule} return this schedule object
     */
    self.init = function(){
        self.id       = "currentACL"
        self.aclId    = value.id;
        self.aclName  = value.name;
        self.override = value.override;
        if(self.override == undefined)
        	self.override = false;
        self.saved    = true;
        
        self.managed = value.managed;

        self.savedParams = {
          aclId           : self.aclId,
          override        : self.override
        }

        self.seriesScheduler = seriesScheduler;
        return self;
    }

    return self.init().render();
}

