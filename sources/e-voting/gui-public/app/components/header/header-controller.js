angular
  .module('e-voting.header', [])
  .controller('HeaderController', ['$scope', 'userInfo', 'signOut', '$sessionStorage',
    function ($scope, userInfo, signOut, $sessionStorage) {
      var hc = this;
      hc.userInfo = userInfo.getInfo();
      hc.signOut = signOut.signOut;
      hc.isLoggedIn = !angular.isUndefined($sessionStorage.cookie);
      $scope.$watch(function () {
        return $sessionStorage.cookie;
      },
      function (newValue) {
        hc.isLoggedIn = !angular.isUndefined(newValue);
      });
      $scope.$watch(function () {
        return userInfo.getInfo();
      },
      function (newValue) {
        hc.userInfo = newValue;
      });
    }
  ]);
