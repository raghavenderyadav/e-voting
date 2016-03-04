'use strict';

angular
  .module('e-voting.voting.voting-list-model', [])
  .service('votingListInfo', ['apiRequests',
    function (apiRequests) {
      return {
        getVotingList: getVotingList
      };
      function getVotingList(getVotingListComplete) {
        return apiRequests.postCookieRequest(
          'votings',
          {},
          getVotingListComplete,
          getVotingListFailed,
          null
        );

        function getVotingListFailed(data) {
          console.log('XHR Failed for getVotingList.' + data.error);
        }
      }
    }
  ]);
