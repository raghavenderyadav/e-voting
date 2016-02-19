#!/usr/bin/env bash
echo Updating git repository...
git reset --hard
git pull
echo git repository updated
gradle clean build
echo Unzipping distribution
unzip -o /home/ubuntu/e-voting/src/e-voting/sources/e-voting/tests-launcher/build/distributions/tests-launcher-0.1-SNAPSHOT.zip -d /home/ubuntu/e-voting/build
echo Coping necessary jar files to libs folder
cp /home/ubuntu/e-voting/build/tests-launcher-0.1-SNAPSHOT/lib/client-0.1-SNAPSHOT.jar /home/ubuntu/e-voting/build/tests-launcher-0.1-SNAPSHOT/libs/client.jar
cp /home/ubuntu/e-voting/src/e-voting/sources/e-voting/libs/nxt.jar /home/ubuntu/e-voting/build/tests-launcher-0.1-SNAPSHOT/libs/nxt.jar
cp -r /home/ubuntu/e-voting/src/e-voting/sources/e-voting/tests-launcher/src/main/resources/json/* /home/ubuntu/e-voting/build/tests-launcher-0.1-SNAPSHOT/bin/json/
cp -r /home/ubuntu/e-voting/src/e-voting/sources/e-voting/registries-server/src/main/resources/json/* /home/ubuntu/e-voting/build/tests-launcher-0.1-SNAPSHOT/bin/json/
echo Build process is finished successfully