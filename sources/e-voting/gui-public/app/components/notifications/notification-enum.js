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
  .module('e-voting.notifications.notification-enum', [])
  .service('notificationEnum', function () {
    return {
      "errors": {
        "INCORRECT_LOGIN_OR_PASSWORD": "Incorrect login or password",
        "UNKNOWN_EXCEPTION": "Server error",
        "WRONG_COOKIE": "Invalid session",
        "INCORRECT_RIGHTS": "You don't have rights to do this operation",
        "INVALID_SIGNATURE": "Invalid vote's signature",
        "VOTING_NOT_FOUND": "Unknown voting",
        "CLIENT_NOT_FOUND": "Server error",
        "VOTE_NOT_FOUND": "You didn't vote for this voting",
        "VOTE_RESULTS_NOT_FOUND": "Voting result are not ready"
      }
    }
  });
