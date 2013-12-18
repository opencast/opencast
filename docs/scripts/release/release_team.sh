#Get the current branch name
curBranch=`git status | grep "On branch" | sed 's/\# On branch \(.*\)/\1/'`

#Get the point at which it branched off of master
branchPoint=`git show-branch --sha1-name master $curBranch | tail -n 1 | sed 's/.*\[\(.*\)\].*/\1/'`

#Get the names of the committers.
echo "Note that this is not verified in any way, so whatever the committer put as their name is what you're going to get"
echo "Manual cleanup and deduplication is strongly recommended!"
git log --since $branchPoint | grep Author | sort | uniq | sed 's/Author: \(.*\) <.*/\1/'
