# Opencast 2.0 Player Configuration

The Opencast 2.0 Player (aka Theodul Pass Player) is the new default player in 2.0. The old engage player from 1.x is still available too.

The configurations for the player are done for each tenant. So the configuration keys are located in `<felix_home>/etc/load/org.opencastproject.organization-mh_default_org.cfg`.

## Select the Opencast 2.0 Player
To activate the player set:

    prop.player=/engage/theodul/ui/core.html

## Configuration

### Logo 
The logo in the top right can easily be replaced by changing the path or URL for logo small.

    prop.logo_small=/engage/ui/img/mh_logos/OpencastLogo.png

Options:

 - Any URL or local path to a PNG, GIF, JPG image. Default displayed hight in the browser 36px.

### Position of the controls
The basic controls for the player can be placed over or under the video display.

    prop.player.positioncontrols=bottom

Options:

 - top
 - bottom

### Main video flavor
The default flavor of the master video (the video on the "left side" in the video display). This source also provides the audio. You can change this to every falvor that your installation might provide. If no mastervideotype was selected, or the mastervideotype is not available the videos are taken in their sequence within the mediapackage.

    prop.player.mastervideotype=presenter/delivery

Options (default flavors):

 - presenter/delivery
 - presentation/delivery

### Show Embed links
The player can show a dialog with links to the current video that can be embeded into other websites. This function can be disabled

    prop.show_embed_links=true

Options:

 - true
 - false

### Link to Media Module
If you don't want to use the Opencast Media Module the link within the player back to the overview of the recordings can be disabled

    prop.link_mediamodule=true

Options:

 - true
 - false

### Keyboard Shortcuts
The keyboard shortcuts in the player can be customized

    prop.player.shortcut.playPause=space
    prop.player.shortcut.seekRight=right
    prop.player.shortcut.seekLeft=left
    prop.player.shortcut.playbackrateIncrease=mod+9
    prop.player.shortcut.playbackrateDecrease=mod+8
    prop.player.shortcut.muteToggle=m
    prop.player.shortcut.volUp=9
    prop.player.shortcut.volDown=8
    prop.player.shortcut.fullscreenEnable=mod+enter
    prop.player.shortcut.fullscreenCancel=escape
    prop.player.shortcut.jumpToBegin=backspace
    prop.player.shortcut.prevChapter=pagedown
    prop.player.shortcut.nextChapter=pageup

