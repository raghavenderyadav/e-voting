# e-voting
Blockchain based voting system



#### Modules
###### client
Client module represents each node of the network.
Each node contains:
-blockchain client application;
-set of configuration files;
-client/admin web application.
Nodes collect it's own configuration and information about voting and shareholders from set of configuration files (XML and JSON).
It's possible to run client as a master node using special configuration preset.

###### results-builder
Results builder receives votes from correct (fair) clients and calculate benchmark result to compare with blockchain result.

###### tests-launcher
Tests launcher launches all modules in necessary order with predefind configuration.
JSON condiguration is highly customizable and can be easily created. Examples of configurations can be found in `tests-launcher\src\main\resources\json` folder.
