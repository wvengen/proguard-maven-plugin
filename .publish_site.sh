#!/bin/bash
#
# Publish generated site to Github Pages (usually run by Travis CI)
#

if [ "${TRAVIS}" != "true" ]; then
    echo "It only makes sense to run this as part of a Travis CI job" 1>&2
    exit 1
fi

# only upload documentation from the main repository's master
[ "$TRAVIS_REPO_SLUG" == "wvengen/proguard-maven-plugin" ] || exit 2
[ "$TRAVIS_PULL_REQUEST" == "false" ] || exit 2
[ "$TRAVIS_BRANCH" == "master" ] || exit 2

# build documentation
mvn -q site

# setup ssh with key from (secure) environment variables
SSH_DEPLOY_KEY=~/.ssh/id_dsa
env | sort | sed 's/DEPLOY_KEY_[0-9]*=//p;d' >$SSH_DEPLOY_KEY
chmod 600 $SSH_DEPLOY_KEY
cat >~/.ssh/config <<EOF
Host github.com
    CheckHostIP no
    StrictHostKeyChecking no
EOF

# setup credentials so Travis CI can push to GitHub
git config --global user.email "travis-ci"
git config --global user.name "travis@travis-ci.org"

git clone --quiet --branch=gh-pages "git@github.com:${TRAVIS_REPO_SLUG}.git" gh-pages > /dev/null
cd gh-pages
rm -Rf *
cp -Rf ../target/site/* .
git add -f .
git commit -m "Site build of ${TRAVIS_COMMIT} by Travis CI job ${TRAVIS_BUILD_NUMBER}" -a
git push -fq origin gh-pages >/dev/null
rm -f ~/.ssh/id_dsa

