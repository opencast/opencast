ocCaptureAgent = new (function ()
  {
  
    this.polling_time;
    this.last_updated;
    this.agentsObj = {};
    this.agentsObj["agents"] = [];
    this.agentsCount = undefined;
    this.dispAgCount = 0;
    
    this.init = function ()
    {
      $.ajax({
        dataType : 'json',
        type : 'get',
        url : '/capture-admin/agents.json',
        success : processAgents
      });
    }
    
    function displayAgentState(data){		
      var agent_name = "";
      ocCaptureAgent.dispAgCount++;
      ocCaptureAgent.polling_time = 0;			

      if ( data["properties-response"].properties != ""){			
        $.each(data["properties-response"].properties.item,function(a,item){				
          if (item.key==="capture.agent.state.remote.polling.interval"){
            ocCaptureAgent.polling_time = item.value;
          }
          if (item.key==="capture.agent.name") {
            agent_name = item.value;
          }				
        });	
      }				
      if (ocCaptureAgent.polling_time > 0) {	
        $.each(ocCaptureAgent.agentsObj["agents"],function(i,item){
                // Added a 5 second slack to account for overheard processing the http request.
		if ((item.name == agent_name) && (item["time-since-last-update"] > (ocCaptureAgent.polling_time + 5) * 1000 )) {						
            item.state = "offline";
            return false;
          }	
        });
      }
      showAgentsStatus();		
    }

    function processOneAgent(agent){
      $.each(agent,function(b,property){
        var devices,devices_arr;
        if (b==="url"){
          if (property=="") property="#";
          else if(property.lastIndexOf("http://")!=0 && property.lastIndexOf("https://")!=0)
            property = "http://"+property;
        }			
        if (b==="name") {
          $.ajax({
            dataType : 'json',
            type : 'get',
            url : '/capture-admin/agents/'+property+'/configuration.json',
            success : displayAgentState
          });
        } else if (b==="time-since-last-update") {
          ocCaptureAgent.last_updated=property;
        } else if ((b==="capabilities")&&(property!="")) {
          if(!$.isArray(property.item)) {
            property.item = [property.item];
          }
          $.each(property.item,function(c,device){
            if (device.key=="capture.device.names"){									
              devices = device.value.toLowerCase().split(',');							
              devices_arr = device.value.toLowerCase().split(',');										
              return false;
            }
          });
          var i;
          agent["devices"] = devices;
          for(i=0;i<devices.length;i++) {
            agent.devices[i] = {
              "device": devices[i]
            };              
            agent.devices[i]["properties"] = [];
          }
          $.each(property.item,function(c,device){
            var prop = device.key.split('.');
            var name = "",i;
            for (i=3;i<prop.length;i++){
              name += prop[i]+" ";
            }						
            var index = $.inArray( prop[2].toLowerCase(), devices_arr);
            if ( index != -1) {														
              agent.devices[index]["properties"].push({
                "key":$.trim(name),
                "value":device.value
              });	
            }
          });
        } else {
          agent[b.toString()] = property;	
        }
      });		
    }	
	
    function processAgents(data){			
      $.each(data.agents,function(a,agent){			
        if ($.isArray(agent)) { 
          $.each(agent,function(b,one){ 
            processOneAgent(one); 
          }); 
        }	
        else processOneAgent(agent);
        ocCaptureAgent.agentsObj.agents = $(agent).toArray();
      });
      ocCaptureAgent.agentsCount = ocCaptureAgent.agentsObj.agents.length;	
      showAgentsStatus();									
    }		

    function showAgentsStatus(){
      //      var result = TrimPath.processDOMTemplate("tableTemplate", agentsObj);	
      //      $('#stage').empty().append(result);

      $('#addHeader').jqotesubtpl("templates/capture_agents-table.tpl", ocCaptureAgent.agentsObj);
			 		
      $('#captureTable').tablesorter({
        cssHeader: 'oc-ui-sortable',
        cssAsc: 'oc-ui-sortable-Ascending',
        cssDesc: 'oc-ui-sortable-Descending' ,
        headers: {
          3: {
            sorter: false
          }
        }
      });				
										
	   
      $("ul.propnav li").click(function() { 		  
        //Drop down the subnav on click  
        $(this).find("ul.itemnav").slideDown('fast').show();
	 			
        $(this).find('ul.itemnav li span.dev-prop-val').each(function(){
          var span_len = $(this).parents('li').width()-$(this).siblings('span').width();
          if ($(this).width()>span_len) {	
            var font_size = Math.round(parseFloat($(this).css('font-size')));
            var letters = Math.floor(span_len/font_size - 3);
            $(this).attr('title',$(this).text());
            $(this).text($(this).text().substring(0,letters)+"...");													
          }
        });
			   
        $(this).hover(function() {  
          }, function(){  
            //When the mouse hovers out of the subnav, move it back up
            $(this).find("ul.itemnav").slideUp('slow');   
          });  
			   			 
      }); 				
    }
  
  })();