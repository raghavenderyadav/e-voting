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
  .module('e-voting.voting.voting-question-view', [])
  .controller('VotingController',['$scope', 'votingInfo', '$state', '$interval', function ($scope, votingInfo, $state, $interval) {
    var vc = this;

    vc.voting = [];
    vc.votingChoice = {};
    vc.cancel = cancel;
    vc.vote = vote;
    vc.totalVotes = 0;
    vc.votingCountdown = 0;

    activate();

    function activate() {
      return votingInfo.getVoting($state.params.id, getVotingComplete);

      function getVotingComplete(data) {
        vc.voting = data.questions;
        vc.totalVotes = data.amount;
        vc.votingCountdown = data.timer;
        angular.forEach(vc.voting, function(question) {
          vc.votingChoice[question.id] = {};
          angular.forEach(question.answers, function(answer) {
            vc.votingChoice[question.id][answer.id] = 0;
          });
        });
        return vc.voting;
      }

      function getTimerComplete(data) {
        if(vc.votingCountdown < 0) {
          interval.cancel(countdownInterval);
        } else {
          vc.votingCountdown = data;
        }
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
    $scope.$watch(function() {
      return vc.votingChoice;
    }, function(newValue, oldValue) {
      if((newValue !== oldValue) && (oldValue != undefined) && !angular.equals(oldValue, {}) && votingInfo.isNormal(oldValue, vc.voting)) {
        vc.votingChoice = votingInfo.normalizeAnswers(newValue, oldValue, vc.voting)
      }
    }, true)
  }]);
