<!-- Open comments Icon -->
[icon_open_comment]:data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA3UlEQVR4nKXTvUoDQRTF8V/GYO3HA0heIAbyBpaWgpWFjRCwsLIWbAVjkTZtirSCdVLYS4JC6hSKWNqJoMXuxUUQd7MHBqa4/zPnwplGt9NRR6lwb+IUM3z9cR5wgrWAGnmCHdyiXfLhGfbxnLCBSQUYdnGP7YQLtCrAoRYuEw5XgEMHCZs1DLYSHmsYPCXc1DDoJ4wxWAG+xiiKdCYrURm9oYdzsvaFFr8GP2Tt+8Qr5rjLE7/HUBis46oAT3GM5X9xYoUhujl4hL0yMD9/oS3b7aUMVFSsMK8Khr4BytkpdpaZ1jIAAAAASUVORK5CYII= "Open comments icon"


<!-- Resolved comments Icon -->
[icon_resolved_comment]:data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAABI0lEQVR4nKXSvSuFYRgG8N95nVisMvgqoRh0chhkYmUxWUkmf8DJQKEkpUS+MljZlc2mlFK+TgYWi04MYlM+hvd9O5J4D1fdPffz1HXd9/3cVyqbyfgPgi/3Viwijze8R/GKCyyg5TuBCmziFA2YQxMqo2jGPBpxjlWUQyqbyVRgH1UYiqr/hDbsoID+NCZQjw48JRg7jx6cIBdgGJMJyTGeMYWRANUJ2v4OV6gJoqTzDwJduAmwjGnUlkCuxQxWAmwLt3CI3gTkARxjDxvp6HEM4zhAN46EW3lBmfCfujGIOsxiCdJFYXncR+caRhVdWBAaaB27eIxJsUC50KYPuMS10H23v80TW3kL2ahSDn1JyIRWhvao/bskpM+IRzgrlRjjA6hxPa3f8mSnAAAAAElFTkSuQmCC "Resolved comments icon"

<!-- Delete icon -->
[icon_delete]:data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABEAAAARCAYAAAA7bUf6AAABEklEQVR42q2Uuw4BURRFVYpLoSCYL2PQTTU0vsBXeY14TIyan/Ao6ChQcE6yJTs37phCsTLZ++x7cp+Te9TrNkWhL6yEi/DCdwW/aI+xG/jCUQe6QN13NRlQcCM0hRpqNegNZQZ2Ex+Fp9CF56KrOeTb6n324AQzUDMDAfJnoaRGD0ZCobUwFgx0QYiELWUSjOupiCCaFBjCWwoVIYaeUKYBL1Kxh6hSwAhz+DdqaCjjwT+ouEPkGMzgqjV8y1Y9j9pdxeHLTAqfJVCjRdpMIogGBaa0hLI2gB592ZOZ63R2aGSgDTY7dpzOf+6J0qIbG/5oENKN7aS9nQRr9nAKHnTiejtMK+MrbvO4tP9JnPV/8gansczJeXp0AgAAAABJRU5ErkJggg== "Delete icon"

<!-- Hamburger Icon -->
[icon_hamburger]:data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAPCAYAAAAGRPQsAAAARklEQVQ4y2Ow6L3SCsQ/gfg/BRikv5WBCgbB8GcGKrnsF9hlIwSQEGY/CYYLiYH/mVouG1ExRqUwIxy7FGalz9RyGUbsAgCNXmeVduHT9gAAAABJRU5ErkJggg== "Edit Icon"


# Overview

An Event in Opencast refers to a single lecture, training, or meeting that is automatically recorded or uploaded. Events can either stand alone or they can belong to [a Series](series.md) can be scheduled or created ad-hoc if an institution allows it. You can access the Events page from the **Main Menu > Recordings > Events**.


## How to create an Event

An Event can either be a single, standalone Event or one of many Events that belong to a single Series.  

1. Click the Add Event button and a Create New Event modal will open
1. On the Source tab you will need to select whether the event(s) are being scheduled or uploaded.
1. On the processing tab, you will need to select the workflow you would like this Event to be processed with. If you don’t see a workflow you recognize, contact your local systems administrator
1. Select the Access Policy you would like to apply to this Event.
1. On the Summary tab, review the Event details. To make changes click the Back button.
1. Click the Create button and you should receive confirmation that the Event was created


See also the [Access Policies section](accesspolicies.md#apply-access-policies-to-events) to find out more about applying Access Policies to Events

## Events Progress Status

The Progress column shows in which state the Task running on the Event is. These are the possible values and their meaning:

* **INSTANTIATED**: The task on the event is being prepared
* **RUNNING**: The task is currently running
* **STOPPED**: The task has stopped. Note. Stopping a task can only be achieved using the REST endpoint
* **PAUSED**: The task is paused and requires manual action (such as cutting for example) to proceed further
* **SUCCEEDED**: The task has been run successfully
* **FAILED**: The task has failed. See the Event’s details for more information
* **FAILING**: The current task is about to fail and processing artifacts are being cleaned up


## How to edit Events

Use the edit icon ( ![icon_hamburger][] ) on the Actions column to open the Event’s details and start editing.

Metadata and Access Policies are being saved automatically when the cursor leaves the field or an option is selected.

> Without further action, the information changed in the Event’s details will be updated in the asset management layer of Opencast but **not appear in the various publications channels until the Event has been re-published**. See the [Processing section](processing.md) on how to start a task on an Event.


## How to view Event’s attributes
Use the edit icon ( ![icon_hamburger][] ) on the Actions column to open the Event’s details.

* **Media**: Lists all video tracks that are available for the Event
* **Attachments**: Lists all attachments that are available for the Event
* **Workflows**: Lists all Tasks/Workflows that have been applied to the Event

## How to delete Events
From the Event page you have the capability to delete a single or multiple events.

Use the delete icon ( ![icon_delete][] ) in the actions column of each row to delete individual events. To delete Events in bulk, **select the items to be deleted** and then **Actions > Delete**.

> When events are deleted from the asset management layer of Opencast, their link to various publication channels will be lost, i. e. there will no longer be an easy way to un-publish them. It is therefore recommended to first un-publish the events in question. See the [Processing section](processing.md) on how to start a task on an Event.


## Working with comments on Events
Leaving comments are a great way to flag an event - in case of bad audio for example - for further review by yourself or your peers. The comments allows you create, reply and resolve comments on a per event basis. To access comments go to Main Menu > Recordings > Events use the edit icon ( ![icon_hamburger][] ) to access comments for an individual event.

Comments indicators are displayed in the Actions column as follow:

* ![icon_open_comment][] At least one unresolved comment on the Event
* ![icon_resolved_comment][] All comments on the Event have been resolved
* No icon means that no comment has ever been added to the Event

> Use the [Filters](searchandfilter.md) to find flagged events.


### How to write comments
Use the edit icon ( ![icon_hamburger][] ) on the Actions column to open the Event’s details and go to the **Comments tab**. Type a comment in the text field and press Submit.

### How to reply to comments
To reply to a comment, click on **Reply** in the initial comment, type your reply and press Reply. You can press Dismiss at any time to cancel.

### How to resolve comments
To resolve a comment, click on **Reply** in the initial comment, type your reply, **check the Resolve checkbox** and press Reply. The initial comment will be marked as resolved.

### How to delete comments
To delete a comment or a reply to a comment, click on the **Delete** in the comment section.
