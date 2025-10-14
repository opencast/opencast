Tobira
======

Tobira is a video portal for Opencast that aims to be a pleasant interface through which users interact with Opencast content.

Videos, series and playlists can be presented in a customizable, hierarchical page structure, and users can easily search through all media. Additionally, Tobira offers tools to upload videos and create series, as well as to edit some metadata. This is currently limited to changing the title, description, access policies and series content.

Any changes done in Tobira are immediately sent to Opencast, while changes done in Opencast are synced periodically to prevent conflicts.
Tobira's main goal is to serve as a video portal for end users.
**It is not meant to replace the Admin interface, which is still the preferred address for admins to manage publications.**

## Setup and configuration

Tobira runs separately from Opencast and needs its own installation. The configuration necessary to connect these systems is mainly done in Tobira itself, but there are some things that you'll need to adjust in Opencast as well. For an in-depth explanation and the complete setup and configuration guide, see Tobira's main [documentation](https://elan-ev.github.io/tobira/setup).

## Tobira in the Admin UI

When you have connected a Tobira instance to your Opencast, the Admin UI's series modal will now allow you to choose the series' mount point (i.e. the page it will show up on) in Tobira.
In addition to that, detail overviews for series and events have an extra panel that lists all pages in Tobira that contain the respective series or video.
In order for these options to show up in the Admin UI, you need to configure `tobira.mh_default_org.origin` and `tobira.mh_default_org.trustedKey` in `etc/org.opencastproject.adminui.endpoint.SeriesEndpoint.cfg`.
