case "$1" in
  1.3.* )
    export FELIX_HOME=/opt/matterhorn/felix
    export FELIX_LOAD_DIR="$FELIX_HOME/load"
    export FELIX_WORK_DIR="$FELIX_HOME/work"
    export FELIX_CONFIG_DIR="$FELIX_HOME/conf"
    export FELIX_DEPLOY_DIR="$FELIX_HOME/matterhorn"
    export FELIX_JARS_DIR="$FELIX_HOME/matterhorn"
    export FELIX_CACHE_DIR="$FELIX_HOME/felix-cache"
    export FELIX_INBOX_DIR="$FELIX_HOME/inbox"
    ;;
  1.4.* )
    export FELIX_HOME=/opt/matterhorn/trunk
    export FELIX_LOAD_DIR="$FELIX_HOME/etc/load"
    export FELIX_WORK_DIR="$FELIX_HOME/work"
    export FELIX_CONFIG_DIR="$FELIX_HOME/etc"
    export FELIX_DEPLOY_DIR="$FELIX_HOME"
    export FELIX_JARS_DIR="$FELIX_HOME/lib/matterhorn"
    export FELIX_CACHE_DIR="$FELIX_HOME/work/felix-cache"
    export FELIX_INBOX_DIR="$FELIX_HOME/inbox"
    ;;
  trunk )
    export FELIX_HOME=/opt/matterhorn/trunk
    export FELIX_LOAD_DIR="$FELIX_HOME/etc/load"
    export FELIX_WORK_DIR="$FELIX_HOME/work"
    export FELIX_CONFIG_DIR="$FELIX_HOME/etc"
    export FELIX_DEPLOY_DIR="$FELIX_HOME"
    export FELIX_JARS_DIR="$FELIX_HOME/lib/matterhorn"
    export FELIX_CACHE_DIR="$FELIX_HOME/work/felix-cache"
    export FELIX_INBOX_DIR="$FELIX_HOME/inbox"
    ;;
  * ) # same as trunk
    export FELIX_HOME=/opt/matterhorn/trunk
    export FELIX_LOAD_DIR="$FELIX_HOME/etc/load"
    export FELIX_WORK_DIR="$FELIX_HOME/work"
    export FELIX_CONFIG_DIR="$FELIX_HOME/etc"
    export FELIX_DEPLOY_DIR="$FELIX_HOME"
    export FELIX_JARS_DIR="$FELIX_HOME/lib/matterhorn"
    export FELIX_CACHE_DIR="$FELIX_HOME/work/felix-cache"
    export FELIX_INBOX_DIR="$FELIX_HOME/inbox"
    ;;
esac
