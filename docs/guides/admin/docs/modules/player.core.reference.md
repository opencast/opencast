# Core Reference

## Engage Core
*RequireJS Path*

    'engage/engage_core'

Inherited object functions of the BackboneJS view, see http://backbonejs.org/#View

Added object functions and properties:

|Name|Parameters|Type|Description|
|----|----------|----|-----------|
|log(value):void	|value:String	|function	|function to log via the core cross browser|
|Event:EngageEvent|none	|property	|Returns the EngageEvent object prototype, the see EngageEvent Object for more information|
|trigger(event):void	|event:EngageEvent	|function	|triggers an EngageEvent|
|on(event, handler, context):void	|event:EngageEvent, handler:function, context:object|function	|install an event handler on a EngageEvent|
|model:EngageModel	|none	|property	|Returns the singleton engage model for this session, see EngageModel for more information's|
|getPluginPath(pluginName):String	|pluginName:String	|function	|Returns the absolute path of a plugin by name.|

## EngageEvent Object

|Name|Paramters|Type|Description|
|----|---------|----|-----------|
|EngageEvent(name, description, type)	|name:String, description:String, type:String | constructor	|Create a new unbound EngageEvent, with a name, description and a type. For Example: var myEvent = new EngageEvent('play', 'plays the video', 'trigger')|
|getName:String	|none	|function	|Gets the name|
|getDescription:String	|none	|function	|Gets the description|
|getType:String	|none	|function	|Gets the Type, can be a "handler", "trigger" or "both"|
|toString:String	|none	|function	|Build a string that describes the event|

## Engage Model

Inherited object functions of the BackboneJS model, see http://backbonejs.org/#Model, how to use BackboneJS models. This model is a global singleton object and can be used by each plugin to add new models which can be used by another plugin again.

No special functions are added, but the model is filled with some default data. This default data can be used by each plugin, which has a reference to the EngageModel.


|Property Name|Type|Description|
|-------------|----|-----------|
|pluginsInfo	|Backbone Model	|Contains Information's of each plugin|
|pluginModels	|Backbone Collection	|Contains the plugin models|
|urlParameters	|Object	|Contains the data of the URL parameters.|
 
## Plugin Object

Each plugin **must** create and return a object with some properties which are set by the plugin itself. It is recommend to keep a reference to the object because some properties are set by the core after the plugin is processed.

|Property Name|Type|Description|
|-------------|----|-----------|
|name	|String	|Name of the plugin, e.g. "Engage Controls". **This property is set by the plugin.**|
|type	|String |Type of the plugin, e.g. "engage_controls", see the plugin table in Architecture for the other plugin types. **This property is set by the plugin.**|
|version	|String	|Version of plugin. **This property is set by the plugin.**|
|styles	|Array of Strings	|Array of the paths of css files relative to the static folder of each plugin . **This property is set by the plugin.**|
|template	|String	|Before the plugin object is returned by the plugin logic, the template property contains the path to the template relative to the static folder. **The path property is set first by the plugin**. After the plugin object is returned and the Theodul core processed the plugin, the template property is filled with the real template data and can be used to re-render the view.|
|container	|String	|Contains the ID of the HTML div container, which contains the rendered template. This can be used to re-render the view. **This property is set by the core.**|
|pluginPath	|String	|Contains the absolute path of the plugin.  **This property is set by the core.**|
|events	|Object	|Contains all events which are used of this plugin. Each handled and each triggered event.|

