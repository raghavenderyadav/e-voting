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
          null,
          null
        );
      }
    }
  ]);
