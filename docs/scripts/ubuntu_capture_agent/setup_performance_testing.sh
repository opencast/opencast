#! /bin/bash

##############################################################
# Setup environment for performance testing of capture agent #
##############################################################

# Note: Not integral to capture agent setup

sudo apt-get install dstat python-setuptools gnuplot
sudo easy_install ipython

echo "=====Attention====="
echo "You should install FFMPEG with whichever codecs you intend to test with"
echo "Because some of these codecs are not redistributable we cannot use the"
echo "default Ubuntu ffmpeg, nor can this script build them for you."
echo ""
echo "If you have any problems please email matterhorn@opencastproject.org"
echo "Or join us in our IRC room:  #opencast on the Freenode IRC network"

