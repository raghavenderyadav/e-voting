/******************************************************************************
 * e-voting system                                                            *
 * Copyright (C) 2016 DSX Technologies Limited.                               *
 * *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation; either version 2 of the License, or          *
 * (at your option) any later version.                                        *
 * *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied                         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * *
 * You can find copy of the GNU General Public License in LICENSE.txt file    *
 * at the top-level directory of this distribution.                           *
 * *
 * Removal or modification of this copyright notice is prohibited.            *
 * *
 ******************************************************************************/

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
