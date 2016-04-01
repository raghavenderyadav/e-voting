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
  .module('e-voting.voting.all-voting-statuses-view', [])
  .controller('AllVotingStatusesController', ['allVotingStatusesInfo', '$state', function (allVotingStatusesInfo, $state) {
    var avsc = this;
    avsc.votingStatuses = [];
    avsc.cancel = cancel;

    activate();

    function activate() {
      return allVotingStatusesInfo.getAllVotingStatuses($state.params.id, getAllVotingStatusesComplete);

      function getAllVotingStatusesComplete(data) {
        avsc.votingStatuses = data;
        return avsc.votingStatuses;
      }
    }

    function cancel() {
      $state.go('votingList');
    }
  }]);
