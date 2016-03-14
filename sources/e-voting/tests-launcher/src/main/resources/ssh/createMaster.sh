cd /home/ubuntu/e-voting/build/;
mkdir master;
cd master;
cp ../libs/client.jar ./;
cp ../../src/e-voting/sources/e-voting/client/src/main/resources/client.properties ./;
cp -r ../../src/e-voting/sources/e-voting/gui-public/* ./gui-public;
sed -i 's/base href=\"\/\"/base href=\"\/nsd\/\"/g' ./gui-public/app/index.html
