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

package uk.dsxt.voting.client.datamodel;

import lombok.Value;

@Value
public class SessionInfoWeb {
    String userName;
    String cookie;
    String rights;

    public SessionInfoWeb(String userName, String cookie, UserRole role) {
        this.userName = userName;
        this.cookie = cookie;
        this.rights = getRights(role);
    }

    private String getRights(UserRole role) {
        if (role != null) {
            switch (role) {
                case VOTER:
                    return "10";
                case ADMIN:
                    return "01";
            }
        }
        return "00";
    }
}
