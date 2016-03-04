'use strict';

angular
  .module('e-voting.voting.voting-result-model', [])
  .service('votingResultInfo', ['apiRequests',
    function (apiRequests) {
      return {
        getVotingResult: getVotingResult
      };
      function getVotingResult(votingId, getVotingResultComplete) {
        return apiRequests.postCookieRequest(
          'votingResults',
          {
            votingId: votingId
          },
          getVotingResultComplete,
          getVotingResultFailed,
          null
        );

        function getVotingResultFailed(data) {
          console.log('XHR Failed for getVotingResult.' + data.error);
        }
      }
    }
  ]);
