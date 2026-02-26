Coverage Reports
================

Ensuring adequate test coverage is an important part of our development process.  While we do not yet have minimum
coverage levels defined, your goal as a developer is to ensure your code is always fully covered.


A note on the module weirdness
------------------------------

Our coverage reports in the maven site output are blank.  This is due to the way Maven handles multi-module reports,
and is docuemnted in [Jacoco's docs](https://github.com/jacoco/jacoco/wiki/MavenMultiModule).


Local Coverage Reports
----------------------

Measuring code coverage can be done locally with `./mvnw install jacoco:report jacoco:report-aggregate`, then the
reports are in `modules/jacoco-reports/target/site/jacoco-aggregate/index.html`


Branch Coverage Reports
-----------------------

The current [coverage reports](https://opencast.github.io/coverage-reports/) contain the coverage reports from all
of the current branches.  The coverage report for each branch can be found at 
`https://opencast.github.io/coverage-reports/<branchname>/modules/opencast-jacoco-reports/jacoco-aggregate/index.html.`
