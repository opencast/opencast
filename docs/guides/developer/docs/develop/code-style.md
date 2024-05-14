Code Style
==========

Maven will automatically check for coding style violations for Java and interrupt the build process if any are found,
displaying a message about it. Apart from that, here are some general rules:


General Rules
-------------

- UTF-8 encoded
- No trailing spaces
- End files with a newline character


Java, HTML and JavaScript
-------------------------

- Indentation is done with exactly two spaces
- No line of code should be wider than 120 columns
- Avoid unnecessary code style changes


Markdown
--------

- Avoid lines wider than 120 columns
- Avoid unnecessary style changes


Everything Else
---------------

- Try applying the Java style rules
- If in doubt, ask on list


Logging Rules
-------------

The following is a list of logging levels and their use.

Level | Description
------|---------------------------------------------------------------------------
TRACE | Information that would only be useful when debugging a specific subsystem.
DEBUG | Information relevant to development. Used to provide detailed information for developers.
INFO  | Information relevant to server administrators. Creating, updating, and deleting files should be logged here.
WARN  | Handled exceptions. A warning should be logged any time there is a possible problem administrators may need to investigate..
ERROR | Unhandled exceptions. Problems that were not automatically handled.
