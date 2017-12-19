# If we have phantomjs installed on our machine (i.e. if it's in the PATH), use
# that instead of the prebuilt version.
if command -v phantomjs >/dev/null 2>&1; then
  export PHANTOMJS_BIN
  PHANTOMJS_BIN="$(command -v phantomjs)"
fi
