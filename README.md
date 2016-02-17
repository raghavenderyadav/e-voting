# e-voting
Blockchain based voting system



#### Modules
###### registries-server
Registries server module store all necessary information to perform voting procedure, such as votings with all queastions and answers, participants and their rights, blacklist.
Module runs Jetty server and has web service to provide registry information to other modules.

In module configuration file `e-voting\registries-server\src\main\resources\registries-server.properties` you can configure some module properties. For example to configure Jetty TCP/IP port you should change **registries.server.web.port** property. Set of **.filepath** properties configure where module should look for registry JSON files to load data from. 

###### master-client
Master client module represents special blockchain master node, which runs blockchain and produces emission of money required for voting.

###### client
Client module represents nodes of the network. Each node is used by shareholder or its' representative. Nodes connect to registries-server to collect some common data and to master-client wallet. Also it send votes to results-builder module.

###### results-builder
Results builder receives votes from correct (fair) clients and calculate benchmark result to compare with blockchain result.

###### tests-launcher
Tests launcher launches all modules in necessary order with predefind configuration.
JSON condiguration is highly customizable and can be easily created. Examples of configurations can be found in `tests-launcher\src\main\resources\json` folder.
