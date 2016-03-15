'use strict';

angular
  .module('e-voting.voting.votes-list-view', [])
  .controller('VotesListController', ['votesListInfo', '$state', function (votesListInfo, $state) {
    var volc = this;
    volc.votesList = [];
    volc.votesType = 'confirmed';
    volc.getVotes = getVotes;

    function getVotesListComplete(data) {
      volc.votesList = data;
      return volc.votesList;
    }

    activate();

    function activate() {
      return votesListInfo.getVotesList($state.params.id, getVotesListComplete, 'confirmed');
    }

    function getVotes(type) {
      votesListInfo.getVotesList($state.params.id, getVotesListComplete, type);
    }
  }]);
