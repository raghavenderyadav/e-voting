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

package uk.dsxt.voting.client;

public class NodeCommunicationTest {
    
    public static void main(String[] args) {
        VotingClientMain.main(new String[]{
            "./conf/nxt-default.properties",
            "NXT-9PHW-CVXU-2TDY-H4878",
            "client_password",
            "00",
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCOcmezdk84J9QUIPl6URnlJrnCybBTsluqVcWxddVARLLE1k2dk5AA4i7gJ1xHapKAQL7F3XjcTZplitWfP2jxiTMMBcYqMG31dy5f90v+B+4juwQvpJoISYhXaBUtZxcos016ZcY8ylXTpqXTt5MCSXoSMvfSGdBE74acodG6sLXwnukNqOBSgU/SJkVWPxtjb14cEF2CXRQklNKs83B03ELDU7+08teFh89Mon9qHiUR8WqCAPMaTm0qWIbLvxGr99LZRLgxCTe8Hgj2Md2oT85voQdueldll/EKP6VpIBgzOqjBoMc/3cUjd8G5fmdQZEPK2teWE4gO9G59cTEFAgMBAAECggEAOuTiKzjHGBiffpMDkqblZfDU7MwmsvQTIiHEUtK9EI1WvDs+a+AOsc7SQqsDZCOT3qLmPTiMN8l+BG2aVPUKlpJ7IIVioR7U16Am9FZyfN0agHtaB7iuVq7QSBMoblUpJhK7/dcGVyvwwEkuVpKXnWJzrgKUo7E4gsflh+z/oYe/xHkeIAZG1C9r19ntR7s8sE/lACoD3TxZDmGbOXuoBQLTz0TYXWsNMguu7DKflp41sD8SybOfe9z8Tfq3rTqpcBZivHbtGzgR/dJxiInqtDwmAPU1xLr6Cv9KK53/UHAs557TbrBzVAAqUxSjpReA08OxYhRneHIJY8MGjU9WIQKBgQDPE/vRst6D6EK0z1nvESPuJqZQkMg5RSYu9nIMRKYV9kZl9GJ5kTaHtM27URd5ctAXHiqJYEQEIfBLwxAXksyC1jLWd3X34Zl+omzNy1+HjbwInMhifjW7nfwJXJVfKhHZFORLkCBLL5xt1erazbEeYiTfiQ9Z0g3XvveG3D1YfwKBgQCwGYx3owGfgnKbPpsCIVm3v2zd7Rh8c6HcibeomTcK0tVrkKfi4Kk9ed+iOytf+Up2HtBzJqp64R8Xs2eFccOOzx6tJN82pzcP48J4XIXQyuxpHEheQABLACHwy+WUVqR0jsUQf/7934v1Fv07B1woGMIaDjFJffFDkbQYrhJUewKBgQCNR7wSCPBJnKgORj28nrwd2l60LuN8N1Jizh9ngVqzNzA2lTKucEV89v06JIxYft28OAebbINbMnCIsBAFlVFUnqFWs3BX66JWxKhpC60kha3ZTmZk1GkClToEhRcgM0q0Cc3sQ+vUgCpAwacXGykRarJvlEpV5LsvDApDB3YPLwKBgGVHjF4SRhCzOa7Hpubmv27KjZZlkjuhVWo9Wn+A/wMeltgybhwyEaPlwBTR6vRbr9OXjVNs3YemifdbmyJId6xeusnh9u675RMibupCbEPVMXqSZZyvOnvoK50N55AU9KiEpBoFQ2ZHd3sSKboVVY9KDfhmSTp3UJcH6Yh4NNqZAoGBALRYToCYl7NJR2iGExW1nLlTWT2+LzqMb/wI32MOfKQefgiW2tYDk1GyULhCIkbEGMLsVqvziF8ntH3OwydLX30OrZwnOgkbKV+FiIBQ1qL7H8cMjBwC4b6C1JhfBp8OpNd3zu1zFAZrvEQRleKTASmPLJhLbEqydYSV4XJsgDl7",
            "3:123456,1,8,1.2-1-8\n3:123456,2,20,1.2-1-15",
            null,
            "9000",
            "true",
            "true",
            "./gui-public/app",
            null,
            "credentials00.json",
            "clients00.json",
        });
        VotingClientMain.main(new String[]{
            "./conf/nxt-default.properties",
            "NXT-9PHW-CVXU-2TDY-H4878",
            "client_password1",
            "5",
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCOcmezdk84J9QUIPl6URnlJrnCybBTsluqVcWxddVARLLE1k2dk5AA4i7gJ1xHapKAQL7F3XjcTZplitWfP2jxiTMMBcYqMG31dy5f90v+B+4juwQvpJoISYhXaBUtZxcos016ZcY8ylXTpqXTt5MCSXoSMvfSGdBE74acodG6sLXwnukNqOBSgU/SJkVWPxtjb14cEF2CXRQklNKs83B03ELDU7+08teFh89Mon9qHiUR8WqCAPMaTm0qWIbLvxGr99LZRLgxCTe8Hgj2Md2oT85voQdueldll/EKP6VpIBgzOqjBoMc/3cUjd8G5fmdQZEPK2teWE4gO9G59cTEFAgMBAAECggEAOuTiKzjHGBiffpMDkqblZfDU7MwmsvQTIiHEUtK9EI1WvDs+a+AOsc7SQqsDZCOT3qLmPTiMN8l+BG2aVPUKlpJ7IIVioR7U16Am9FZyfN0agHtaB7iuVq7QSBMoblUpJhK7/dcGVyvwwEkuVpKXnWJzrgKUo7E4gsflh+z/oYe/xHkeIAZG1C9r19ntR7s8sE/lACoD3TxZDmGbOXuoBQLTz0TYXWsNMguu7DKflp41sD8SybOfe9z8Tfq3rTqpcBZivHbtGzgR/dJxiInqtDwmAPU1xLr6Cv9KK53/UHAs557TbrBzVAAqUxSjpReA08OxYhRneHIJY8MGjU9WIQKBgQDPE/vRst6D6EK0z1nvESPuJqZQkMg5RSYu9nIMRKYV9kZl9GJ5kTaHtM27URd5ctAXHiqJYEQEIfBLwxAXksyC1jLWd3X34Zl+omzNy1+HjbwInMhifjW7nfwJXJVfKhHZFORLkCBLL5xt1erazbEeYiTfiQ9Z0g3XvveG3D1YfwKBgQCwGYx3owGfgnKbPpsCIVm3v2zd7Rh8c6HcibeomTcK0tVrkKfi4Kk9ed+iOytf+Up2HtBzJqp64R8Xs2eFccOOzx6tJN82pzcP48J4XIXQyuxpHEheQABLACHwy+WUVqR0jsUQf/7934v1Fv07B1woGMIaDjFJffFDkbQYrhJUewKBgQCNR7wSCPBJnKgORj28nrwd2l60LuN8N1Jizh9ngVqzNzA2lTKucEV89v06JIxYft28OAebbINbMnCIsBAFlVFUnqFWs3BX66JWxKhpC60kha3ZTmZk1GkClToEhRcgM0q0Cc3sQ+vUgCpAwacXGykRarJvlEpV5LsvDApDB3YPLwKBgGVHjF4SRhCzOa7Hpubmv27KjZZlkjuhVWo9Wn+A/wMeltgybhwyEaPlwBTR6vRbr9OXjVNs3YemifdbmyJId6xeusnh9u675RMibupCbEPVMXqSZZyvOnvoK50N55AU9KiEpBoFQ2ZHd3sSKboVVY9KDfhmSTp3UJcH6Yh4NNqZAoGBALRYToCYl7NJR2iGExW1nLlTWT2+LzqMb/wI32MOfKQefgiW2tYDk1GyULhCIkbEGMLsVqvziF8ntH3OwydLX30OrZwnOgkbKV+FiIBQ1qL7H8cMjBwC4b6C1JhfBp8OpNd3zu1zFAZrvEQRleKTASmPLJhLbEqydYSV4XJsgDl7",
            "5:123456,6,10,1.2-1-5\n5:123456,7,20,1.2-1-5",
            null,
            "9005",
            "false",
            "true",
            "./gui-public/app",
            "http://127.0.0.1:9000/holderAPI",
            "credentials5.json",
            "clients5.json",
        });
    }
    
}
