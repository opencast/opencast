#!/bin/bash

set -ue

ret=0
cd docs/guides

for docs in admin developer; do
  cd $docs
  echo "Building $docs documentation…"
  if mkdocs build &> mkdocs.log; then
    if grep \
        -e 'WARNING.*Documentation file' \
        -e 'pages exist in the docs directory' \
        -e 'is not found in the documentation files' \
         mkdocs.log;
    then
      echo "$docs did not build correctly!"
      cat mkdocs.log
      ret=1
    fi
  else
    echo mkdocs exited abnormally.
    cat mkdocs.log
    ret=1
  fi
  cd ..
done

if ! npm test; then
  echo "npm test failed!"
  ret=1
fi

exit $ret
