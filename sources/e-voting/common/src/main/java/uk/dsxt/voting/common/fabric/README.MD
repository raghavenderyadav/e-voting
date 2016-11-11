To run FabricManager do this:
1. Configure GOPATH in your system.
2. Run following command in fabric folder:
  ./setupFabric.sh
3. Add docker-compose.yml to your home directory.

After ending of process run this command:
  docker rm $(docker ps -a -q) -f
