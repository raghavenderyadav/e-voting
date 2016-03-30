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

package uk.dsxt.voting.client;

import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

@Log4j2
public class StateFileSerializer {
    
    private final File file;
    
    private static final Charset CHARSET = Charset.forName("utf-8");
    
    public StateFileSerializer(String filePath) {
        file = filePath == null || filePath.length() == 0 ? null : new File(filePath);
    }
    
    public String load() {
        if (file == null || !file.exists())
            return null;
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(file.getPath()));
            return new String(encoded, CHARSET);
        } catch (IOException e) {
            log.warn("load. Couldn't read state from file: {}. error={}", file.getAbsolutePath(), e.getMessage());
            return null;
        }
    }
    
    public void save(String state) {
        if (file == null)
            return;
        try {
            Files.write(file.toPath(), Arrays.asList(state), CHARSET, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            log.warn("save. Couldn't save state to file: {}. error={}", file.getAbsolutePath(), e.getMessage());
        }
    }
    
}
