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
  .module('e-voting.notifications.global-notification-directive', [])
  .directive('globalNotification', globalNotification);

function globalNotification($injector) {
  return {
    restrict: 'AE',
    scope: true,
    templateUrl: 'components/notifications/global-notification-directive.html',
    link: link
  };

  function link(scope, element, attrs) {
    var notificationInfo = $injector.get('notificationInfo'),
      timeout = $injector.get('$timeout'),
      currentTimeout = null,
      showNotificationTime = 10000;
    scope.show = false;
    scope.message = '';
    scope.dialogStyle = {};
    if (attrs.width)
      scope.dialogStyle.width = attrs.width;
    if (attrs.height)
      scope.dialogStyle.height = attrs.height;
    scope.hideModal = function () {
      scope.show = false;
    };
    scope.$watch(notificationInfo.getNotificationCounter, function(newValue) {
      if(newValue > 0) {
        scope.message = notificationInfo.getNotificationMessage();
        scope.show = true;
        if (currentTimeout) timeout.cancel(currentTimeout);
        currentTimeout = timeout(function () {
          scope.show = false;
          scope.message = '';
        }, showNotificationTime);
      }
    })
  }
}