'use strict';

angular
  .module('e-voting.auth.user-info', [])
  .service('userInfo', ['$sessionStorage',
    function ($sessionStorage) {
      var userInfo = angular.isUndefined($sessionStorage.header) ? {} : $sessionStorage.header;
      return {
        setInfo: setInfo,
        getInfo: getInfo,
        deleteInfo: deleteInfo
      };
      function getInfo() {
        return userInfo;
      }
      function setInfo(info) {
        userInfo.userName = info.userName;
        $sessionStorage.header = userInfo;
        $sessionStorage.cookie = info.cookie;
      }
      function deleteInfo() {
        angular.forEach($sessionStorage, function(value, key) {
          if($sessionStorage.hasOwnProperty(key)) {
            delete $sessionStorage[key];
          }
        });
      }
    }
  ]);
