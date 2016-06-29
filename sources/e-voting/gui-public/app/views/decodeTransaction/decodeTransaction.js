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
  .module('e-voting.decodeTransaction.decode-transaction-view', [])
  .controller('DecodeTransactionController', ['transactionInfo', 'apiRequests', function (transactionInfo, apiRequests) {
    var dtc = this;
    dtc.transactionId = null;
    dtc.privateKey = null;
    dtc.transaction = null;
    dtc.decoded = null;

    dtc.transaction = null;

    dtc.getMessage = function(transactionId) {
      return transactionInfo.getTransaction(transactionId, onSuccess);

      function onSuccess(data) {
        dtc.transaction = data;
        return dtc.transaction;
      }
    };

    dtc.decode = function(transaction, key) {
      var items = transaction.attachment.message.split(';');
      var data = {};
      for (var p in items) {
        var current = items[p].split('=');
        data[current[0]] = current[1].replace(/!e/g, "=").replace(/!p/g, ";").replace(/!o/g, "!");
      }
      if (data.TYPE == 'VOTE_STATUS') {
        var body = data["BODY"].split('_');
        data.BODY = {
          votingId: body[0],
          messageId: body[1],
          status: body[2],
          voteDigest: body[3],
          voteSign: body[4]
        };
        dtc.decoded = JSON.stringify(data, null, 2);
      } else if (data.TYPE == 'VOTING' || data.TYPE == 'VOTING_TOTAL_RESULT') {
        dtc.decoded = JSON.stringify(data, null, 2);
      } else if (data.TYPE == 'VOTE') {
        apiRequests.postRequest('decodeMessage', { message: data.BODY, privateKey: key }, function(result) {
          data.BODY = result;
          dtc.decoded = JSON.stringify(data, null, 2);
        }, function(e) {
          console.log(e);
        }, null);
      }
    }
  }]);
