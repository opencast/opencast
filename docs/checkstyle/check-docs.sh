#!/bin/bash

set -ue

ret=0
cd docs/guides

for docs in admin developer; do
  cd $docs
  echo "Markdown doc $docs build log:"
  mkdocs build &> mkdocs.log
  cat mkdocs.log
  if grep \
       -e 'WARNING.*Documentation file' \
       -e 'pages exist in the docs directory' \
       -e 'is not found in the documentation files' \
        mkdocs.log;
    then
      echo "$docs did not build correctly!"
      ret=1
  fi
  cd ..
done

if ! npm test; then
  echo "npm test failed!"
  ret=1
fi

exit $ret
