# Debugging in Opencast

This guide will show you how to debug Opencast.


## Debugging strategies

There are different ways to debug Opencast. You can debug the backend and the frontend.
The backend is the Java code while the frontend is the Angular code but soon to be replaced for Typescript.

### How to know what to debug?

First, you need to determine where the problem is. The frontend problems are mostly visible in the browser.
These problems normally are JavaScript errors or problems with the user interface.
The backend problems are mostly visible in the logs and the API calls. These problems are mostly exceptions or errors.

The frontend and the backend are connected. The frontend calls the backend via REST endpoints.
If the information from the API is not correct, is a problem of the backend.
If the information is correct but, the frontend is not showing the information correctly, is a problem of the frontend.






## Backend debugging

### Logging configuration

The logging configuration is in the `etc/org.ops4j.pax.logging.cfg` file.
You can change the logging level of a package by adding the following line:

    log4j.logger.org.opencastproject.capture=DEBUG

Where `org.opencastproject.capture` is the package name and `DEBUG` is the logging level.

The logging level can be one of the following:

* `TRACE`
* `DEBUG`
* `ERROR`
* `WARN`
* `INFO`


### Attaching a Remote Debugger

There are different ways to debug the backend of Opencast.
The easiest way is to attach a remote debugger here are some two ways to do that.

#### For source installations

To debug a running Opencast system, you can attach a remote debugger in your IDE (Eclipse, NetBeans, etc.). For that
you have to enable the remote debugging in Karaf OSGI server that runs Opencast.

You have to add "debug" as an additional parameter to the Opencast start script:

    bin/start-opencast debug

If you want to enable debug permanently you can export the variable in the shell:

    export DEFAULT_JAVA_DEBUG_OPTS='-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005'

You can connect the remote debugger of your IDE on port `5005`.

#### For package installations

Albeit you can use the afforemented method for package installations, you can't start debug mode via system services.
The recommended way is to enable the debug mode in the `setenv` file, normally found in:

    /usr/share/opencast/bin/

And add this line:

    export KARAF_DEBUG=true

In case you need to change the port for debugging, you can add another line:

    export JAVA_DEBUG_PORT={{PORT}}

Where `{{PORT}}` is the desired port.


***
For more information on remote debugging with Karaf you can visit [this
site.](https://karaf.apache.org/manual/latest/#_debugging)

Enabling remote debugging on production systems is **not recommended**!

### Adding Dummy Capture Agent


In the case that is needed, you can create a dummy capture agent with testing purposes.
To add a dummy CA, go to the API Docs page in Opencast and enter a new capture agent in:

    {opencast-url}/docs.html?path=/capture-admin#setAgentStateConfiguration-4


  **Name:** Any name of your desire.


  **Configuration:**

    {
    "key": "capture.device.names",
    "value": "presentation,presenter"
    }

Click on `submit` and is ready to go.

Additional you can call this REST endpoint directly using `curl` for example.


## Frontend debugging

To debug the frontend you can use the browser developer tools. For most browsers this is bound to your F12 key by default.
In the developer tools you can find the tab `Sources`. In this tab you can find the source code of the frontend. You can
set breakpoints in the source code and debug the frontend.



## Enable the Karaf web console

The Karaf web console is a web interface to manage the OSGI bundles. You can enable the web console by adding the following:

In the `etc/org.apache.karaf.features.cfg` file add the following line:

    webconsole/4.2.15, \
    jaas/4.2.15, \

For security reasons, remember to disable the web console in production systems after you finished debugging.

## Automatic Module Reloading

See [Build Single Modules](development-environment.md#build-single-modules)


