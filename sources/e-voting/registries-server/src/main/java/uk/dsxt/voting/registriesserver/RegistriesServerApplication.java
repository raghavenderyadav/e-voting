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
import uk.dsxt.voting.common.datamodel.BlockedPacket;
import uk.dsxt.voting.common.datamodel.Holding;
import uk.dsxt.voting.common.datamodel.Participant;
import uk.dsxt.voting.common.datamodel.Voting;
import uk.dsxt.voting.common.utils.JettyRunner;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import javax.ws.rs.ApplicationPath;
import java.util.Properties;

@Log4j2
@ApplicationPath("")
public class RegistriesServerApplication extends ResourceConfig {

    public RegistriesServerApplication(Properties properties) throws Exception {
        //loading properties
        ObjectMapper mapper = new ObjectMapper();
        String participantsJson = PropertiesHelper.getResourceString(properties.getProperty("participants.filepath"));
        Participant[] participants = mapper.readValue(participantsJson, Participant[].class);
        String holdingsJson = PropertiesHelper.getResourceString(properties.getProperty("holdings.filepath"));
        Holding[] holdings = mapper.readValue(holdingsJson, Holding[].class);
        String votingJson = PropertiesHelper.getResourceString(properties.getProperty("voting.filepath"));
        Voting voting = mapper.readValue(votingJson, Voting.class);
        String blackListJson = PropertiesHelper.getResourceString(properties.getProperty("blacklist.filepath"));
        BlockedPacket[] blackList = mapper.readValue(blackListJson, BlockedPacket[].class);
        //initialization
        RegistriesServerManager manager = new RegistriesServerManager(participants, holdings, new Voting[] {voting}, blackList);
        JettyRunner.configureMapper(this);
        this.registerInstances(new RegistriesServerResource(manager));
    }
}
