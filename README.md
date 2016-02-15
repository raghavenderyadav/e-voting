# e-voting
Blockchain based voting system



#### Modules
###### registries-server
Registries server module store all necessary information to perform voting procedure, such as votings with all queastions and answers, participants and their rights, blacklist.
Module runs Jetty server and has web service to provide registry information to other modules.

In module configuration file `e-voting\registries-server\src\main\resources\registries-server.properties` you can configure some module properties. For example to configure Jetty TCP/IP port you should change **registries.server.web.port** property. Set of **.filepath** properties configure where module should look for registry JSON files to load data from. 
