# Configure Atom and RSS Feeds

This document will help you understand and configure the Matterhorn RSS and Atom feed catalog. The catalog supports multiple versions of each syndication format.

## Feed Catalog
The catalog is located at:

    http://<matterhorn_root>/feeds

Individual feeds are located at:

    http://<matterhorn_root>/feeds/<feed_selector>

## Defaults

The catalog contains the following default feeds:

### Latest 
    http://nightly.opencastproject.org/feeds/[atom|rss|*]/<version_number>/latest

Need an example? Visit http://demo.opencastproject.org/feeds/atom/1.0/latest
### Series 
    http://nightly.opencastproject.org/feeds/[atom|rss|*]/<version_number>/series/<series_id>
### Aggregation (of a set of series) 
    http://nightly.opencastproject.org/feeds/[atom|rss|*]/<version_number>/aggregated/<name_of_configured_aggregation>
### Custom 
    http://nightly.opencastproject.org/feeds/[atom|rss|*]/<version_number>/custom/<query>

## Aggregation
The feed allows administrators to pre-configure feeds for specific sets of series. Given the following configuration, http://<matterhorn_root>/feeds/aggregated/myseries would return the latest episodes from series series_1 and series_2.
The Matterhorn feed specifications are located in:

    <felix_home>/conf/feeds

Update aggregation.properties, the specification for an example feed aggregation:

    feed.selector=myseries
    feed.series=series_1,series_2

## Custom
The Matterhorn feed specifications are located in:

    <felix_home>/conf/feeds

Below is custom.properties, the default specification for an example custom feed of published episodes:

    feed.class=org.opencastproject.feed.impl.CustomFeedService
    feed.uri=custom
    feed.size=25
    feed.selector=custom
    feed.query=dc_title:{0}
    feed.name=Special episodes
    feed.description=Special collection of episodes
    feed.copyright=All rights reserved by The Opencast Project
    feed.home=/engage/ui
    feed.entry=/engage/ui/embed.html?id={0}
    feed.cover=/engage/feed-cover.png
    feed.rssflavors=presentation/delivery,presenter/delivery
    feed.rsstags=rss
    feed.rssmediatype=video,audio
    feed.atomflavors=presenter/delivery,presentation/delivery
    feed.atomtags=atom

### Properties
The following properties are common to all feed specifications:

|Name|Description|
|----|-----------|
|feed.class|Java implementation, e.g. LatestFeedService.|
|feed.uri|Feed location/identifier|
|feed.size|Maximum number of entries in the feed (with a default of 100 if ommitted). Set to 0 to include all available entries.|
|feed.selector|Feed route pattern, e.g. latest.|
|feed.name|Feed title|
|feed.description|Feed description|
|feed.copyright|Feed copyright notice|
|feed.home|Feed catalog homepage, e.g. http://www.opencastproject.org.|
|feed.entry|The route pattern used to generate links to individual enclosures, e.g. /engage/ui/embed.html?id={0}.|
|feed.cover|Feed image|
|feed.rssflavors|The RSS enclosure route pattern, e.g. presenter/delivery, selected according to their appearance.|
|feed.rsstags|A comma, semi-colon or space-separated list of tags used to filter available enclosures|
|feed.rssmediatype|A comma, semi-colon or space-separated list of tags used to decide whether to prefer video or audio enclosures|
|feed.atomflavors|The Atom enclosures route pattern, e.g. presenter/delivery.|
|feed.atomtags|A comma, semi-colon or space-separated list of tags used to filter available enclosures|

The following properties are specific to custom feeds:

|Name|Description|
|----|-----------|
|feed.query|A custom lucene query, matched again Java's MessageFormat using solr.|

Given the following query, http://<matterhorn_root>/feeds/alphabetical/a would return all episodes starting with the letter a.

    feed.selector=alphabetical
    feed.query=dc_title:{0}*
