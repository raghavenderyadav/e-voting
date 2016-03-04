'use strict';

angular
  .module('e-voting.voting.voting-result-view', [])
  .controller('VotingResultController',['votingResultInfo', '$state', function (votingResultInfo, $state) {
    var vrc = this;
    vrc.votingResult = [];
    vrc.cancel = cancel;
  
    activate();
  
    function activate() {
      return votingResultInfo.getVotingResult($state.params.id, getVotingResultComplete);
    
      function getVotingResultComplete(data) {
        vrc.votingResult = data;
        return vrc.votingResult;
      }
    }
  
    function cancel() {
      $state.go('votingList');
    }
  }]);