#!/bin/bash

set -ue

ret=0
cd docs/guides

for docs in admin developer; do
  cd $docs
  if test "$( grep -r 'opencast_major_version[()]*}\|{opencast_major_version' )" != ""; then
    echo "Error, $docs has a syntax error related to opencast_major_version:"
    grep -r 'opencast_major_version[()]*}\|{opencast_major_version'
    ret=1
    continue
  fi

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

echo checking workflow operation docs
cd admin/docs/workflowoperationhandlers
# Check page titles all end identical.
# Should all end on " Workflow Operation"
if test "$(head -n 1 -q *woh.md | awk '{print $(NF-1) $NF}' | uniq | wc -l)" -ne 1; then
  echo 'ERROR: All workflow operation page titlesshould end with " Workflow Operation"'
  head -n 1 -q *woh.md | awk '{print $(NF-1) " " $NF}' | uniq
  ret=1
fi
# Check all docs list the operation identifier
if test "$(grep -L 'ID: `' *-woh.md | wc -l)" -ne 0; then
  echo 'ERROR: All workflow operation pages should list the operation identifier'
  grep -L 'ID: `' *-woh.md
  ret=1
fi
cd -

exit $ret
