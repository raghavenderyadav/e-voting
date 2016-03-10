angular
  .module('e-voting.server-properties', [])
  .constant('serverProperties',
    {
      "serverUrl": "http://localhost",
      "serverPort": "9000",
      "pathToApi": "api",
      "readPortFromUrl": true
    });
