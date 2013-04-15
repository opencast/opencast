General instructions:

FlexBuilder + maven:
----------------------

Project uses flexmojos in order to build and maintain the code with maven. (http://code.google.com/p/flex-mojos/)


After checkout please run

mvn org.sonatype.flexmojos:flexmojos-maven-plugin:3.3.0:flexbuilder

in your matterhorn-engage-player folder.


After this you should be able to include the project in Eclipse.


-------------------


If you use maven for testing instead of FlexBuilder:

Make sure that Flash Player is available on your clathpath; 
Important to run the FlexUnitTests

on MacOSX for example:
export PATH=$PATH:/pathtoflashplayer/Flash\ Player.app/Contents/MacOS



---------------------------
IF MAVEN ISN`T YOUR CHOICE FOR WORKING WITH FLEX/AJAX/HTML


0. Check out as a project in the workspace

1. Flex Project Nature --> Add Flex Project Nature

2. Properties --> Flex Build Path --> Main source folder: "src/main/flex/Videodisplay"

3. .actionScriptProperties --> 2x "main.mxml" in "Videodisplay.mxml"

4. Properties --> Flex Build Path --> Add Folder --> "src/main/resources"

5. Properties --> Flex Build Path --> Add Folder --> "src/main/html/html-debug"

6. Properties --> Flex Compiler --> Require Flash Player version: "10.0.0"

7. Right click html-template --> Replace With --> Latest from repository

8. Delete File main.mxml

9. Properties --> Flex Build Path --> Output folder: --> <any_folder_of_your_local_server>

10. Properties --> Flex Build Path --> Output folder: --> <the_localhost_url_your_server_points_to>

11. Update your Flex Build Path and add modules/shared-resources and modules/matterhorn-engage-ui/src/main/resources/ui



------
CODE DOCUMENTATION:

Maven will create the docs for this project

mvn site

creates the 'site' folder in the /target directory


------
CODE FORMATING:
FlexCode formater
http://sourceforge.net/projects/flexformatter/

------
RUN JavaScript Tests:

mvn -P jstests test


------
CODECOVERAGE report for Javascript

<config filename>-coverage.dat will be created inside the folder /src/test/resources

html formated report can be created for example by LCOV (http://ltp.sourceforge.net/coverage/lcov.php)

genhtml <config filename>-coverage.dat -o /home/yourchoice









