Licenses and Legal Matters
==========================

> This is a guide from developers for developers and therefore not an official legal document. We try to be as accurate
> as possible but cannot guarantee that there are no mistakes. If in doubt, please send a question to the Opencast
> developer mailing list, to the Opencast Board or to the Apereo Foundation, preferably in that order.


Which Licenses May I Use for Opencast?
--------------------------------------

All libraries that are used in Opencast need to be compatible with the Educational Community License version 2.0.

In short, if the license is on:

* the Apereo or Apache Category-A list, it is fine to use the library.
* the Apereo or Apache Category-B list, we may use it unmodified. Please avoid Category-B if possible.
* the Apereo or Apache Category-X list, we cannot use it.

Everything else needs to go to the Apereo foundation for approval.

These lists, along with a more detailed explanation, can be found on the [Apereo page about practices on third-party
licenses](https://apereo.org/licensing/third-party)


Here is the slightly longer section from the „Apereo Licensing & Intellectual Property Practices“ page:


How To Label Third Party Libraries
----------------------------------

Third party libraries that are used within an Opencast system in production need to be listed in the `NOTICES` file,
which can be found in the root directory of our code repository. Libraries and tools that are only used for testing or
building the project, or are run via system call, do not need to be listed (e.g. maven, maven plugins, junit, ...).


### JavaScript Libraries

For JavaScript libraries, list the file or folder that is included and give a short statement about the copyright and
license. This can usually be taken from the copyright header at the top of the library.

Example:

    modules/admin-ui/src/main/webapp/lib/angular/*
      AngularJS v1.3.6
      (c) 2010-2014 Google, Inc. http://angularjs.org
      License: MIT


### Java Libraries / Maven Dependencies

Java dependencies are listed pre module. Maven provides some helpful tools to list dependencies and even report
libraries. Have a look at the output of `mvn dependency:list` and `mvn dependency:tree` or generate a full report for a
module using:

    mvn -s settings.xml project-info-reports:dependencies

This will create a file `target/site/dependencies.html` containing a full report, including the library versions and
licenses.

Finally, add the library and license to the `NOTICES` file in the form:


    rn-workflow-service-remote
      GroupId       ArtifactId    License
      commons-io    commons-io    The Apache Software License, Version 2.0
