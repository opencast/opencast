# Developer installation guide

These instructions outline how to install Opencast on a Linux system. Currently guided flavors are Fedora 30, Ubuntu 18.04 and Mac OS 10.14.

<br/>

## TL;DR
--------------------

    $ git clone https://github.com/opencast/opencast.git
    $ cd opencast && mvn clean install -Pdev
    $ cd build/opencast-dist-develop-*/bin && ./start-opencast

You can find the default Admin UI at: [localhost](http://localhost:8080/admin-ng/index.html)

Default credentials are:

- username: admin
- password: opencast

<br/>

## Configuring Git (optional)
--------------------

    $ git config --global user.name "Example Name"
    $ git config --global user.email example@domain.com
    $ ssh-keygen -t rsa -b 4096 -C "example@domain.com"
    $ cat ~/.ssh/id_rsa.pub

Go to: [Github](https://github.com/settings/keys), click "New SSH Key" and paste your content of id_rsa.pub into the input field. It should look like:

    ssh-rsa at90k0PY+z7mTyLB7UZXDnmpNHkU/MzOqpOHlEf1fCPViDYMXcFYeUMw0O/q0tR69TqQvwnFZuat90k0PY+z7mTyLB7UZXDnmpNHkU/MzOqpOHlEf1fCPViDYMXcFYeUMw0O/q0tR69TqQvwnFZuat90k0PY+z7mTyLB7UZXDnmpNHkU/MzOqpOHlEf1fCPViDYMXcFYeUMw0O/q0tR69TqQvwnFZuat90k0PY+z7mTyLB7UZXDnmpNHkU/MzOqpOHlEf1fCPViDYMXcFYeUMw0O/q0tR69TqQvwiodajsiodjaaosdiasdjsaddioasjosij== example@domain.com

Now press "Add SSH Key" and return to your terminal and:

    $ ssh -T git@github.com
    $ yes <enter>

<br/>

## Clone Opencast
--------------------

You can get the Opencast source code by cloning the Git
repository.

Cloning the Git repository:

    $ git clone https://github.com/opencast/opencast.git

<br/>

## Install Dependencies
--------------------

### General Information
Please make sure to install the following dependencies.

Required:

    java-1.8.0-openjdk-devel.x86_64 / openjdk-8-jdk (other jdk versions untested / Oracle JDK strongly not recommended)
    ffmpeg >= 3.2.4
    maven >= 3.1
    python >= 2.6, < 3.0
    firefox/chrome/some other major browser
    unzip
    gcc-c++
    tar
    bzip2

Required as a service for running Opencast:

    ActiveMQ >= 5.10 (older versions untested)

Required for some services. Some tests may be skipped and some features
may not be usable if they are not installed. Hence, it's generally a good idea to
install them.

    tesseract >= 3
    hunspell >= 1.2.8
    sox >= 14.4
    synfig

<br/>

## Ubuntu 18.04
--------------------

### Update System

    $ sudo apt update && sudo apt upgrade -y

### Install Packages via APT

    $ sudo apt install -y git openjdk-8-jdk maven gcc g++ build-essential cmake curl sox hunspell synfig ffmpeg

### Install NodeJS (optional)

    $ curl -sL https://deb.nodesource.com/setup_10.x | sudo -E bash -
    $ sudo apt-get install -y nodejs 
    $ sudo npm install -g eslint

### Install Chrome

    $ cd && cd Downloads && wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && sudo dpkg -i google-chrome-stable_current_amd64.deb

### Set System Java JDK

Choose the Java Version 1.8.0 by entering:

    $ sudo update-alternatives --config java

### Set the JAVA_HOME Variable

Open your .bashrc

    $ cd && nano .bashrc

and paste following content at the end of the file:

    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

<br/>

## Fedora 30
--------------------

### Update System

    $ sudo dnf update -y

### Install Packages via DNF and RPM Fusion

    $ sudo dnf install https://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm https://download1.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm -y

    $ sudo dnf group install 'Development Tools' -y && $ sudo dnf install -y java-1.8.0-openjdk ffmpeg maven tesseract hunspell sox synfig unzip gcc-c++ tar bzip2


### Install NodeJS (optional)

    $ sudo dnf install -y nodejs
    $ sudo npm install -g eslint

### Install Chrome

    $ cd && cd Downloads && wget https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm && sudo dnf install google-chrome-stable_current_x86_64.rpm -y

<br/>

## Mac OS 10.14
--------------------

### Update System

Try to install all updates via the App Store or the Apple Icon on the top left corner.

### Java JDK 8

Install the JDK 8 by downloading it from https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

### XCode

Install XCode over the App Store. It will be needed for building and for git.

### Install Packages via Homebrew

The Homebrew Project adds a package manager to Mac OS. You can install it by:

    $ /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"

You can now install needed packages:

    $ brew install maven ffmpeg 

### Install NodeJS (optional)

    $ brew install nodejs
    $ sudo npm install -g eslint

### ActiveMQ with Homebrew

Homebrew offers you an ActiveMQ Package. Please decide, if you want to use Homebrew for ActiveMQ or if you want to follow the general guide below and run it by downloading the binaries. If you want continue you can install ActiveMQ by

    $ brew install activemq

Remember to copy the activemq.xml like mentioned below in the right directory. You have to find the right folder for that operation, Homebrew will put the ActiveMQ files in a different location. You could find it by

    $ sudo find / -name activemq.xml

After changing the configuration file you can list and start or stop you services with

    $ brew services list
    $ brew services start activemq

### Git Bash Completion

In Mac OS you can not complete or suggest half typed commands with your Tab Key (like you probably know from linux). If you want to use bash completion, you have to install it by

    $ brew install bash-completion

Find the location of the configuration file

    $ sudo find / -type f -name "git-completion.bash" 

Normally it should be in

    $ cp /Library/Developer/CommandLineTools/usr/share/git-core/git-completion.bash /usr/local/etc/bash_completion.d/

Then add following line to the bash_profile in home

    [ -f /usr/local/etc/bash_completion ] && . /usr/local/etc/bash_completion

Finally apply your changes with

    $ source /usr/local/etc/bash_completion.d/git-completion.bash

<br/>

## Install and Configure ActiveMQ
--------------------

Download the current version from https://activemq.apache.org/components/classic/download

Extract and copy it to a directory, in this case you could use the opt directory.

    $ sudo tar -zxvf apache-activemq-*-bin.tar.gz -C /opt
    $ cd /opt && sudo mv apache-activemq-*/ activemq

Copy the preconfigured XML from your opencast directory into your ActiveMQ configuration. In this example you have following folder structure:

- ~/Projects/opencast 
- /opt/activemq

With that folder structure you could use following command:

    $ cd && cd Projects && sudo cp opencast/docs/scripts/activemq/activemq.xml /opt/activemq/conf/activemq.xml

If your folder structure is different from that example or you do decide to put it somewhere else, you should copy and replace the preconfigured XML from

- /location/to/your/opencast/docs/scripts/activemq/activemq.xml

into

- /location/to/your/activemq/conf/activemq.xml

You can start your ActiveMQ instance with:

    $ sudo ./location/to/your/activemq/bin/activemq start

<br/>

## Build and Start Opencast
--------------------

You can build now opencast by changing your directory into your opencast location and by running:

    $ mvn clean install

After the successfully compilation you can start opencast with:

    $ cd build/opencast-dist-develop-*/bin && ./start-opencast

The `-Pdev` argument decreases the build time and skips the creation of multiple tarballs and turning on the developer tarball.

    $ cd opencast && mvn clean install -Pdev
    $ cd build/opencast-dist-develop-*/bin && ./start-opencast

For further information visit [Development Environment](../development-environment.md).

### Useful Commands for Testing Purposes

For a quick build, you can use the following command to skip Opencast's tests.

    $ cd opencast
    $ mvn clean install -Pdev -DskipTests=true

To see the whole stacktrace of the installation you can use the following command to disable the trimming.

    $ cd opencast
    $ mvn clean install -DtrimStackTrace=false

If you want to start opencast in debug mode, you could use the debug argument:

    $ cd build/opencast-dist-develop-*/bin && ./start-opencast debug

<br/>

## Modify Code and Build Changes
--------------------

After you modified your code you can go back to step "Build and Start Opencast" to rebuild Opencast.

<br/>

## Common Build Errors or Fixes
--------------------

### NPM Access Error

To fix an npm access error ([example](https://stackoverflow.com/questions/16151018/npm-throws-error-without-sudo)), you can run

    $ sudo chown -R $USER:$(id -gn $USER) ~/.config && sudo chown -R $USER:$(id -gn $USER) ~/.npm

### JDK Version

Some IDEs attempt to use the most recent version of the JDK. Make sure that your IDE is configured to use JDK 1.8.0.

### Waiting for ActiveMQ

Opencast requires ActiveMQ to be both running and properly configured, otherwise it will wait forever to connect. See [here](#install-and-configure-activemq) for details on how to configure ActiveMQ. Make sure, that ActiveMQ runs without errors and with the right JAVA_HOME Variable (explained [here](#set-the-javahome-variable)).

### Slow Idea Fix

Edit following file

    $ sudo nano /etc/sysctl.conf

and copy this text into it

    fs.inotify.max_user_watches = 524288

Apply your changes with

    $ sudo sysctl -p --system

<br/>

## Intellij Idea IDE Community Edition (optional)
--------------------

If you are currently on Fedora, you can install it with following command.
Make sure, that the versions match, you probably have to change it depending on the most current version.

    $ cd && cd Downloads && wget https://download.jetbrains.com/idea/ideaIC-2019.2.tar.gz
    $ sudo tar -zxvf ideaIC-*.tar.gz -C /opt
    $ cd /opt && sudo mv idea-IC-*/ idea && sh /opt/idea/bin/idea.sh

Otherwise install it by downloading and following the manufacturer guide, select Community Edition:

[IDEA Intellij Community Edition](https://www.jetbrains.com/idea/download/)

Follow the next steps, if you want to import opencast correctly

- Import project from external model
- Choose Maven
- Search for projects recursively
- Uncheck all listed profiles
- Check all projects to import
- Make sure not to select JDK 11, please select JDK 1.8.0, it should be somewhere around /usr/lib/jvm/java-1.8.0-openjdk depending on your current system

Now Idea should import the projects, it could take some time, you can make it faster by following [this](#slow-idea-fix). 

Import the opencast code style configuration by following the steps

- Go to settings
- Search for code style
- You should find it under Editor->Code Style
- Select Java and click on the gear icon
- Select Import Scheme and Intellij IDEA code style XML
- Import it from opencast/docs/intellij_settings/codestyle.xml

Now your IDE should be ready for developing.

<br/>

## Visual Studio Code Editor (optional)
--------------------
If you are currently on Fedora, you can install it with

    $ cd && cd Downloads && sudo rpm --import https://packages.microsoft.com/keys/microsoft.asc && sudo sh -c 'echo -e "[code]\nname=Visual Studio Code\nbaseurl=https://packages.microsoft.com/yumrepos/vscode\nenabled=1\ngpgcheck=1\ngpgkey=https://packages.microsoft.com/keys/microsoft.asc" > /etc/yum.repos.d/vscode.repo' && dnf check-update && sudo dnf install code -y

Otherwise install it by downloading and following the manufacturer guide:

[Visual Studio Code](https://code.visualstudio.com/)

After installation you can open a folder in bash with

    $ code .

Recommended Extensions are

- ESLint by Dirk Baeumer
- AngularJs 1.x Code Snippets by alexandersage
- Debugger for Chrome by Microsoft
