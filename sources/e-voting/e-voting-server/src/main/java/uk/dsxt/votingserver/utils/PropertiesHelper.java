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

package uk.dsxt.votingserver.utils;

import lombok.extern.log4j.Log4j;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

@Log4j
public class PropertiesHelper {
    public static Properties loadProperties(String moduleName) {
        Properties properties = new Properties();
        URL propertiesURL = getResource(String.format("%s.properties", moduleName));

        if (propertiesURL != null) {
            try (InputStream resourceStream = propertiesURL.openStream()) {
                properties.load(resourceStream);
                log.info(String.format("Loading %s properties from file: %s", moduleName.toUpperCase(), propertiesURL));
            } catch (Exception e) {
                log.error(String.format("Couldn't load %s properties from file: %s", moduleName.toUpperCase(), propertiesURL), e);
            }
        } else {
            log.info(String.format("Couldn't find %s properties file", moduleName.toUpperCase()));
        }
        return properties;
    }

    private static URL getResource(String fileName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader.getResource(fileName);
    }
}
