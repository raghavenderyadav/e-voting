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

package uk.dsxt.voting.resultsbuilder;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.demo.ResultsBuilder;

import javax.ws.rs.*;

@Log4j2
@Path("results-api")
public class ResultsBuilderResource implements ResultsBuilder {

    private final ResultsManager manager;

    public ResultsBuilderResource(ResultsManager manager) {
        this.manager = manager;
    }

    private void execute(String name, Runnable action) {
        try {
            log.debug("{} called", name);
            action.run();
        } catch (Exception ex) {
            log.error("{} failed", name, ex);
        }
    }

    @Override
    @POST
    @Path("/addResult")
    public void addResult(@FormParam("holderId") String holderId, @FormParam("voteResult") String voteResult) {
        execute(String.format("addResult holderId=%s voteResult=%s", holderId, voteResult), () -> manager.addResult(holderId, voteResult));
    }

    @Override
    @POST
    @Path("/addVote")
    public void addVote(@FormParam("voteResult") String voteResult) {
        execute(String.format("addVote voteResult=%s", voteResult), () -> manager.addVote(voteResult));
    }
}
