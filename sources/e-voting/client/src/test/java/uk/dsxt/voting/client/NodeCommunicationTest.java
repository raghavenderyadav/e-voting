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
            "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAlNntpENmQzCyPx+M3D1RZypdxkfFF2+60CSDtqCSvsi/MLsPEu87CDYxuBmTtLY5zBP2HcNIvT9cB699nRNFAQIDAQABAkBAk4sViGgFHks2N2nU4oU+TJMCQoCu+joBstWxlVgUjDYGk/QHEMhx60kZ3L2Pw8k8uFZVCDXy0/uemuIp8vABAiEA7xlJWC8bCDYqVggQgK9yzAuL7P1T0+dUF080P8kR7nECIQCfX4epGtFSWJFOK+CGly/mLyhZrn6g0cu7jKCw5BgnkQIhAJFihNCURBGoLfIGEVLOXDVqR/kgyNou7VkHFjQ65SZhAiAf79fSpmId+0ua+6XxsqhRm0+dsR8FASWvfr3Q1NSWUQIgGfqAUV4I0nG8sIz3UE7rf+tzQaScDYOoCNu4amJjxEI=",
            "3:123456,1,8,1.2-1-8",
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
            "MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAygnxmB4jC7wPZdx9/2M0vRt0zvt+Xq6kJRcd2dxhKLSWxoxL9CHlzoEFHrSWGaEZMSHqw3Spxpb5bu6nlMrkfwIDAQABAkAjVQvYA2UzlybGNIIgWHQPoi6SR+74legEyH8i62ReXq6Vdk0xaVqWpzoo7Ih89vPdND6abFrNLd7SOrk1ASZZAiEA+u+jJjnfB4hGV1PfKsqP8GyLNWNGR6qXJvoEUdu9q60CIQDOHbKyyizMt7QPbBxlT7/nBxrPwz66hzM3+XLebdYWWwIhAPTxniveKZrMpvzvXdQDTmW9TlWaxiuGlWzyd+z/tjExAiBSmeQ7cnpxsE0gwRrAHy2w0FAWYxCIgBYuoHFAYpQhcQIhAJGkcJ56CWS4Y24EJGwfEZyIt4ixuKYSY6eoBHuYfP7f",
            "5:123456,6,10,1.2-1-5",
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
