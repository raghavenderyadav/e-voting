'use strict';

angular
  .module('e-voting.auth.sign-in', [])
  .service('signIn', ['apiRequests', '$state', 'userInfo',
    function (apiRequests, $state, userInfo) {
      return {
        signIn: signIn
      };
      function signIn(login, password) {
        return apiRequests.postRequest(
          'login',
          {
            login: login,
            password: password
          },
          signInComplete,
          signInFailed,
          null
        );

        function signInComplete(data) {
          userInfo.setInfo(data.result);
          $state.go('votingList')
        }

        function signInFailed(data) {
          console.log('XHR Failed for getVotingList.' + data.error);
        }
      }
    }
  ]);
