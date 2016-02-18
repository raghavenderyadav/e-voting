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

package uk.dsxt.voting.tests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientConfiguration {
    String holderId;
    String privateKey;
    @NonFinal
    @Setter
    boolean isHonestParticipant;
    String vote;
    @NonFinal
    @Setter
    String disconnectMask;

    @JsonCreator
    public ClientConfiguration(@JsonProperty("holderId") String holderId, @JsonProperty("privateKey") String privateKey,
                               @JsonProperty("isHonestParticipant") boolean isHonestParticipant, @JsonProperty("vote") String vote,
                               @JsonProperty("disconnectMask") String disconnectMask) {
        this.holderId = holderId;
        this.privateKey = privateKey;
        this.isHonestParticipant = isHonestParticipant;
        this.vote = vote;
        this.disconnectMask = disconnectMask;
    }
}


