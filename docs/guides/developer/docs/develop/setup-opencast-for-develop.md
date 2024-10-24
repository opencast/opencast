Developer Environment Setup
============================

These instructions outline how to install Opencast.
This is meant for developers.
For the installation of a production cluster, take a look at the admin guides.


Warning: This probably won´t work with Windows.



## TL;DR


```sh
$ git clone https://github.com/opencast/opencast.git
$ cd opencast
$ ./mvnw clean install -Pdev
$ cd build/opencast-dist-develop-*
$ ./bin/start-opencast
```

Opencast will then listen on [127.0.0.1:8080](http://localhost:8080)

Default credentials are:

- username: admin
- password: opencast

Where `*` is the version number of the distribution.

## Clone Opencast


You can get the Opencast source code by cloning the Git repository.

Cloning the Git repository:

```sh
$ git clone https://github.com/opencast/opencast.git
```


## Install Dependencies


Please make sure to install the following dependencies.

Required:

    java-17-openjdk-devel
    ffmpeg >= 3.2.4
    maven >= 3.6
    python
    firefox/chrome/chromium
    unzip
    gcc-c++
    tar
    bzip2

Required as a service for running Opencast:

    elasticsearch = 7.9.x and analysis-icu plugin

Required for some services. Some tests may be skipped and some features
may not be usable if they are not installed. Hence, it's generally a good idea to
install them.

    tesseract >= 3
    hunspell >= 1.2.8
    sox >= 14.4
    synfig


## Build Opencast


You can now build opencast by changing into your opencast directory and running:

    $ ./mvnw clean install [Options]

After the successful compilation you can start opencast with:

    $ ./build/opencast-dist-develop-*/bin/start-opencast

Where `*` is the version number of the distribution.

### Build options

#### Assembly profiles

Besides the default `dist` Maven profile, you can specify other profiles to create different builds.
Activate these profiles using:

profile | description
--------|------------
`-Pdev`   | Create a single `allinone` distribution and unpack it
`-Pnone`  | Do not create any distribution

You can only use one of these profiles at a time.

#### Build options

The following options can be used to customize the build process. They can be used in combination with the assembly profiles.

Option | Description
-------|------------
`-DtrimStackTrace=false` | Do not trim stack traces in the logs
`-DskipTests=true` | Skip tests
`-T 1.0C` | Use multiple threads for building (Experimental)

### Build Single Modules

When working on a single Opencast module, it can be extremely helpful having the new built version automatically
included in the Opencast OSGi infrastructure. This can be achieved by watching the module with the
[bundle:watch](https://karaf.apache.org/manual/latest/commands/bundle-watch.html) command in Karaf.
The procedure would be as follows:

- Start Opencast and use `la -u` in the Karaf console to list all installed bundles/modules. Note down the IDs of the
  bundles you want to watch.
<<<<<<< HEAD
- Use `bundle:watch IDs` to watch the desired modules, e.g. `bundle:watch 190 199`
- Make your changes and rebuild the module (e.g. execute `./mvnw clean install` in the module folder).
- Watch how Karaf automatically redeploys the changed jars from your local Maven repository. You can verify that
  everything went smoothly by checking the log with `log:tail`.

To see this technique in action, you can watch the following short video:

- [Opencast development: Watch and reload modules](https://asciinema.org/a/348132)

The updated bundles are only available in the currently running Karaf instance. To create an Opencast version that contains
your changes permanently, you have to run `./mvnw install` in the `assemblies` directory again.

In several cases the `bundle:watch` can put Karaf in an unstable condition, as dependencies between bundles will not
correctly be restored after the new bundle has been deployed.


### Examples
#### Build enabling multiple threads (Experimental)

Building with multiple threads decreases the build time significantly.
If you want to enable multiple threads, you can use the following command:

    $ ./mvnw clean install -T 1.0C -DskipTests -Pnone 
    && cd assemblies && ./mvnw install -T 1.0C -Dskiptests -Pdev  
    && cd ..
    $ ./build/opencast-dist-develop-*/start-opencast

Multiple threads build have not been thoroughly tested and may cause runtime problems or unexpected behavior.
We don't advise using this feature for production.

#### Useful Commands for Testing Purposes

For a quick build, you can use the following command to skip Opencast's tests.

    $ cd opencast
    $ ./mvnw clean install -Pdev -DskipTests

To see the whole `stacktrace` of the installation you can use the following command to disable the trimming.

    $ cd opencast
    $ ./mvnw clean install -DtrimStackTrace=false

If you want to start opencast in debug mode, you could use the debug argument:

    $ cd build/opencast-dist-develop-*/bin && ./start-opencast debug


## Common Build Errors or Fixes

### NPM Access Error

To fix a NPM access error ([example](https://stackoverflow.com/questions/16151018/npm-throws-error-without-sudo)),
 you can run

    $ sudo chown -R $USER:$(id -gn $USER) ~/.config && sudo chown -R $USER:$(id -gn $USER) ~/.npm

### JDK Version

Some IDEs attempt to use the most recent version of the JDK. Make sure that your IDE uses JDK 11.

## Recommended Development Tools

While you can use any IDE or editor you like, we recommend the following tools.

### IntelliJ IDEA IDE Community Edition

Install it by downloading and following the manufacturer guide, select Community Edition:

[IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/)


#### Import Opencast

Follow the next steps, if you want to import opencast correctly

- Import project from external model
- Choose Maven
- Search for projects recursively
- Uncheck all listed profiles
- Check all projects to import
- Select JDK 17, it should be somewhere around `/usr/lib/jvm/java-17-openjdk` depending on your current system

Now Idea should import the projects, it could take some time, you can make it faster by following [this](#slow-intellij-idea-fix).


#### Setup Code Style

Import the opencast code style configuration by following the steps

- Go to settings
- Search for code style
- You should find it under Editor → Code Style
- Select Java and click on the gear icon
- Select Import Scheme and IntelliJ IDEA code style XML
- Import it from `opencast/docs/intellij_settings/codestyle.xml`

#### Setup CheckStyle

Check style is one of the tools we use to ensure code quality. Not following the check style rules will produce build errors.
To use it in IntelliJ, follow these steps:

- Install the [CheckStyle-IDEA plugin](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea)
- Configure the plugin:
    1. Go to settings → Tools → Checkstyle
    2. Click on the plus icon
    3. Add a description
    4. Select “Use a local Checkstyle file“
    5. Fill the blanks:
        - checkstyle.header.file: `/docs/checkstyle/checkstyle-header.txt`
        - checkstyle.supressions.file: `/docs/checkstyle/checkstyle-suppressions.xml`

To use the plugin, you can run manually the check-style plugin through the menu View → Tool Windows → Checkstyle.



Now your IDE should be ready for developing.

#### Slow IntelliJ IDEA Fix

Edit following file

    $ sudo nano /etc/sysctl.conf

and copy this text into it

    fs.inotify.max_user_watches = 524288

Apply your changes with

    $ sudo sysctl -p --system


### Visual Studio Code Editor

We use VS code for mainly:

- Frontend development
- Configure files
- Markdown files
- And other simple tasks.

Install it by downloading and following the [manufacturer guide](https://code.visualstudio.com/):

Recommended Extensions are:

- ESLint by Dirk Baeumer
- AngularJS 1.x Code Snippets by alexandersage
- Debugger for Chrome by Microsoft
