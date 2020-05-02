Paella Player
=============

The Paella `(pronounced 'paeja')` [Player](https://paellaplayer.upv.es) is an Open Source
Javascript video player capable of playing an unlimited number of audio & video streams 
synchronously, Live Streaming, Zoom, Captions, contributed user plugins and a lot more. 
It is easy to install and customize for your own needs.

Paella has been specially designed for lecture recordings. It works with all HTML5 browsers
(Chrome, Firefox, Safari and Edge) and within iOS and Android.

Have a look to the paella [features list](https://paellaplayer.upv.es/features/)
or see them live on paella [demos page](https://paellaplayer.upv.es/demos/)

Enable paella player
--------------------

To enable paella player you need to edit the `prop.player` variable.
This can be enabled for each tenant. So the configuration keys are located in
`etc/org.opencastproject.organization-mh_default_org.cfg`.


To activate the paella player set:

    prop.player=/paella/ui/watch.html?id=#{id}


Configuration
-------------

The configurations for the paella player are done for each tenant. The paella configuration files are located in
`etc/ui-config/<tenant_id>/paella/config.json`.

For the default `mh_default_org` tenant file is located at `etc/ui-config/mh_default_org/paella/config.json`.

For more information about the configuration format options, see the paella [documentation](https://paellaplayer.upv.es/docs/)


Tracks to be played
-------------------

An event can have many tracks, but an institution can configure which of these tracks are played and which are not.
To do it, you need to configure [es.upv.paella.opencast.loader](plugins/es.upv.paella.opencast.loader.md) plugin.

Multiple audio tracks
---------------------

An event can have multiple audio tracks. Paella only plays one at a time, but you can configure paella to allow the user to 
decide which one to play. Read the [es.upv.paella.opencast.loader](plugins/es.upv.paella.opencast.loader.md) documentation
plugin for more information.

This feature is usefull when you have multiple audio languages, so the users can switch to the audio language they want.
