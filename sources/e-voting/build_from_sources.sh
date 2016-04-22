#!/usr/bin/env bash
echo Updating git repository...
git reset --hard
git pull
echo git repository updated
gradle clean build
echo Unzipping distribution
unzip -o /home/$(whoami)/e-voting/src/e-voting/sources/e-voting/tests-launcher/build/distributions/tests-launcher-0.1-SNAPSHOT.zip -d /home/$(whoami)/e-voting/build
cp -r /home/$(whoami)/e-voting/build/tests-launcher-0.1-SNAPSHOT/* /home/$(whoami)/e-voting/build/
rm -r /home/$(whoami)/e-voting/build/tests-launcher-0.1-SNAPSHOT/*
echo Coping necessary jar files to libs folder
cp /home/$(whoami)/e-voting/build/lib/client-0.1-SNAPSHOT.jar /home/$(whoami)/e-voting/build/libs/client.jar
cp /home/$(whoami)/e-voting/src/e-voting/sources/e-voting/libs/nxt.jar /home/$(whoami)/e-voting/build/libs/nxt.jar
cp /home/$(whoami)/e-voting/src/e-voting/sources/e-voting/libs/bcprov-jdk15on-154.jar /home/$(whoami)/e-voting/build/libs/bcprov-jdk15on-154.jar
cp /home/$(whoami)/e-voting/src/e-voting/sources/e-voting/libs/nxt.jar /home/$(whoami)/e-voting/build/lib/nxt.jar
cp -r /home/$(whoami)/e-voting/src/e-voting/sources/e-voting/tests-launcher/src/main/resources/json/* /home/$(whoami)/e-voting/build/bin/json/
cp -r /home/$(whoami)/e-voting/src/e-voting/sources/e-voting/registries-server/src/main/resources/json/* /home/$(whoami)/e-voting/build/bin/json/
cp /home/$(whoami)/e-voting/build/libs/client.jar /home/$(whoami)/e-voting/build/nsd/
cp /home/$(whoami)/e-voting/build/libs/client.jar /home/$(whoami)/e-voting/build/nsd1/
cp /home/$(whoami)/e-voting/build/libs/client.jar /home/$(whoami)/e-voting/build/nsd2/
echo build of frontend
cp -r /home/$(whoami)/e-voting/src/e-voting/sources/e-voting/gui-public/* /home/$(whoami)/e-voting/build/gui-public/;
cd /home/$(whoami)/e-voting/build/gui-public/;
npm update;
bower update;
sed -i 's/base href=\"\/\"/base href=\"\/nsd\/\"/g' /home/$(whoami)/e-voting/build/gui-public/app/index.html
sed -i 's/\/nxt?requestType=/\/nsd\/nxt?requestType=/g' /home/$(whoami)/e-voting/build/nsd/html/ui/js/nrs.server.js
echo "angular.module('e-voting.server-properties', []).constant('serverProperties', {\"serverUrl\": \"https://dsx.tech\", \"serverPort\": \"443\", \"pathToApi\": \"nsd/api\", \"readPortFromUrl\": true });" > /home/$(whoami)/e-voting/build/gui-public/app/server-properties.js;
cp -r /home/$(whoami)/e-voting/build/gui-public/* /home/$(whoami)/e-voting/build/gui-public1/;
sed -i 's/nsd/nd1/g' /home/$(whoami)/e-voting/build/gui-public1/app/index.html
sed -i 's/\/nxt?requestType=/\/nd1\/nxt?requestType=/g' /home/$(whoami)/e-voting/build/nsd1/html/ui/js/nrs.server.js
echo "angular.module('e-voting.server-properties', []).constant('serverProperties', {\"serverUrl\": \"https://dsx.tech\", \"serverPort\": \"443\", \"pathToApi\": \"nd1/api\", \"readPortFromUrl\": true });" > /home/$(whoami)/e-voting/build/gui-public1/app/server-properties.js;
cp -r /home/$(whoami)/e-voting/build/gui-public/* /home/$(whoami)/e-voting/build/gui-public2/;
sed -i 's/nsd/nd2/g' /home/$(whoami)/e-voting/build/gui-public2/app/index.html
sed -i 's/\/nxt?requestType=/\/nd2\/nxt?requestType=/g' /home/$(whoami)/e-voting/build/nsd2/html/ui/js/nrs.server.js
echo "angular.module('e-voting.server-properties', []).constant('serverProperties', {\"serverUrl\": \"https://dsx.tech\", \"serverPort\": \"443\", \"pathToApi\": \"nd2/api\", \"readPortFromUrl\": true });" > /home/$(whoami)/e-voting/build/gui-public2/app/server-properties.js;
echo Build process is finished successfully
