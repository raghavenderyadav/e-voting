'use strict';

angular
  .module('e-voting.voting.voting-list-view', [])
  .controller('VotingListController', ['votingListInfo', '$state', function (votingListInfo, $state) {
    var vlc = this;
    vlc.votingList = [];
    vlc.showResults = showResults;
    vlc.showConfirmedVotes = showConfirmedVotes;
    vlc.vote = vote;

    activate();

    function activate() {
      return votingListInfo.getVotingList(getVotingListComplete);

      function getVotingListComplete(data) {
        vlc.votingList = data;
        return vlc.votingList;
      }
    }

    function showResults(votingId) {
      $state.go('votingResult', {id: votingId});
    }
    function showConfirmedVotes(votingId) {
      $state.go('confirmedVotes', {id: votingId});
    }
    function vote(votingId) {
      $state.go('voting', {id: votingId});
    }
  }]);
