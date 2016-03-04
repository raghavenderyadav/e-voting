'use strict';

angular
  .module('e-voting.auth.sign-out', [])
  .service('signOut', ['apiRequests', '$state', 'userInfo',
    function (apiRequests, $state, userInfo) {
      return {
        signOut: signOut
      };
      function signOut() {
        return apiRequests.postCookieRequest(
          'logout',
          {},
          signOutComplete,
          signOutFailed,
          null
        );

        function signOutComplete() {
          userInfo.deleteInfo();
          $state.go('signIn');
        }

        function signOutFailed(data) {
          console.log('XHR Failed for logout.' + data.error);
          $state.go('signIn');
        }
      }
    }
  ]);
