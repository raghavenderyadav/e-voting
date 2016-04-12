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
import org.apache.logging.log4j.Logger;
import uk.dsxt.voting.client.datamodel.*;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Log4j2
public class AuthManager {
    
    private final String ADMIN_NAME = "admin";
    
    public HashMap<String, ClientCredentials> userCredentials = new HashMap<>();

    protected final ConcurrentMap<String, LoggedUser> loggedUsers = new ConcurrentHashMap<>();

    private final Map<String, Participant> participantsById;

    private Logger audit;

    public AuthManager(String credentialsFilepath, Logger audit, Map<String, Participant> participantsById) {
        log.debug("Initializing AuthManager...");
        this.audit = audit;
        this.participantsById = participantsById;
        try {
            ClientCredentials[] credentials = PropertiesHelper.loadResource(credentialsFilepath, ClientCredentials[].class);
            log.debug("Found {} client credentials.", credentials.length);
            if (credentials.length > 0) {
                for (ClientCredentials c : credentials) {
                    userCredentials.put(c.getClientId(), c);
                }
                //TODO: added default admin role
                userCredentials.put(ADMIN_NAME, new ClientCredentials(ADMIN_NAME, "admin", UserRole.ADMIN));
            }
            log.info("{} client credentials loaded.", userCredentials.size());
        } catch (InternalLogicException e) {
            log.error("Couldn't initialize AuthManager", e);
        }
    }

    public RequestResult login(String clientId, String password) {
        try {
            if (userCredentials.containsKey(clientId)) {
                ClientCredentials clientCrls = userCredentials.get(clientId);
                if (clientCrls.getPassword().equals(password)) {
                    LoggedUser loggedUser = new LoggedUser(clientId, clientCrls.getRole());
                    String cookie = generateCookieAndLogin(loggedUser);
                    String userName = participantsById.get(clientId).getName();
                    audit.info("[Voting WEB APP] SUCCESSFUL user login. User ID: {}.", clientId);
                    return new RequestResult<>(new SessionInfoWeb(userName, cookie, loggedUser.getRole()), null);
                } else {
                    audit.info("[Voting WEB APP] User login FAILED (INCORRECT PASSWORD). User ID: {}.", clientId);
                    return new RequestResult<>(APIException.INCORRECT_LOGIN_OR_PASSWORD);
                }
            } else {
                audit.info("[Voting WEB APP] User login FAILED (LOGIN NOT FOUND). Login: {}.", clientId);
                return new RequestResult<>(APIException.INCORRECT_LOGIN_OR_PASSWORD);
            }
        } catch (InternalLogicException e) {
            log.error("login failed. clientId {} error={}", clientId, e.getMessage());
            return new RequestResult<>(APIException.UNKNOWN_EXCEPTION);
        }
    }

    private String generateCookieAndLogin(LoggedUser user) {
        String cookie = UUID.randomUUID().toString();
        while (loggedUsers.putIfAbsent(cookie, user) != null) {
            log.trace(String.format("UUID is already in use: %s", cookie));
            cookie = UUID.randomUUID().toString();
        }
        audit.info("[Voting WEB APP] New cookie generated for user with ID: {}. Cookie: {}.", user.getClientId(), cookie);
        return cookie;
    }

    public LoggedUser tryGetLoggedUser(String cookie) {
        LoggedUser user = loggedUsers.get(cookie);
        if (user == null)
            return null;
        return user;
    }

    public RequestResult logout(String cookie) {
        log.debug("logout method called. cookie={}", cookie);
        final LoggedUser loggedUser = tryGetLoggedUser(cookie);
        if (loggedUser != null) {
            List<String> cookiesToDelete = loggedUsers.entrySet().stream().filter(entry -> entry.getValue().getClientId().equals(loggedUser.getClientId())).map(Map.Entry::getKey).collect(Collectors.toList());
            log.debug("logout. found {} sessions for client with id={}", cookiesToDelete.size(), loggedUser.getClientId());
            cookiesToDelete.forEach(loggedUsers::remove);
            log.debug("logout method executed successfully for client with id={}.", loggedUser.getClientId());
            final boolean loggedOut = !loggedUsers.containsKey(cookie);
            if (loggedOut) {
                audit.info("[Voting WEB APP] User is LOGGED OUT SUCCESSFULLY. User ID: {}.", loggedUser.getClientId());
            }
            return new RequestResult<>(true, null);
        }
        log.warn("logout. Couldn't find user by cookie to logout. Cookie: {}", cookie);
        return new RequestResult<>(APIException.WRONG_COOKIE);
    }

}
