@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION

REM ##
REM # Configure these variables to match your environment
REM # If you have system-wide variables for FELIX_HOME and M2_REPO then you
REM # should not have to make any changes to this file.
REM ##

REM ## NOTA BENE:
REM ## Those programmers who, like me, are unfamiliar with the batch programming 
REM ## constructs, may be shocked of the use of a 'for' loop when it does not
REM ## seem necessary. For them, I would like to clarify that there is other way 
REM ## (but using a for loop) to assign the output of a command to a variable.


REM ## Try to guess the latest Java JDK location, if not already set
if not defined JAVA_HOME (
   REM ## Search java directory
   if exist "%PROGRAMFILES%\Java" (
      set search_dir=%PROGRAMFILES%\Java
   ) else (
      set search_dir=%PROGRAMFILES%
   )

   REM ## Chooses the directory starting with jdk with the highest alphabetical order (= latest version)
   for /f "delims=" %%f in ('dir "%search_dir%\jdk*" /b /o-n') do (
      set JAVA_HOME=%search_dir%\%%f
      REM ## Exits after the first coincidence 
      goto break_javahome
   )

   :break_javahome
   REM ## If we get to here and JAVA_HOME is still not defined, exit with an error
   if not defined JAVA_HOME (
      echo JAVA_HOME is not defined!!! >&2
      exit /b 1
   )
)

REM ## Try to guess the latest Maven installation, if not already set
if not defined M2_HOME (
   REM ## Searches a directory under Program Files, starting by "apache-maven"
   for /f "delims=" %%f in ('dir "%PROGRAMFILES%\apache-maven*" /b /ad /s /o-n') do (
      set M2_HOME=%%f
      goto break_m2home
   )
   
   :break_m2home
   REM ## If we get to here and M2_HOME is still not defined, exit with an error
   if not defined M2_HOME (
      echo M2_HOME is not defined!!! >&2
      exit /b 1
   )
)

REM ## Assumes the Maven repository is in its default location
if not defined M2_REPO set M2_REPO=%USERPROFILE%\.m2\repository

REM ## Try to guess the latest Felix installation
if not defined FELIX_HOME (
   REM ## Searches a directory named felix-framework-* under the home drive
   for /f "delims=" %%f in ('dir "%HOMEDRIVE%\felix-framework-*" /b /s /ad /o-n') do (
      set FELIX_HOME=%%f
      goto break_felixhome
   )

   :break_felixhome
   if not defined FELIX_HOME (
      echo FELIX_HOME is not defined!!! >&2
      exit /b 1
   )
)

SET OPENCAST_LOGDIR=%FELIX_HOME%\logs

REM # To enable the debugger on the vm, enable all of the following options
SET DEBUG_PORT=8000
SET DEBUG_SUSPEND=n
REM SET DEBUG_OPTS=-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=%DEBUG_PORT%,server=y,suspend=%DEBUG_SUSPEND%

REM ##
REM # Only change the lines below if you know what you are doing
REM ##

SET FELIX_WORK_DIR=%FELIX_HOME%\work
SET FELIX_CONFIG_DIR=%FELIX_HOME%\etc
SET FELIX_OPTS=-Dfelix.home=%FELIX_HOME% -Dfelix.work=%FELIX_WORK_DIR%
SET FELIX_CONFIG_OPTS=-Dfelix.config.properties="file:%FELIX_CONFIG_DIR%/config.properties" -Dfelix.system.properties="file:%FELIX_CONFIG_DIR%/system.properties"
SET MAVEN_ARG=-DM2_REPO="%M2_REPO%"
SET FELIX_FILEINSTALL_OPTS=-Dfelix.fileinstall.dir="%FELIX_CONFIG_DIR%\load"
SET JMX_OPTS=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false
SET PAX_CONFMAN_OPTS=-Dbundles.configuration.location="%FELIX_CONFIG_DIR%"
SET PAX_LOGGING_OPTS=-Dorg.ops4j.pax.logging.DefaultServiceLog.level=WARN -Dopencast.logdir="%OPENCAST_LOGDIR%"
SET ECLIPSELINK_LOGGING_OPTS=-Declipselink.logging.level=SEVERE
SET UTIL_LOGGING_OPTS=-Djava.util.logging.config.file="%FELIX_CONFIG_DIR%\services\java.util.logging.properties"
SET FELIX_CACHE=%FELIX_WORK_DIR%\felix-cache
SET GRAPHICS_OPTS=-Djava.awt.headless=true -Dawt.toolkit=sun.awt.HeadlessToolkit

REM # Make sure matterhorn bundles are reloaded
if exist "%FELIX_CACHE%" (
   rmdir /s /q "%FELIX_CACHE%"
   if not exist "%FELIX_CACHE%" mkdir "%FELIX_CACHE%"
)

REM # Finally start felix

pushd "%FELIX_HOME%"
java %DEBUG_OPTS% %FELIX_OPTS% %FELIX_CONFIG_OPTS% %GRAPHICS_OPTS% %MAVEN_ARG% %FELIX_FILEINSTALL_OPTS% %PAX_CONFMAN_OPTS% %PAX_LOGGING_OPTS% %ECLIPSELINK_LOGGING_OPTS% %UTIL_LOGGING_OPTS% %JMX_OPTS% -jar "%FELIX_HOME%\bin\felix.jar" "%FELIX_CACHE%"
popd

ENDLOCAL