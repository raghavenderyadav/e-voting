'use strict';

angular
  .module('e-voting.voting.votes-list-view', [])
  .controller('VotesListController', ['votesListInfo', '$state', function (votesListInfo, $state) {
    var volc = this;
    volc.votesList = [];

    function getVotesListComplete(data) {
      volc.votesList = data;
      return volc.votesList;
    }

    activate();

    function activate() {
      return votesListInfo.getVotesList($state.params.id, getVotesListComplete);
    }
  }]);
