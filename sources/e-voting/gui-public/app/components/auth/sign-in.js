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
  .module('e-voting.auth.sign-in', [])
  .service('signIn', ['apiRequests', '$state', 'userInfo', 'notificationInfo', 'notificationEnum',
    function (apiRequests, $state, userInfo, notificationInfo, notificationEnum) {
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
          null,
          null
        );

        function signInComplete(data) {
          userInfo.setInfo(data);
          $state.go('votingList');
        }
      }
    }
  ]);
