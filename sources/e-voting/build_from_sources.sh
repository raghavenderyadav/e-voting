#!/usr/bin/env bash
echo Updating git repository...
git reset --hard
git pull
echo git repository updated
gradle clean build
echo Unzipping distribution
unzip -o /home/ubuntu/e-voting/src/e-voting/sources/e-voting/tests-launcher/build/distributions/tests-launcher-0.1-SNAPSHOT.zip -d /home/ubuntu/e-voting/build
cp -r /home/ubuntu/e-voting/build/tests-launcher-0.1-SNAPSHOT/* /home/ubuntu/e-voting/build/
rm -r /home/ubuntu/e-voting/build/tests-launcher-0.1-SNAPSHOT/*
echo Coping necessary jar files to libs folder
cp /home/ubuntu/e-voting/build/lib/client-0.1-SNAPSHOT.jar /home/ubuntu/e-voting/build/libs/client.jar
cp /home/ubuntu/e-voting/src/e-voting/sources/e-voting/libs/nxt.jar /home/ubuntu/e-voting/build/libs/nxt.jar
cp /home/ubuntu/e-voting/src/e-voting/sources/e-voting/libs/nxt.jar /home/ubuntu/e-voting/build/lib/nxt.jar
cp -r /home/ubuntu/e-voting/src/e-voting/sources/e-voting/tests-launcher/src/main/resources/json/* /home/ubuntu/e-voting/build/bin/json/
cp -r /home/ubuntu/e-voting/src/e-voting/sources/e-voting/registries-server/src/main/resources/json/* /home/ubuntu/e-voting/build/bin/json/
echo build of frontend
cp -r /home/ubuntu/e-voting/src/e-voting/sources/e-voting/gui-public/* /home/ubuntu/e-voting/build/gui-public/;
cd /home/ubuntu/e-voting/build/gui-public/;
npm update;
bower update;
echo Build process is finished successfully
