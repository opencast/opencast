Compiling & installing 3rd party tools for Linux, Windows and Mac
=================================================================

To compile new versions of 3rd party libraries/tools, scripts
on this and underlying subdirectories must be executed.

This can be done in two ways:

1. interactively

Start top level script, which shows a menu:

./menu3p

and follow the steps in menu (starting with 0).

2. automatically (batch)

2a) compile everything for Linux/Mac

Define some required variables and start the script:

export HOME3P=<3rd_party_dir>   # use absolute path, not "."
export SUDOPWD=<sudo_password>  # if needed
./do-all

If you want a complete log of the compilation, use:
./do-all 2>&1 | tee do-all.log

At the end of compilation (approx. 30 minutes) you'll have 3rd
party tools installed in /usr/local/bin on Linux or /opt/local/bin
on Mac.

2b) cross-compile everything for Windows

Define some required variables and start the script:

export HOME3P=<3rd_party_dir>   # use absolute path, not "."
export SUDOPWD=<sudo_password>  # if needed
./do-all-win32

If you want a complete log of the cross-compilation, use:
./do-all-win32 2>&1 | tee do-all-win32.log

At the end of cross-compilation (approx. 30 minutes) you'll have 3rd
party tools for Windows packed in zip files ready for installation
on Windows.

+----------------------------------------------------------------+
| NOTE: Cross-compilation for Windows can be done only on Linux! |
+----------------------------------------------------------------+

If you want to do some steps manually, for example if an error
occurs during compilation, you need to define at least HOME3P
variable, which must point to the top level 3rd_party directory
and should be defined as an absolute path.

A note about root privileges/sudo password:

Scripts will execute privileged commands either:
- directly, if executed under root
- use supplied sudo password for sudo command
  If run interactively, sudo password is asked only once (no echo).
  sudo password can be empty, if the user is allowed to do sudo
  without asking for password.
- ask for sudo password whenever sudo command is encountered and
  again after sudo timeout is passed (typically after 5 minutes)


Operating Systems & environment
===============================

The compilation of 3rd party tools described here has been tested on
the following newly installed operating systems:

- 32-bit Linux
  CentOS 5.5-6.0, Red Hat Enterprise Linux Server 5.5-6.0,
  Ubuntu 10.04-11.10

- 64-bit Linux
  CentOS 5.5-6.0, Red Hat Enterprise Linux Server 5.5-6.0,
  Ubuntu 10.04-11.10

- Mac OS X
  Snow Leopard 10.6.* with Xcode 3.2.*
  Snow Lion 10.7.* with Xcode 4.1.*

Operating systems should be installed for a developer/development,
wherever possible.

All necessary prerequisites are downloaded and installed
automatically, except for the following utilities, which should
already be installed on the system:

- java
- gzip
- bzip2

On Mac OS X 10.7.* java runtime is installed as soon as it's
needed for the 1st time (via an interactive window), so the 1st run
of 3rd party scripts may fail. It can be repeated again as soon
as the java runtime is installed.

Packages download
=================

All packages needed for the compilation are downloaded from their
original locations. The locations and possible mirrors are specified
in "master" config file ($HOME3P/config.txt).

Typical entry in this config.txt has the following syntax:

  variable: value [another_value ...]

First the base URLs for most common mirrors are defined (like
CENTOS_MIRROR, EPEL_MIRROR, ...). These definitions can be used later on
as shell variables when defining URLs for packages.

Then come the definitions for packages (always in pairs):

  package_url: primary_url [mirror1_url ...]
  package_pkgs: package1.rpm [package2.rpm ...]

All packages listed in the <package_pkgs> are downloaded from the same
<package_url>. If download from the <primary_url> fails, next URL
in the URL list is tried (i.e. <mirror1_url>) and so on.

Similar syntax is used in all other package-specific config files
in subdirectories (see box below).

To check the availability of all needed packages, a script "check-urls"
is provided. This script parses "master" config file and all
package-specific config files and tries to download all packages listed.
No data is actually downloaded, just the availability is checked. If any
of the URLs are not valid/found/..., their number is reported at the end
of script execution. Problematic URLs are also listed in the file
"not_found.txt", where mirror URLs are marked with the keyword "mirror:"
before the URL.

If you want a complete log of the url check, use:
./check-urls 2>&1 | tee check-urls.log

Keywords to search for possible errors in log file are WARNING: and ERROR:


Description of interactive run
==============================

To do compilation step by step, run the top level script "menu3p".
You'll be prompted for action, which is a character(s) left of ")".
Default action is proposed in "[...]".

When action finishes executing, last lines on screen shows elapsed
time and success/failure of executed action. All actions and their
output is stored indefinitely in log files on /tmp. This is not
true for batch run (see above).

0) check & install prerequisites
   (script check-prereq)

All needed packages for successful compilation of 3rd party tools
are checked for presence and/or downloaded and installed, if needed.

Mingw32 cross-compile environment is downloaded from Fedora EPEL
repository and installed on all supported Linuxes. On Ubuntu the
mingw32 compiler is patched with the new LD_LIBRARY_PATH to satisfy
requirement for the only dependent library libgmp (see shell function
fix_libs()).

If you have different versions of required packages, you could/will
have problems compiling all 3rd party tools.

Note: URLs and names of needed packages are hard-coded in the script
(see shell functions install_prog() and install_mingw32())!

1) download sources
   (script download-sources)

This action checks, if all 3rd party source packages are downloaded
and if not, it downloads them from the locations specified in files
"config.txt" in each subdirectories. This is done by calling the
script download-sources, which either calls scripts with the same name
or does the specific download (by calling the shell function download(),
see below).

+--------------------------------------------------------------------+
| Syntax of config.txt                                               |
| --------------------                                               |
| For example for ffmpeg:                                            |
|                                                                    |
| URL: http://ffmpeg.org/releases                                    |
| PKG: ffmpeg-0.6.tar.gz                                             |
| SHA: 9e4bc2f1fdb4565bea54d7fb38e705b40620e208                      |
| DIR: ffmpeg-0.6                                                    |
| PCP1: pc-ffmpeg.zip                                                |
| PCP2: _ffmpeg.zip                                                  |
|                                                                    |
| - URL is location of the package (without package name)            |
| - PKG is the package name                                          |
| - SHA is expected SHA1 160-bit checksum of package (not required)  |
| - DIR is untarred/unzipped directory from package                  |
| - PCP is PC package zip name                                       |
|                                                                    |
| URL definition can have primary and mirrors URLs.                  |
| There can be up to 4 URL/PKG combinations (URL, URL1, URL2 & URL3) |
| and PC package zip names.                                          |
+--------------------------------------------------------------------+

To speed up compiling (if repeating the process) you can copy all
downloaded packages to $HOME3P/tarballs, from where they are taken if
present instead of downloading.

2) compile for Linux
   (script linux-compile)

Script linux-compile either calls scripts with the same name
or does the specific compilation and installation in /usr/local/...

Specific compilation consists of unpacking the source package,
pre-patching source, calling "configure", post-patching source,
calling "make/scons/jam", installing tool and post-patching installed
tool (if needed). When all is successful, a file .done-linux is
created so specific subdirectory can be skipped next time.

3) cross-compile for Windows (only on Linux)
   (script win32-compile)

Script win32-compile either calls scripts with the same name
or does the specific cross-compilation and installation in mingw32
specific areas.

Specific compilation consists of unpacking the source package,
pre-patching source, calling "configure", post-patching source,
calling "make/scons/jam", installing tool (done manually in mingw32
specific areas) and post-patching installed tool (if needed).
When all is successful, a file .done-win32 is created so specific
subdirectory can be skipped next time.

3z) create final packages for Windows

During the cross-compilation for Windows compiled tools are
copied into the $HOME3P/usr/local/... tree. After the cross-compilation
is finished, they can be stored in one zip package
(3rd_party_windows.zip) with this action. This zip is suitable for
direct installation on Windows (see later on).

4) compile for Mac
   (scripts mac-compile)

Not completed yet, but will have the same functionality as compile
for Linux.

5) compile for Mac with MacPorts
   (scripts ports-compile)

Script ports-compile either calls scripts with the same name
or does the specific compilation and/or installation in /opt/local/...

Where port from MacPorts exists, it's installed from MacPorts,
otherwise it's compiled the same way as on Linux (see above).
When all is successful, a file .done-ports is created so specific
subdirectory can be skipped next time.

a) do all for current platform
   (script do-all)

This script, which can be used also for batch run, calls all actions
for current platform in sequence: 0, 1 and 2 for Linux and
0, 1 and 5 for Mac.

At the end of compilation (approx. 30 minutes) you'll have 3rd
party tools installed in /usr/local/bin on Linux or /opt/local/bin
on Mac.

w) do all for Windows
   (script do-all-win32)

This script, which can be used also for batch run, calls all actions
for cross-compilation for Windows in sequence: 0, 1, 3 and 3z.

At the end of cross-compilation (approx. 30 minutes) you'll have 3rd
party tools for Windows packed in zip files ready for installation
on Windows.

l) view log of last action

Logs of executed actions are stored indefinitely in /tmp in the form
"menu3p-<action>.log.XXXXX". Log of last action can be seen here with
less/more.

r) reset intermediate marks

Removes all .done-linux and .done-win32 files, which are created when
specific library/tool is successfully compiled and installed. The
complete compilation can be done again after this.

c) clean up build directories
   (scripts clean-up)

Each compilation creates a build subdirectory from source tar/zip:

- <DIR>-linux for Linux
- <DIR>-win32 for Windows
- <DIR>-mac for Mac

<DIR> is read from file config.txt (see box above). All patches and
compilation occurs in these subdirectories so they can be easily
removed to start over with clean compilation.

This action calls script clean-up which either calls other scripts
with the same name or does the cleaning up of a specific subdirectory
(by calling the shell function cleanup(), see below).

ca) clean up all (including downloads)
    (scripts clean-up with parameter "all")

In addition to the actions from the step above the downloaded tars/zips
are deleted too (but not in the subdirectory "tarballs", if you are using
one).

q) quit

Quits the top level script.


Helper shell functions (utilx)
==============================

All shell functions and global environment variables used in above
mentioned scripts are defined in file $HOME3P/utilx.

The following global variables are defined in utilx:
export LOCAL_PREFIX=/usr/local  (for Linux)
export LOCAL_PREFIX=/opt/local  (for Mac)
export MACPORTS_SVN=http://svn.macports.org/repository/macports/trunk/dports
export MINGW32=i686-pc-mingw32
export MINGW32_PREFIX=/usr/i686-pc-mingw32/sys-root/mingw
export CFLAGS=""
unset CDPATH

The following is a list of shell functions from utilx:
echox(), os(), chkprog(), chksudo(), getpwd(), getans(), getcfg(),
shasumx(), copypkg(), download(), cleanup(), compile(), sudox(), 
copypc(), fix_scons().


Windows installation of precompiled 3rd party tools
===================================================

After successful cross-compilation for Windows a zip package
named "3rd_party_windows.zip" is created on $HOME3P. This zip can be
transferred to Windows and unzipped in root directory of the drive,
where Opencast Matterhorn will run, for example:

C:\> unzip 3rd_party_windows.zip

This will create the /usr/local/... structure on Windows, needed
for proper operation of 3rd party tools on Windows.

<Drive:>\usr\local\bin should be added to PATH variable for proper
operation of Opencast Matterhorn.

You also need to define environment variable TESSDATA_PREFIX as
set TESSDATA_PREFIX=<Drive:>\usr\local\share\
(last "\" is obligatory), where tesseract will find it's language files.

If you want to use ffmpeg preset files (as supplied with the source),
you can use the zip package _ffmpeg.zip, which is also created on $HOME3P.
Transfer it to Windows and unzip it in user's home directory, for
example:

On Windows XP & 2003:
C:\Documents and Settings\matjaz> unzip _ffmpeg.zip

On Vista, Windows 7 & 2008:
C:\Users\matjaz> unzip _ffmpeg.zip

This will create the directory .ffmpeg/..., which is a default location
for ffmpeg preset files in ffmpeg.


Windows installation of gstreamer
=================================

Gstreamer on Windows can be installed from the precompiled packages
from OSSBuild, following the steps outlined here:

- download precompiled version of gstreamer from one of the following
  locations:

GPL version:
  http://ossbuild.googlecode.com/files/GStreamer-WinBuilds-GPL-x86.msi
LGPL version:
  http://ossbuild.googlecode.com/files/GStreamer-WinBuilds-LGPL-x86.msi

  You can also check the OSSBuild site for any newer beta versions.

- start installation by clicking on downloaded msi
  * click Next until you get to the "Custom Setup" screen, where you
    have to select a new install location with the button "Browse".
    Write "C:\usr\local" as a folder name (or any other disk:\usr\local)
    and click OK.
  * select wanted plugins, click Next and then Install

- If the correct folder was selected, gstreamer should be installed along
  with the other 3rd party tools.


Picture of 3rd party tools dependencies
=======================================

Picture 3rd_party.gif shows dependencies among libraries and tools
used in this compilation (created with Graphviz Editor).
