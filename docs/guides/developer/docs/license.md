Licenses and Legal Matters
==========================

> This is a guide from developers for developers and therefore not an official legal document. We try to be as accurate
> as possible but cannot guarantee that there are no mistakes. If in doubt, please send a question to the Opencast
> developer mailing list, to the Opencast Board or to the Apereo Foundation, preferably in that order.


Which Licenses May I Use for Opencast?
--------------------------------------

All libraries that are used in Opencast need to be compatible with the Educational Community License version 2.0:

 - In short, if the license is on this list, it is fine to use the library:
    - http://www.apache.org/legal/resolved.html#category-a

 - If instead it is listed here, we cannot use it:
    - http://www.apache.org/legal/resolved.html#category-x

Everything else needs to go to the Apereo lawyers.


Here is the slightly longer section from the „Apereo Licensing & Intellectual Property Practices“ page:

> Third-Party Content
>
> Third-party content (source code, binary artifacts like libraries, documentation, etc.) is anything that was not
> submitted directly to Apereo or that is not covered under an ICLA, CCLA, or SGLA. Apereo projects can use third-party
> content as long as the project is in compliance with the license under which the content is acquired, and as long as
> use of the third-party content does not violate Apereo's own licensing and policies. Use of any third-party content
> not available under one of the licenses on ASF's "Category A" list must first be reviewed by the project’s governance
> body since this has implications for how the derivative work can be used. See the Apereo documentation on Third-Party
> Licenses for more details.
>
> Third-party content should be handled much differently from Apereo content. Copyright and license notices inside
> source files should not be modified and the standard Apereo source header should not be added. Licenses for the
> third-party content should be included with the distribution and appropriate notices should be included in the NOTICE
> file. Minor modifications to third-party sources should be licensed under the same terms as the original third-party
> source, while major modifications to third-party sources should be handed on a case-by-cases by the project's
> governance body.


How To Label Third Party Libraries
----------------------------------

Third party libraries that are used within an Opencast system in production need to be listed in the `NOTICES` file,
which can be found in the root directory of our code repository. Libraries and tools that are only used for testing or
building the project, or are run via system call, do not need to be listed (e.g. maven, maven plugins, junit, ...).


### JavaScript Libraries

For JavaScript libraries, list the file or folder that is included and give a short statement about the copyright and
license. This can usually be taken from the copyright header at the top of the library.

Example:

    modules/matterhorn-admin-ui-ng/src/main/webapp/lib/angular/*
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
