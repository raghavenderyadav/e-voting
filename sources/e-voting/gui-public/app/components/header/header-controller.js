angular
  .module('e-voting.header', [])
  .controller('HeaderController', ['userInfo', 'signOut', '$sessionStorage',
    function (userInfo, signOut, $sessionStorage) {
      var hc = this;
      hc.userInfo = userInfo.getInfo();
      hc.signOut = signOut.signOut;
      hc.isLoggedIn = !angular.isUndefined($sessionStorage.cookie);
    }
  ]);