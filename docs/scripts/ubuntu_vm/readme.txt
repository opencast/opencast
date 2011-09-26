These scripts are not yet working and should not be relied upon for production.

Main script:
buildvm.sh - call this with sudo to build a vm using vmbuilder scripts

Scripts called by buildvm.sh: 
postinstall.sh - currently installs Sun JDK in the VM, other actions that should be performed on the VM after creation can be added here.

Files that will be copied on the VM:
startup.sh - will start matterhorn on the VM
shutdown.sh - will stop matterhorn on the VM
update-matterhorn - performs a clean matterhorn SVN update on the VM
rc.local - startup of matterhorn on the VM, first boot setup will be called here too.
matterhorn-setup.sh - installs ffmpeg and third-party tools on VM, performs some basic config settings
mediainfo - mediainfo 0.7.19
libmediainfo.a - mediainfo 0.7.19
libmediainfo.la - mediainfo 0.7.19
