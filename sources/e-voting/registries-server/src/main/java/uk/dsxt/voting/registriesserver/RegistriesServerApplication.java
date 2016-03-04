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

package uk.dsxt.voting.registriesserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.glassfish.jersey.server.ResourceConfig;
import org.joda.time.Instant;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.web.JettyRunner;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import javax.ws.rs.ApplicationPath;
import java.util.Properties;

@Log4j2
@ApplicationPath("")
public class RegistriesServerApplication extends ResourceConfig {

    private ObjectMapper mapper = new ObjectMapper();

    public RegistriesServerApplication(Properties properties, String[] args) throws InternalLogicException {
        //loading properties
        String subdirectory = null;
        Integer votingDuration = null;
        if (args != null && args.length > 1) {
            subdirectory = args[0];
            votingDuration = Integer.valueOf(args[1]);
            log.info(String.format("Testing mode. subdirectory: '%s'. votingDuration: %s minutes", subdirectory, votingDuration));
        }
        Participant[] participants = loadResource(properties, subdirectory, "participants.filepath", Participant[].class);
        Voting[] votings = loadResource(properties, subdirectory, "votings.filepath", Voting[].class);

        if (votingDuration != null) {
            long now = Instant.now().getMillis();
            for (int i = 0; i < votings.length; i++) {
                votings[i] = new Voting(votings[i].getId(), votings[i].getName(), now, now + votingDuration * 60000, votings[i].getQuestions());
            }
        }
        //initialization
        RegistriesServerManager manager = new RegistriesServerManager(participants);
        JettyRunner.configureMapper(this);
        this.registerInstances(new RegistriesServerResource(manager));
    }

    private <T> T loadResource(Properties properties, String subdirectory, String propertyName, Class<T> clazz) throws InternalLogicException {
        String path = properties.getProperty(propertyName);
        path = String.format(path, subdirectory == null ? "" : subdirectory);
        String resourceJson = PropertiesHelper.getResourceString(path);
        if (resourceJson.isEmpty())
            throw new InternalLogicException(String.format("Couldn't find file for %s property with value '%s'.", propertyName, path));
        try {
            return mapper.readValue(resourceJson, clazz);
        } catch (Exception ex) {
            throw new InternalLogicException(String.format("Couldn't parse '%s' file for type %s due to %s", path, clazz, ex.getMessage()));
        }
    }

}
