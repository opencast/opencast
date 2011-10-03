REM ##
REM # Configure these variables to match your environment
REM # If you have system-wide variables for FELIX_HOME and M2_REPO then you
REM # should not have to make any changes to this file.
REM ##

REM # SET FELIX_HOME=E:/Libraries/felix-1.8.0
REM # SET M2_REPO=C:/Users/cab938/.m2/repository

REM ##
REM # Deploy without tests since test fail on windows
REM ##

cd ..
mvn clean install -DdeployTo=%FELIX_HOME%/load -DskipTests
