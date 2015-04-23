# Architecture

## Overview
The architecture of the theodul player has a plugin based structure based around a core. The core and the plugins have been realized as OSGi modules. Each plugin can be separately build.
The following figure shows the OSGi architecture of the player.

![Architecture](player.architecture1.png)

All Theodul OSGi modules are stored under:

    modules/matterhorn-engage-theodul-*
    #Core module
    modules/matterhorn-engage-theodul-api/
    modules/matterhorn-engage-theodul-core/
    #A plugin module
    modules/matterhorn-engage-theodul-plugin-*
    modules/matterhorn-engage-theodul-plugin-tab-description/

## Plugin Manager

The main workflow is implemented by the core, which recognizes new plugins, collects information about the plugin type and resources, runs the JavaScript logic and inserts the first compiled templates into the HTML DOM.
The Plugin Manager Endpoint recognizes the OSGi modules. Each plugin has some information about its name and its resources. The Plugin Manager collects these information and publishes them via a REST endpoint. The following URL links to an example REST endpoint:

http://localhost:8080/engage/theodul/manager/list.json

The documentation and test forms of the endpoint can be found on the Matterhorn start page. The following data in JSON shows an example list of plugins, which are used by the player and provided by the Plugin Manager Endpoint.

    {
      "pluginlist":{
        "plugins":[
        {
          "name":"EngagePluginTabSlidetext",
          "id":"6",
          "description":"Simple implementation of a tab with the text of the slides",
          "static-path":"6\/static"
        },
        {
          "name":"EngagePluginControlsMockup",
          "id":"5",
          "description":"Simple implementation of a control bar",
          "static-path":"5\/static"
        }]
      }
    }

Next to the Plugin Manager there is the Theodul Core module, which publishes the main HTML page, core.html.

## UI Core
The **core.html** is the main entry point and starts the Javascript core logic. Following listing shows the directory structure of core in the **matterhorn-engage-theodul-core OSGi** module.

    |-src
    |---main
    |-----java          #Java impl of the plugin manager
    |-----resources
    |-------ui          #UI of the core, core.html and engage_init.js
    |---------css       #Global CSS Styles
    |---------js        #JavaScript logic
    |-----------engage  #Core logic, engage_core.js and engage_model.js
    |-----------lib     #External libraries, backbone.js, jquery.js, require.js and underscore.js
    |---test            #Unit Tests
    |-----resources
    |-------ui          #JavaScript Unit Tests
    |---------js
    |-----------spec

All Theodul JavaScript components are defined as a RequireJS module. The file engage_init.js is loaded firstly and contains the configuration of RequireJS. This init script additionally loads the core module, which is defined in the engage_core.js.

The core module initializes the main HTML view. This view is realized as a BackboneJS view and is linked to a global Backbone model, which is stored in the model module in engage_model.js. The view is returned by the core module, so every other module, which has a dependency to the core module, has a reference to the view (simply called "Engage" in the plugins) and it's functions. See the Core Reference for more information about the functions of the core view.

## Plugins

Plugins in the Theodul player are developed and distributed in own OSGi modules. Every plugin has a special UI type. In dependency of this type the core injects the plugin to the right position of the player. The following plugin types are possible:

|Plugin Type|Description|Characteristics|Module Name|JS Plugin Type Name|Maven Plugin Type Name|
|-----------|-----------|---------------|-----------|-------------------|----------------------|
|Controls	|Implements the main controls of the top of the player	|Only one plugin per player possible.	|matterhorn-engage-theodul-plugin-controls	|engage_controls	|controls|
|Timeline	|Timeline information below the main controls.|Good for processing time-based data like user tracking, slide previews or annotations.|Optional plugin, more than one possible.	matterhorn-engage-theodul-plugin-timeline-<pluginName>	|engage_timeline	|timeline|
|Videodisplay	|Implementation of the video display.	|Currently only one plugin per player possible, but in the future more video displays should be possible.|matterhorn-engage-theodul-plugin-video-<pluginName>	|engage_video	|video|
|Description/Label	|A plugin below the video display, good to show simple information about the video, like a title and the creator.	|Only one plugin per player possible.	|matterhorn-engage-theodul-plugin-description	|engage_description	|description|
|Tab	|Shows a tab in the tab view at the bottom of the player.	|Optional plugin, more than one possible.	|matterhorn-engage-theodul-plugin-tab-<pluginName>	|engage_tab	|tab|
|Custom	|A custom plugin without a relationship to an UI element.|Good for a custom REST endpoint, global data representation or to load custom JS code or libraries.|Optional plugin, more than one possible.|No connection to a preserved UI element.|matterhorn-engage-theodul-plugin-custom-<pluginName>	|engage_custom	|custom|

The following listing shows the directory structure of a plugin module:

    |-src
    |---main
    |-----java
    |-------org
    |---------opencastproject
    |-----------engage
    |-------------theodul
    |---------------plugin
    |-----------------controls  #Simple Java class, and optional REST endpoint
    |-----resources
    |-------OSGI-INF            #OSGi information about the plugin
    |-------static              #web ressources, contains the main.js entry point of the plugin
    |---------images            #plugin ressources
    |---------js                #plugin js libs
    |-----------bootstrap
    |-----------jqueryui
    |---test                    #Jasmine test ressources
    |-----resources
    |-------js
    |---------engage            #Test Wrapper of the core
    |---------lib               #Required test libs
    |---------spec              #Jasmine test specs

The main JavaScript entry point of the plugin is main.js in the static folder. This contains the RequireJS module definition of the plugin and the main logic. All other plugin logic can be implemented as a RequireJS module and loaded in the main module. The main module should have a dependency to the core, the Engage object. With this object you have access to main features of the core. See the Core Reference for more information about that.

After the initialization process of the plugin, the plugin returns a plugin object with information about the plugin, like the type, the name, the ui template etc. This object is used by the core to decide about the UI type/location of plugin. The Core Reference describes the plugin object, before and after it is being processed by the core.

Have a look to the code of a plugin to get an impression about the plugin implementation.

##Model View Controller Support

The Theodul player supports MVC design patterns for each plugin based on methods and objects of the BackboneJS library. It is not necessary to design a plugin in MVC style but it is highly recommended. An overview of the methods and objects of the BackboneJS library is listed on the official website of BackboneJS.

Each plugin with a visual component has a reference to its view container and its template to fill the view container. Have a look at the Core Reference how to access the container and the template data. With this information the plugin can create a Backbone view with a reference to the to div container and a render function to compile the template.

The next step is the creation of a model, which is being bound to the view. An usual way is to create a Backbone model, which is being passed by the view. In the initialization function of the view, the view binds the model change event to his render function:

*Bind the "change" event always to the render function of a view*

    // bind the render function always to the view
    _.bindAll(this, "render");
    // listen for changes of the model and bind the render function to this
    this.model.bind("change", this.render);

The model can only be visible by the plugin itself or it can be added to the global Engage model of the core. Adding the model to the Engage model has the advantage, that on the one hand data can be used by other plugins and on the other hand it is able to listen to change- or add-events. So other plugins are able to listen to a change of data in another model and can react to it by e.g. re-render its view. This feature is e.g. used by the "mhConnection" custom plugin. The plugin receives data of Matterhorn endpoints and saves them to a model, which is being added to the Engage Model. Each time the plugin gets newer endpoint data and updates its model's data, each plugin gets a notification and can re-render its view.

A typical way to add a model to the Engage model is to add the model in the initialization function of the plugin after all other initializations. Here is an example of the video plugin:

### Add a custom model to the Engage Model

    Engage.model.set("videoDataModel", new VideoDataModel(videoDisplays, videoSources, duration));

In the same initialization function an event handler should be added to notice the addition of the model. Has the model successfully been added, a view with this model and other data can be created:

*Model Event Handler*

    Engage.model.on("change:videoDataModel", function() {
       new VideoDataView(this.get("videoDataModel"), plugin.template, videojs_swf);
    });

If another plugin wants to use the defined "videoDataModel" model, it has to list it in its own initialization process:

    Engage.model.on("change:videoDataModel", function() {
       initCount -= 1;
       if (initCount === 0) {
          initPlugin();
       }     
    });

Have a look at the full implementation of the VideoJS Plugin and the Controls Plugin to get an idea how the Backbone MVC design works. For completeness' sake, the "Controller" does not have an extra Object in the Backbone MVC design. The "Controller" is usually used as the render function in the view. This function can be very complex and should link to other functions, which are short and easy to be tested by the Jasmine Test Framework.

