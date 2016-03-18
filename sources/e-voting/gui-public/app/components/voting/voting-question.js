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
  .module('e-voting.voting.voting-question-model', [])
  .service('votingInfo', ['apiRequests', 'cryptoHelper',
    function (apiRequests, cryptoHelper) {
      return {
        getVoting: getVoting,
        vote: vote,
        getTimer: getTimer,
        normalizeAnswers: normalizeAnswers,
        signVote: signVote
      };
      function getVoting(votingId, getVotingComplete) {
        return apiRequests.postCookieRequest(
          'getVoting',
          {
            votingId: votingId
          },
          getVotingComplete,
          null,
          null
        );
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
          null,
          null
        );
      }

      function getTimer(votingId, getTimerComplete) {
        return apiRequests.postCookieRequest(
          'getTime',
          {
            votingId: votingId
          },
          getTimerComplete,
          null,
          null
        );
      }

      function getKeyByValue(arrObj, targetValue, targetValueKey) {
        var result = null;
        angular.forEach(arrObj, function (value, key) {
          if (value[targetValueKey] === targetValue) {
            result = key;
          }
        });
        return result;
      }

      function normalizeAnswers(answers, questionList, totalVotes) {
        angular.forEach(answers, function (value, key) {
          if (!questionList[getKeyByValue(questionList, key, "id")].canSelectMultiple) {
            var resultObj = {};
            resultObj[value] = totalVotes;
            answers[key] = resultObj;
          }
        });
        return answers;
      }

      function signVote(params, signDataComplete) {
        var requestParams = {};
        if(params.isSign) {
          cryptoHelper.signData(params.signKey, params.xmlData, function (signature) {
            requestParams = {
              votingId: params.votingId,
              signature: signature,
              isSign: true
            };
            sendSign(requestParams);
          });
        } else {
          requestParams = {
            votingId: params.votingId,
            isSign: false
          };
          sendSign(requestParams);
        }

        function sendSign(requestParams) {
          apiRequests.postCookieRequest(
            'signVote',
            requestParams,
            signDataComplete,
            null,
            null
          )
        }
      }
    }
  ]);
