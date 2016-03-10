'use strict';

angular
  .module('e-voting.voting.confirmed-votes-list-view', [])
  .controller('ConfirmedVotesListController', ['confirmedVotesListInfo', '$state', function (confirmedVotesListInfo, $state) {
    var cvlc = this;
    cvlc.confirmedVotesList = [];

    activate();

    function activate() {
      return confirmedVotesListInfo.getConfirmedVotesList($state.params.id, getConfirmedVotesListComplete);

      function getConfirmedVotesListComplete(data) {
        cvlc.confirmedVotesList = data;
        return cvlc.confirmedVotesList;
      }
    }
  }]);
