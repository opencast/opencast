[icon_hamburger]:data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAPCAYAAAAGRPQsAAAARklEQVQ4y2Ow6L3SCsQ/gfg/BRikv5WBCgbB8GcGKrnsF9hlIwSQEGY/CYYLiYH/mVouG1ExRqUwIxy7FGalz9RyGUbsAgCNXmeVduHT9gAAAABJRU5ErkJggg== "Edit Icon"

[icon_delete]:data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABEAAAARCAYAAAA7bUf6AAABEklEQVR42q2Uuw4BURRFVYpLoSCYL2PQTTU0vsBXeY14TIyan/Ao6ChQcE6yJTs37phCsTLZ++x7cp+Te9TrNkWhL6yEi/DCdwW/aI+xG/jCUQe6QN13NRlQcCM0hRpqNegNZQZ2Ex+Fp9CF56KrOeTb6n324AQzUDMDAfJnoaRGD0ZCobUwFgx0QYiELWUSjOupiCCaFBjCWwoVIYaeUKYBL1Kxh6hSwAhz+DdqaCjjwT+ouEPkGMzgqjV8y1Y9j9pdxeHLTAqfJVCjRdpMIogGBaa0hLI2gB592ZOZ63R2aGSgDTY7dpzOf+6J0qIbG/5oENKN7aS9nQRr9nAKHnTiejtMK+MrbvO4tP9JnPV/8gansczJeXp0AgAAAABJRU5ErkJggg== "Delete icon"

# Overview
Access Policies allow to define which user / groups of users can access which Event or Series content. This is done by providing read and/or write permission to a Role. See the [About Roles section](groups.md#about-roles) for more information about Roles in the UI.

You can access the Access Policies page from the **Main Menu > Organization > Access Policies**

## How to add Access Policy templates
Access Policy templates can be added using the **Add Access Policy** button.

## How to edit Access Policy templates
Access Policy templates can be edited using the edit icon ( ![icon_hamburger][] ) in the Actions column.

> Note that Series and Events are not bound to an Access Policy template. Therefore, changes made to a template that has been applied **will not affect** any Event or Series Access Policy.

## How to delete Access Policy templates
Use the delete icon ( ![icon_delete][] ) in the Actions column to delete an Access Policy template.

# Using Access Policy templates
Once an Access Policy template has been created it can be applied to Series and Events. Templates are useful to efficiently adjust the Access Policy of a Series or Event.


## Apply Access Policies to Events
Access Policies can be applied to an Event upon its creation or using the edit mode.

In order to use a template, select the Access Policy template that should be applied to the Event. The changes are automatically saved when you are editing an Event.
Additional Roles can be added to the Access Policy by clicking “+ New Policy” at the bottom of the table.

Note that when an Event is being processed, changes to its Access Policy will not be saved.

## Apply Access Policies to Series
Access Policies can be applied to a Series upon its creation or using the edit mode.

In order to use a template, select the Access Policy template that should be applied to the Series. The changes are automatically saved when you are editing a Series.
Additional Roles can be added to the Access Policy by clicking “+ New Policy” at the bottom of the table.

Note that Events will inherit automatically from the Series’ Access Policy. Additionally, modifying a Series’ Access Policy will override any Access Policy that would have been previously set to Events in the Series.
