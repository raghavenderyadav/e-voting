To run FabricManager do this:
1. Configure GOPATH in your system.
2. Add file evoting.go to "github.com/hyperledger/fabric/examples/chaincode/go/evoting"
3. Run following command in fabric folder:
  ./setupFabric.sh

After ending of process run this command:
  docker rm $(docker ps -a -q) -f
