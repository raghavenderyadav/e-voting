#!/usr/bin/env bash
echo Updating git repository...
git pull
echo git repository updated
gradle clean build
echo Unzipping distribution
unzip -o /home/ubuntu/e-voting/src/e-voting/sources/e-voting/tests-launcher/build/distributions/tests-launcher-0.1-SNAPSHOT.zip -d /home/ubuntu/e-voting/build
echo Build process is finished successfully