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

package uk.dsxt.voting.client.web;

import lombok.extern.log4j.Log4j2;
import org.glassfish.jersey.server.ResourceConfig;
import uk.dsxt.voting.common.utils.web.JettyRunner;

import javax.ws.rs.ApplicationPath;

@Log4j2
@ApplicationPath("")
public class MockVotingAPIApplication extends ResourceConfig {
    public MockVotingAPIApplication() {
        JettyRunner.configureMapper(this);
        this.registerInstances(new MockVotingApiResource());
    }
}
