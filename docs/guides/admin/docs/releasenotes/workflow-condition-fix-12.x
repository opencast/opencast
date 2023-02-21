Fix workflow conditions with string variables

Re-enable parsing of workflow conditions with string variables where the variable is unquoted, as was possible
in 11.x, while still keeping the option to quote string variables as was introduced in 12.0.
This change was initially aimed at 12.x, 12.8 or higher.