angular
  .module('e-voting.api-requests.api-properties', [])
  .constant('apiProperties',
    {
      "paths": {
        "login": "/login"
      },
      "cookiePaths": {
        "votings": "/votings",
        "getVoting": "/getVoting",
        "getTime": "/getTime",
        "votingResults": "/votingResults",
        "confirmedVotes": "/getConfirmedClientVotes",
        "signResult": "/signResult",
        "vote": "/vote",
        "logout": "/logout"
      }
    }
  );
