'use strict';

angular
  .module('e-voting.voting.voting-question-model', [])
  .service('votingInfo', ['apiRequests',
    function (apiRequests) {
      return {
        getVoting: getVoting,
        vote: vote,
        getTimer: getTimer,
        normalizeAnswers: normalizeAnswers,
        isNormal: isNormal
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

      function getTimer(votingId, getTimerComplete) {
        return apiRequests.postCookieRequest(
          'getTime',
          {
            votingId: votingId
          },
          getTimerComplete,
          getTimerFailed,
          null
        );

        function getTimerFailed(data) {
          console.log('XHR Failed for getVotingResult.' + data.error);
        }
      }

      function normalizeAnswers(newAnswers, oldAnswers, questionList) {
        var differenceId = null;
        angular.forEach(newAnswers, function(value, key) {
          if(!angular.equals(value, oldAnswers[key])) {
           differenceId = key;
          }
        });
        if(differenceId !== null && !questionList[getKeyByValue(questionList, differenceId, "id")].canSelectMultiple) {
          angular.forEach(newAnswers[differenceId], function(value, key) {
            if(value === oldAnswers[differenceId][key]) {
             newAnswers[differenceId][key] = 0;
            }
          });
        }
        return newAnswers;

        function getKeyByValue(arrObj, targetValue, targetValueKey) {
          var result = null;
          angular.forEach(arrObj, function(value, key) {
            if(value[targetValueKey] === targetValue) {
              result = key;
            }
          });
          return result;
        }
      }
      function isNormal(answers) {
        var result = true;
        angular.forEach(answers, function(value) {
          result = result ? checkEquality(value) : result;
        });
        return result;

        function checkEquality(obj) {
          var result = true;
          angular.forEach(obj, function(needle, needleKey) {
            angular.forEach(obj, function(value, valueKey) {
              if((needle !== 0) && (needle === value) && (needleKey !== valueKey)) {
                result = false;
              }
            });
          });
          return result;
        }
      }
    }
  ]);
