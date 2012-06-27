############
# SETTINGS #
############
SRCDIR=/Users/josh/dev/src/mh_jira
VERSION=r8729-bin-all-dist
LOCAL_MH_BUNDLES=/Applications/Matterhorn_felix3/matterhorn
TEMPDIR=felix-framework-3.0.1

#########
# START #
#########
echo "* Building binary distribution version $VERSION from the local source at $SRCDIR and the bundles at $LOCAL_MH_BUNDLES"

##################
# DOWNLOAD FELIX #
##################
# Get an exploded, vanilla felix, downloading if necessary
[ -f ./felix_temp.zip ] || curl -o felix_temp.zip -# http://repo2.maven.org/maven2/org/apache/felix/org.apache.felix.main.distribution/3.0.1/org.apache.felix.main.distribution-3.0.1.zip;
unzip -q felix_temp.zip;
#rm felix_temp.zip

#######################
# Copy files to felix #
#######################
echo "* Copying files to felix";
cp $SRCDIR/docs/felix/bin/binary_dist_start_matterhorn.sh $TEMPDIR/bin/start.sh;
cp -r $SRCDIR/docs/felix/etc $TEMPDIR;
cp -r $SRCDIR/docs/felix/conf $TEMPDIR;
cp -r $SRCDIR/docs/felix/load $TEMPDIR;
cp -r $SRCDIR/docs/felix/inbox $TEMPDIR;
cp -r $SRCDIR/docs/scripts/3rd_party_tools $TEMPDIR;

# Clean up unneeded felix files
echo "* Removing unneeded files from felix";
rm $TEMPDIR/LICENSE;
rm -rf $TEMPDIR/doc;

# Copy matterhorn bundles
echo "* Copying matterhorn bundles";
mkdir $TEMPDIR/matterhorn;
cp $LOCAL_MH_BUNDLES/*.jar $TEMPDIR/matterhorn;

# Copy matterhorn license
cp $SRCDIR/docs/licenses.txt $TEMPDIR;

# Copy the 3rd party osgi bundles to the lib directory
echo "* Downloading 3rd party libraries";
mkdir $TEMPDIR/lib;
for i in `sed '/^ http:\/\/.*\//!d' $SRCDIR/docs/felix/conf/config.properties`;
  do
    len=${#i};
    if [ -n $len -a $len -gt 1 ]; then
      filename=`basename $i`;
      [ -f $TEMPDIR/lib/$filename ] || curl $i -o $TEMPDIR/lib/$filename -# ;
    fi
  done

#########################
# Fix config.properties #
#########################
echo "* Fixing the paths to 3rd party osgi bundles in config.properties";
# Fix the path to the 3rd party osgi bundles in config.properties
sed -i '' 's#^ http:.*/# file:lib/#g' $TEMPDIR/conf/config.properties;


##########################
# Fix demo capture agent #
##########################
echo "* Copying sample media files for the demo capture agent";
mkdir $TEMPDIR/sample_media;
cp $HOME/.m2/repository/org/opencastproject/samples/screen/1.0/screen-1.0.mpg $TEMPDIR/sample_media;
cp $HOME/.m2/repository/org/opencastproject/samples/camera/1.0/camera-1.0.mpg $TEMPDIR/sample_media;
cp $HOME/.m2/repository/org/opencastproject/samples/audio/1.0/audio-1.0.mp3 $TEMPDIR/sample_media;

echo "* Fixing the paths to the sample media files for the demo capture agent";
sed -i '' 's#=\$.M2_REPO.*/#=./sample_media/#g' $TEMPDIR/conf/services/org.opencastproject.capture.impl.ConfigurationManager.properties;

#########################
# Zip felix and cleanup #
#########################
# Ensure we don't have any .svn files in the binary
find $TEMPDIR -name .svn -exec rm -rf {} \;

echo "* Compressing binary release";
cp -R $TEMPDIR matterhorn-$VERSION
zip -r -q matterhorn-$VERSION.zip matterhorn-$VERSION

# Clean up
#rm -rf $TEMPDIR

echo "****************************************************"
echo "* Finished building matterhorn-$VERSION.zip binary *"
echo "****************************************************"

