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
cp /home/ubuntu/e-voting/build/libs/client.jar /home/ubuntu/e-voting/build/nsd/
cp /home/ubuntu/e-voting/build/libs/client.jar /home/ubuntu/e-voting/build/nsd1/
cp /home/ubuntu/e-voting/build/libs/client.jar /home/ubuntu/e-voting/build/nsd2/
echo build of frontend
cp -r /home/ubuntu/e-voting/src/e-voting/sources/e-voting/gui-public/* /home/ubuntu/e-voting/build/gui-public/;
cd /home/ubuntu/e-voting/build/gui-public/;
npm update;
bower update;
sed -i 's/base href=\"\/\"/base href=\"\/nsd\/\"/g' /home/ubuntu/e-voting/build/gui-public/app/index.html
echo "angular.module('e-voting.server-properties', []).constant('serverProperties', {\"serverUrl\": \"https://dsx.tech\", \"serverPort\": \"443\", \"pathToApi\": \"nsd/api\", \"readPortFromUrl\": true });" > /home/ubuntu/e-voting/build/gui-public/app/server-properties.js;
cp -r /home/ubuntu/e-voting/build/gui-public/* /home/ubuntu/e-voting/build/gui-public1/;
sed -i 's/nsd/nsd\/nsd1/g' /home/ubuntu/e-voting/build/gui-public1/app/index.html
echo "angular.module('e-voting.server-properties', []).constant('serverProperties', {\"serverUrl\": \"https://dsx.tech\", \"serverPort\": \"443\", \"pathToApi\": \"nsd/nsd1/api\", \"readPortFromUrl\": true });" > /home/ubuntu/e-voting/build/gui-public1/app/server-properties.js;
cp -r /home/ubuntu/e-voting/build/gui-public/* /home/ubuntu/e-voting/build/gui-public2/;
sed -i 's/nsd/nsd\/nsd2/g' /home/ubuntu/e-voting/build/gui-public2/app/index.html
echo "angular.module('e-voting.server-properties', []).constant('serverProperties', {\"serverUrl\": \"https://dsx.tech\", \"serverPort\": \"443\", \"pathToApi\": \"nsd/nsd2/api\", \"readPortFromUrl\": true });" > /home/ubuntu/e-voting/build/gui-public2/app/server-properties.js;
echo Build process is finished successfully
