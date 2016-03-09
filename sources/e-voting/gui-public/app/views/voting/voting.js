'use strict';

angular
  .module('e-voting.voting.voting-question-view', [])
  .controller('VotingController',['votingInfo', '$state', function (votingInfo, $state) {
    var vc = this;
    vc.voting = [];
    vc.votingChoice = {};
    vc.cancel = cancel;
    vc.vote = vote;
    vc.totalVotes = 0;

    activate();

    function activate() {
      return votingInfo.getVoting($state.params.id, getVotingComplete);

      function getVotingComplete(data) {
        vc.voting = data.questions;
        vc.totalVotes = data.amount;
        angular.forEach(vc.voting, function(question) {
          vc.votingChoice[question.id] = {};
          angular.forEach(question.answers, function(answer) {
            vc.votingChoice[question.id][answer.id] = 0;
          });
        });
        return vc.voting;
      }
    }

    function vote() {
      votingInfo.vote({
        votingId: $state.params.id,
        votingChoice: vc.votingChoice
      }, voteComplete);

      function voteComplete(response) {
        if(response) {
          alert("Vote accepted");
        } else {
          alert("Vote failed");
        }
        cancel();
      }
    }
    function cancel() {
      $state.go('votingList');
    }
  }]);
