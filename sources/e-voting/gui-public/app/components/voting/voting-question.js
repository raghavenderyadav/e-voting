'use strict';

angular
  .module('e-voting.voting.voting-question-model', [])
  .service('votingInfo', ['apiRequests',
    function (apiRequests) {
      return {
        getVoting: getVoting,
        vote: vote
      };
      function getVoting(votingId, getVotingComplete) {
        return apiRequests.postCookieRequest(
          'getVoting',
          {
            votingId: votingId
          },
          getVotingComplete,
          getVotingFailed,
          null
        );

        function getVotingFailed(data) {
          console.log('XHR Failed for getVotingResult.' + data.error);
        }
      }

      function vote(params, voteComplete) {
        var param = JSON.stringify(params.votingChoice);
        return apiRequests.postCookieRequest(
          'vote',
          {
            votingId: params.votingId,
            votingChoice: param
          },
          voteComplete,
          voteFailed,
          null
        );

        function voteFailed(data) {
          console.log('XHR Failed for getVotingResult.' + data.error);
        }
      }
    }
  ]);
