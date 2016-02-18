#!/usr/bin/env bash
echo Updating git repository...
git pull
echo git repository updated
gradle clean build
echo Unzipping distribution
unzip -o /home/ubuntu/e-voting/src/e-voting/sources/e-voting/tests-launcher/build/distributions/tests-launcher-0.1-SNAPSHOT.zip -d /home/ubuntu/e-voting/build
echo Coping necessary jar files to libs folder
cp /home/ubuntu/e-voting/build/lib/client-0.1-SNAPSHOT.jar /home/ubuntu/e-voting/build/libs/client.jar
cp /home/ubuntu/e-voting/build/lib/nxt.jar /home/ubuntu/e-voting/build/libs/nxt.jar
echo Build process is finished successfully