# In Construction

Attaching a Remote Debugger to Karaf
------------------------------------

To debug a running Opencast system, you can attach a remote debugger in your IDE (Eclipse or NetBeans, i.e.). For that
you have to enable the remote debugging in Karaf OSGI server that runs Opencast.

You have to add "debug" as an additional paramenter to the Opencast start script:

    bin/start-opencast debug

If you want to enable debug permanently you can export the variable in the shell:

    export DEFAULT_JAVA_DEBUG_OPTS='-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005'

You can connect the remote debugger of your IDE on port `5005`.

### Enabling debugger for package installations

Albeit you can use the afforemented method for package installations, you can't start debug mode via system services.
The recommended way is to enable the debug mode in the `setenv` file, normally found in:

    /usr/share/opencast/bin/

And add this line:

    export KARAF_DEBUG=true

In case you need to change the port for debbuging, you can adding this another line:

    export JAVA_DEBUG_PORT={{PORT}}

Where `{{PORT}}` is the desired port.


***
For more information on remote debugging with Karaf you can visit [this
site.](https://karaf.apache.org/manual/latest/#_debugging)

It is **not recommended** to enable remote debugging on production systems!

Adding Dummy Capture Agent
---------------------------

In the case that is needed, you can create a dummy capture agent with testing porpurses.
To add a dummy CA, go to the API Docs page in Opencast and enter a new capture agent in:

    {opencast-url}/docs.html?path=/capture-admin#setAgentStateConfiguration-4


  **Name:** Any name of your desire.


  **Configuration:**

    {
    "key": "capture.device.names",
    "value": "presentation,presenter"
    }

Click on `submit` and is ready to go.

Additionaly you can call this REST endpoint directly using `CURL` for example.