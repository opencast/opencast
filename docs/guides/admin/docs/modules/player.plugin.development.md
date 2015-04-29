# How To Create A New Plugin

## Plugin Archetype

The [Maven Archetype Plugin](http://maven.apache.org/archetype/maven-archetype-plugin/) provides a convenient mechanism for automatically generating projects. Project templates are called Archetypes and they are basically maven artifacts of a special kind of packaging, ‘maven-archetype’.

With the Theodul Plugin Archetype you can create a new plugin project in no time and start writing the plugin’s business logic right away, without caring about the POM or SCR component declarations.

### Installation

The Theodul Plugin Archetype is included in the Matterhorn source code (Theodul Player branch) in the modules directory. To make the artifact available on your system you need to install it like any other atrifacts. In the Matterhorn source directory type:

    > cd modules/matterhorn-engage-theodul-plugin-archetype
    > mvn install

After successful build and installation the archetype is available in your system.

### Generating a new plugin

To generate a new plugin project simply go to the modules directory inside the Matterhorn source directory and type:

    > mvn archetype:generate -DarchetypeGroupId=org.opencastproject -DarchetypeArtifactId=matterhorn-theodul-plugin

Provided the archetype is installed maven will now ask you for the properties configuration for the new project:

    [INFO] Generating project in Interactive mode
    [INFO] Archetype [org.opencastproject:matterhorn-theodul-plugin:1.5-SNAPSHOT] found in catalog local
    Define value for property 'groupId': : org.opencastproject
    Define value for property 'artifactId': : matterhorn-engage-theodul-plugin-test
    Define value for property 'version': 1.0-SNAPSHOT: : 1.5-SNAPSHOT
    Define value for property 'package': org.opencastproject: : org.opencastproject.engage.theodul.plugin.custom.test
    Define value for property 'plugin_description': : A test plugin
    Define value for property 'plugin_name': : testName 
    Define value for property 'plugin_type': : custom
    Define value for property 'plugin_version': : 0.1
    Define value for property 'plugin_rest': : false
    Confirm properties configuration:
    groupId: org.opencastproject
    artifactId: matterhorn-engage-theodul-plugin-test
    version: 1.5-SNAPSHOT
    package: org.opencastproject.engage.theodul.plugin.test
    plugin_description: A test plugin
    plugin_name: test
    plugin_rest: true
     Y: : y
    [INFO] ----------------------------------------------------------------------------
    [INFO] Using following parameters for creating project from Archetype: matterhorn-theodul-plugin:1.5-SNAPSHOT
    [INFO] ----------------------------------------------------------------------------
    [INFO] Parameter: groupId, Value: org.opencastproject
    [INFO] Parameter: artifactId, Value: matterhorn-engage-theodul-plugin-test
    [INFO] Parameter: version, Value: 1.5-SNAPSHOT
    [INFO] Parameter: package, Value: org.opencastproject.engage.theodul.plugin.test
    [INFO] Parameter: packageInPathFormat, Value: org/opencastproject/engage/theodul/plugin/test
    [INFO] Parameter: package, Value: org.opencastproject.engage.theodul.plugin.test
    [INFO] Parameter: version, Value: 1.5-SNAPSHOT
    [INFO] Parameter: plugin_description, Value: A test plugin
    [INFO] Parameter: plugin_name, Value: test
    [INFO] Parameter: groupId, Value: org.opencastproject
    [INFO] Parameter: plugin_rest, Value: true
    [INFO] Parameter: artifactId, Value: matterhorn-engage-theodul-plugin-test
    [INFO] project created from Archetype in dir: /home/wulff/code/UOS/plugin-archetype/test/matterhorn-engage-theodul-plugin-test
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time: 3:39.195s
    [INFO] Finished at: Thu Jan 23 15:48:37 CET 2014
    [INFO] Final Memory: 15M/308M
    [INFO] ------------------------------------------------------------------------

There you go, the newly created plugin project is waiting to be filled with life in the directory that is named after the atrifactId you entered before.

### Project Properties

In addition to the above explanation, here is a description of the properties you have to specify when generating a new project with the Theodul Plugin Archetype:

#### groupId

Maven group ID. For the Matterhorn developers this is

    org.opencastproject

#### artifactId

Maven artifact ID. Name by which your project is identified as an artifact by maven. Think of it as the project name. It will also be used as the name for your projects root directory. During the course of the Theodul project the following naming scheme came up:

    matterhorn-engage-theodul-plugin-<plugin type>-<plugin name>

#### version

The project version. For Matterhorn developers: simply put in the version of the Matterhorn source tree your are working on.

#### package

The Java package in which the source for the back end part of your plugin will live. The following scheme is used by the Theodul developers:

    org.opencastproject.engage.theodul.plugin.<plugin type>.<plugin name>

#### plugin_version

The version of the plugin itself. This is not to be confused with the maven project version which will, for instance, be updated when the Matterhorn version changes.

#### plugin_type

The type of the plugin to be created. See https://opencast.jira.com/wiki/display/MH/Architecture
Possible types are: custom, controls, timeline, video, description, tab

#### plugin_name

The name by which your plugin will be registered by the plugin manager when running.

#### plugin_description

(optional) A short description of the plugin. The description will be provided by the [plugin list endpoint](https://engagedevcamp.wordpress.com/2013/04/15/plugin-infrastructure/) together with the other plugin data.

#### plugin_rest

(boolean) Whether or not the plugin should provide a Matterhorn Rest endpoint. If set to true, the Java class that makes up the back end part of your plugin will be augmented with the annotations necessary to work as a Rest endpoint provider in Matterhorn. Also an example endpoint (GET:sayHello) will be generated.


## Example Plugin
Have a look at the [snow showcase example plugin (custom)](https://bitbucket.org/CallToPower/theodul-snowshowcase-plugin).

## Debugging
To display debug information in the developer console, add the following parameters to the URL:

**Display debug information**

    &debug=true

**Display event debug information**

    &debugEvents=true

