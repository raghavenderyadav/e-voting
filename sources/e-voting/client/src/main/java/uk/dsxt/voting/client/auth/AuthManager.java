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

package uk.dsxt.voting.client.auth;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.client.datamodel.ClientCredentials;
import uk.dsxt.voting.client.datamodel.LoggedUser;
import uk.dsxt.voting.client.datamodel.LoginAnswerWeb;
import uk.dsxt.voting.client.datamodel.SessionInfoWeb;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Log4j2
public class AuthManager {

    public HashMap<String, String> userCredentials = new HashMap<>();

    protected final ConcurrentMap<String, LoggedUser> loggedUsers = new ConcurrentHashMap<>();

    public AuthManager(String credentialsFilepath) {
        log.debug("Initializing AuthManager...");
        try {
            ClientCredentials[] credentials = PropertiesHelper.loadResource(credentialsFilepath, ClientCredentials[].class);
            log.debug("Found {} client credentials.", credentials.length);

            if (credentials.length > 0) {
                for (ClientCredentials c : credentials) {
                    userCredentials.put(c.getClientId(), c.getPassword());
                }
            }
            log.info("{} client credentials loaded.", userCredentials.size());
        } catch (InternalLogicException e) {
            log.error("Couldn't initialize AuthManager", e);
        }
    }

    public LoginAnswerWeb login(String clientId, String password) {
        if (userCredentials.containsKey(clientId)) {
            if (userCredentials.get(clientId).equals(password)) {
                LoggedUser loggedUser = new LoggedUser(clientId);
                String cookie = generateCookieAndLogin(loggedUser);
                String userName = String.format("Dear Client %s", clientId); // TODO Get user display name.
                return new LoginAnswerWeb(new SessionInfoWeb(userName, cookie), null);
            } else {
                return new LoginAnswerWeb(null, "INCORRECT_PASSWORD");
            }
        } else {
            return new LoginAnswerWeb(null, "LOGIN_NOT_FOUND");
        }
    }

    private String generateCookieAndLogin(LoggedUser user) {
        String cookie = UUID.randomUUID().toString();
        while (loggedUsers.putIfAbsent(cookie, user) != null) {
            log.trace(String.format("UUID is already in use: %s", cookie));
            cookie = UUID.randomUUID().toString();
        }
        return cookie;
    }

    public LoggedUser tryGetLoggedUser(String cookie) {
        LoggedUser user = loggedUsers.get(cookie);
        if (user == null)
            return null;
        return user;
    }

    public boolean logout(String cookie) {
        log.debug("logout method called. cookie={}", cookie);
        final LoggedUser loggedUser = tryGetLoggedUser(cookie);
        if (loggedUser != null) {
            List<String> cookiesToDelete = loggedUsers.entrySet().stream().filter(entry -> entry.getValue().getClientId().equals(loggedUser.getClientId())).map(Map.Entry::getKey).collect(Collectors.toList());
            log.debug("logout. found {} sessions for client with id={}", cookiesToDelete.size(), loggedUser.getClientId());
            cookiesToDelete.forEach(loggedUsers::remove);
            log.debug("logout method executed successfully for client with id={}.", loggedUser.getClientId());
            return !loggedUsers.containsKey(cookie);
        }
        log.warn("logout. Couldn't find user by cookie to logout. Cookie: {}", cookie);
        return false;
    }

}
