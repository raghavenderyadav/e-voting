/******************************************************************************
 * e-voting system                                                            *
 * Copyright (C) 2016 DSX Technologies Limited.                               *
 *                                                                            *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation; either version 2 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied                         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * You can find copy of the GNU General Public License in LICENSE.txt file    *
 * at the top-level directory of this distribution.                           *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

'use strict';

angular
  .module('e-voting.role-management.role-manager', [])
  .service('roleManager', ['$sessionStorage', '$state', 'userInfo', 'roleEnums',
    function ($sessionStorage, $state, userInfo, roleEnums) {
      return {
        checkAccess: checkAccess,
        checkPagePermission: checkPagePermission
      };
      function checkPagePermission(event, toState) {
        var roles = userInfo.getInfo().roles;
        if (!angular.isUndefined(toState.access)) {
          if (toState.access.loginRequired !== undefined && toState.access.loginRequired) {
            if (angular.isUndefined($sessionStorage.cookie)) {
              event.preventDefault();
              $state.go('signIn', {}, {location: "replace", reload: false, inherit: false, notify: false});
            }
          }
          if (!angular.isUndefined(toState.access.permissionsRequired)) {
            if (toState.access.permissionsRequired.length != intersect(toState.access.permissionsRequired, roles).length) {
              event.preventDefault();
              $state.go('404', {locale: $state.params.locale}, {location: "replace", reload: true, inherit: true, notify: true});
            }
          }
        }

        function intersect(a, b) {
          var t;
          if (b.length > a.length) { // indexOf to loop over shorter
            t = b;
            b = a;
            a = t
          }
          return a.filter(function (e) {
            if (b.indexOf(e) !== -1) return true;
          });
        }

      }

      function checkAccess(loginRequired, roles, permissionType) {
        var result = roleEnums.permissions.granted,
          user = {},
          hasPermission = true,
          permission, i;

        user.cookie = $sessionStorage.cookie;
        user.permissions = userInfo.getInfo().roles;
        permissionType = permissionType || roleEnums.permissionTypes.atLeastOne;
        if (loginRequired === true && user.cookie === undefined) {
          result = roleEnums.permissions.denied;
        } else if ((loginRequired === true && user.cookie !== undefined) &&
          (roles === undefined || roles.length === 0)) {
          result = roleEnums.permissions.granted;
        } else if (roles) {

          for (i = 0; i < roles.length; i += 1) {
            permission = roles[i].toLowerCase();

            if (permissionType === roleEnums.permissionTypes.multiple) {
              hasPermission = hasPermission && user.permissions.indexOf(permission) > -1;
              if (hasPermission === false) {
                break;
              }
            } else if (permissionType === roleEnums.permissionTypes.atLeastOne) {
              hasPermission = user.permissions.indexOf(permission) > -1;
              if (hasPermission) {
                break;
              }
            }
          }

          result = hasPermission ? roleEnums.permissions.granted : roleEnums.permissions.denied;
        }

        return result;
      }
    }
  ]);
