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

package uk.dsxt.voting.resultsbuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.glassfish.jersey.server.ResourceConfig;
import uk.dsxt.voting.common.datamodel.*;
import uk.dsxt.voting.common.utils.JettyRunner;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import javax.ws.rs.ApplicationPath;
import java.util.Properties;

@Log4j2
@ApplicationPath("")
public class ResultsBuilderApplication extends ResourceConfig {

    public ResultsBuilderApplication(ResultsManager manager) throws InternalLogicException {
        JettyRunner.configureMapper(this);
        this.registerInstances(new ResultsBuilderResource(manager));
    }
}
