cd /home/$(whoami)/e-voting/build/;
cd $1;
cd ./gui-public;
/bin/echo '{ "directory": "app/bower_components" }' > ./.bowerrc;
npm install;
sudo npm install -g bower;
bower update;
