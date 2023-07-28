Developer Tips
==============

Common Developer Pitfalls
-------------------------

Every development environment has its quirks, so here are a few which have been collected by the community:

### General
* Build Opencast, then open your IDE.  Opencast generates a number of classes as part of its build process (eg:
  QSnapshotDto), which will not be found by your IDE and thus cause build errors.  It is possible to get your IDE
  to run the appropriate Maven lifecycle event, but that can be hit-and-miss in terms of functionality.
* If your IDE stubbornly refuses to acknowledge that a class exists, even when you're sure it's there, try closing your
  IDE, then running `git clean -fdx`, then building.  This will ensure everything in your clone is up to date.  Also
  ensure you find your project workspace to make sure your IDE isn't keeping a cache of things.
* Check if you have selected the correct java version in your IDE. Opencast requires Java 11, but your IDE might have
  selected a different version.
* Absolute worst case, remove your Maven cache (typically ~/.m2), and possibly your Node cache (typically ~/.npm) and
  repeat the above steps.  This is completely starting from scratch.

### IntelliJ
* Do not use the prebuilt indexes.

Developer Builds
----------------

Besides the default `dist` Maven profile, the assemblies project defines a second `dev` profile which will cause only
one `allinone` distribution to be created. It is already unpacked and ready to be started. Activate the profile using:

    mvn clean install -Pdev

The administrative user interface needs nodejs to build and phantomjs for testing purposes. These will be downloaded as
prebuilt binaries during the maven build process. If there are no prebuilt binaries for your operating system, you can
build the tools manually and then build opencast using the `frontend-no-prebuilt` maven profile:

    mvn clean install -Pdev,frontend-no-prebuilt

Logging During Builds
---------------------

While building Opencast, the default log level for Opencast modules is `WARN`. To increase logging for development,
edit the log level configuration in `docs/log4j/log4j.properties`.

Building single modules
-----------------------

When working on a single Opencast module, it can be extremely helpful to watch the newly built version and include
it automatically in the Opencast OSGi infrastructure. This can be done through the
[bundle:watch](https://karaf.apache.org/manual/latest/commands/bundle-watch.html) command in Karaf. The workflow would
be as follows:

* Start Opencast and use `la -u` in the Karaf console to list all installed bundles/modules. Note down the IDs of the
  bundles you want to watch.
* Use `bundle:watch IDs` to watch the desired modules, e.g. `bundle:watch 190 199`
* Make your changes and rebuild the module (e.g. execute `mvn clean install` in the module folder).
* Watch how Karaf automatically redeploys the changed jars from your local Maven repository. You can verify that
  everything went smoothly by checking the log with `log:tail`.

To see this technique in action, you can watch the following short video:

* [Opencast development: Watch and reload modules](https://asciinema.org/a/348132)

The updated bundles are only available in the currently running Karaf instance. To create a Opencast version that has
this changes permanently, you have to run `mvn clean install` in the the assemblies directory again. Your current
instance will be deleted by the new assembly!

In several cases the `bundle:watch` can bring Karaf in an unstable condition, as dependencies between bundles will not
correctly be restored, after the new bundle has been deployed.



Specific development environments tips
--------------------------------------


### Ubuntu

#### Update System

```sh
$ apt update
$ apt upgrade -y
```

#### Install Packages via APT

```sh
$ apt install -y git openjdk-11-jdk maven gcc g++ build-essential cmake curl sox hunspell synfig ffmpeg
```

#### Install NodeJS (optional)

```sh
$ curl -sL https://deb.nodesource.com/setup_10.x | sudo -E bash -
$ sudo apt-get install -y nodejs
```

#### Install and start Elasticsearch with Docker

You can use `docker-compose` to easily run Elasticsearch:

```sh
$ cd docs/scripts/devel-dependency-containers
$ docker-compose up -d
```

To shut the services down again, run:

```sh
$ cd docs/scripts/devel-dependency-containers
$ docker-compose down
```


#### Set System Java JDK

Choose the Java Version 11 by entering:

```sh
$ update-alternatives --config java
```


### Fedora


#### Update System

```sh
$ dnf update -y
```

#### Install Dependencies

```sh
$ dnf install https://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm https://download1.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm -y
$ dnf group install 'Development Tools'
$ dnf install -y ffmpeg maven tesseract hunspell sox synfig unzip gcc-c++ tar bzip2 nodejs
```


### macOS


#### Update System

Try to install all updates via the App Store or system settings.

#### Java JDK 11

Install the JDK 11
It's recommended to use [SDKMAN](https://sdkman.io/) to install and manage Java versions.

#### XCode

Install XCode over the App Store. It will be needed for building and for git.

#### Install Packages via Homebrew

The Homebrew Project adds a package manager to Mac OS. You can install it by:

```sh
$ /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
```

You can now install needed packages:

```sh
$ brew install maven ffmpeg nodejs
```

#### Git Bash Completion

In macOS you can not complete or suggest half typed commands with your Tab Key (like you probably know from linux).
If you want to use bash completion, you have to install it by:

```sh
$ brew install bash-completion
```

Find the location of the configuration file

```sh
$ sudo find / -type f -name "git-completion.bash"
```

Normally it should be in

    $ cp /Library/Developer/CommandLineTools/usr/share/git-core/git-completion.bash /usr/local/etc/bash_completion.d/

Then add following line to the `bash_profile` in home

    [ -f /usr/local/etc/bash_completion ] && . /usr/local/etc/bash_completion

Finally, apply your changes with

    $ source /usr/local/etc/bash_completion.d/git-completion.bash
