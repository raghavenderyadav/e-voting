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

package uk.dsxt.voting.common.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestResult<T> {
    @Getter
    @JsonProperty("values")
    T[] values;

    @Getter
    @JsonProperty("error")
    String error;

    @JsonCreator
    public RequestResult(@JsonProperty("values") T[] values, @JsonProperty("error") String error) {
        this.values = values;
        this.error = error;
    }

    public RequestResult(T[] values) {
        this(values, null);
    }

    public RequestResult(String error) {
        this(null, error);
    }

    public boolean isSuccessful() {
        return error == null;
    }
}
