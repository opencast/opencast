<!-- Delete icon -->
[icon_delete]:data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABEAAAARCAYAAAA7bUf6AAABEklEQVR42q2Uuw4BURRFVYpLoSCYL2PQTTU0vsBXeY14TIyan/Ao6ChQcE6yJTs37phCsTLZ++x7cp+Te9TrNkWhL6yEi/DCdwW/aI+xG/jCUQe6QN13NRlQcCM0hRpqNegNZQZ2Ex+Fp9CF56KrOeTb6n324AQzUDMDAfJnoaRGD0ZCobUwFgx0QYiELWUSjOupiCCaFBjCWwoVIYaeUKYBL1Kxh6hSwAhz+DdqaCjjwT+ouEPkGMzgqjV8y1Y9j9pdxeHLTAqfJVCjRdpMIogGBaa0hLI2gB592ZOZ63R2aGSgDTY7dpzOf+6J0qIbG/5oENKN7aS9nQRr9nAKHnTiejtMK+MrbvO4tP9JnPV/8gansczJeXp0AgAAAABJRU5ErkJggg== "Delete icon"

# How to search for content

To use the search functionality, click on the search box and type in your search terms. Search terms are automatically
sent as you type. To cancel search entries, just remove the text from the search box.

The search is case-insensitive. To improve the user experience and allow you to see search results while you are typing,
the search terms are automatically surrounded by multiple character wildcards, i.e. `hello` results in the same results
as `*hello*` and therefore would also match the string *helloworld*.

Note that the individual search terms are ORed, so when typing `hello world` you are searching for items matching either
the term *\*hello\** or the term *\*world\** (or both terms).

For the advanced user, the search boxes for Events, Series and Themes support advanced search facilities:

## Wildcard Searches and Phrases
*Events, Series and Themes only*

You can use the symbol `?` for single character wildcard searches and the symbol `*` for multiple character wildcard
searches.

For example, `str?ng` matches to both *string* or *strong*, while `st*g` matches *string*, *strong* or *strassbourg*.

You can use phrases (groups of words surrounded by double quotes) to enforce exact matches:

For example, `"Hello World"` matches *Hello World*, *Hello World 2* but not *Hello* or *World*

## Boolean Operators
*Events, Series and Themes only*

The required operator `+` can be used to limit the search results to result that must match the term following the
required operator, while the prohibit operator `-` is used to exlude items matching the term following the operator.

For example, `hello +world` matches *hello world* but not *hello again*, whereas `hello -world` matches *hello* but not
*hello world*.

The NOT operator `!` can be used to negate a term, so `!hello` matches all items **not** matching *hello*. The AND
operator `&&` can be used to override the default term conjunction which is OR.

For example, `hello world` matches to items where the terms *hello* **or** *world* occur, while `hello && world` matches
only to items where both the terms *hello* **and** *world* occur.

# Playing with Filters

## How to set Filters
Most of the pages contain filtering capabilities. In order to set a filter:
1. Click on the icon in the filter bar
1. Select the column for which the filter should be set
1. Select the value

## How to clear Filters
To remove an individual filter, simply click on the “x” in the filter’s label. To remove all filters, click on the “X”
in the filters field

# How to create Filter sets
In order to save filters, set the filters as described in the Setting Filters section and then:

1. Click the gear icon next to filters
1. Click on Save
1. Provide a name and a description (optional)
1. Click on Save

If you click on the gear icon again you will see your saved filter set with the name you provided

## How to edit Filter sets
Click on the cogwheel in the filter bar and then click on the edit icon to change the name and description of a saved
filter set. If you wish to change on which column a saved filter is set, you will need to delete the filter set and
create a new one with the correct columns.

## How to delete Filter sets
Click on the cogwheel and then on the delete icon ( ![icon_delete][] ) next to the filter you want to delete.

## Predefined Filters
A set of commonly used filters for events is displayed at the top right of the Admin UI along with the current amount
of events matching that filter, filtering by event status or start date. They can be applied to the events table by
clicking on them. Currently filters can only be added or removed from this view by a system administrator.
