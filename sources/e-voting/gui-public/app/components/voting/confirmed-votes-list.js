'use strict';

angular
  .module('e-voting.voting.confirmed-votes-list-model', [])
  .service('confirmedVotesListInfo', ['apiRequests',
    function (apiRequests) {
      return {
        getConfirmedVotesList: getConfirmedVotesList
      };
      function getConfirmedVotesList(votingId, getConfirmedVotesListComplete) {
        return apiRequests.postCookieRequest(
          'confirmedVotes',
          {
            votingId: votingId
          },
          getConfirmedVotesListComplete,
          getConfirmedVotesListFailed,
          null
        );

        function getConfirmedVotesListFailed(data) {
          console.log('XHR Failed for getConfirmedClientVotes.' + data.error);
        }
      }
    }
  ]);
