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
  .module('e-voting.auth.user-info', [])
  .service('userInfo', ['$sessionStorage', 'rightsRolesMap',
    function ($sessionStorage, rightsRolesMap) {
      var userInfo = angular.isUndefined($sessionStorage.header) ? {} : $sessionStorage.header;
      return {
        setInfo: setInfo,
        setTotalVotes: setTotalVotes,
        getInfo: getInfo,
        deleteInfo: deleteInfo
      };
      function getInfo() {
        return userInfo;
      }

      function setInfo(info) {
        userInfo.userName = info.userName;
        userInfo.roles = configRoles(info.rights);
        userInfo.totalVotes = null;
        $sessionStorage.header = userInfo;
        $sessionStorage.cookie = info.cookie;

        function configRoles(rights) {
          var result = [];
          for (var i = 0; i < rights.length; i++) {
            if (rights[i] === '1') {
              result.push(rightsRolesMap[i]);
            }
          }
          return result;
        }
      }

      function setTotalVotes(totalVotes) {
        userInfo.totalVotes = totalVotes;
      }

      function deleteInfo() {
        angular.forEach($sessionStorage, function (value, key) {
          if ($sessionStorage.hasOwnProperty(key)) {
            delete $sessionStorage[key];
          }
        });
      }
    }
  ]);
