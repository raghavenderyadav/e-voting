'use strict';

angular
  .module('e-voting.voting.votes-list-model', [])
  .service('votesListInfo', ['apiRequests',
    function (apiRequests) {
      return {
        getVotesList: getVotesList
      };
      function getVotesList(votingId, getVotesListComplete, votesType) {
        return apiRequests.postCookieRequest(
          votesType + 'Votes',
          {
            votingId: votingId
          },
          getVotesListComplete,
          getVotesListFailed,
          null
        );

        function getVotesListFailed(data) {
          console.log('XHR Failed for ' + votesType + 'Votes.' + data.error);
        }
      }
    }
  ]);
