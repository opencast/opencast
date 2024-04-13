Playlists
=========

A list of videos, similar to the same concept in YouTube. This provides an n:m mapping, meaning that every playlist can
contain multiple videos, and each video can be included in multiple playlists. (As opposed to the 1:n mapping of
Opencast series, where each video is part of at most one series.)

Access to a playlist can be controlled via its ACL, much like events and series. Note that a playlist ACL only
pertains to the playlist, not the videos in the list. These are still governed by their own ACLs.
For example, even though a certain user may  be allowed to GET a playlist, they may not have access to the
publication of a certain video in the playlist, and trying to access that publication will result in an access error. 

Playlists are currently only available through their rest endpoint and don't show up anywhere else.
