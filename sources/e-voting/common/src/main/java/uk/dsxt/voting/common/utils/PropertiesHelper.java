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

package uk.dsxt.voting.common.utils;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@Log4j2
public class PropertiesHelper {

    public static Properties loadPropertiesFromPath(String nxtPropertiesPath) {
        try {
            return loadPropertiesFromPath(nxtPropertiesPath, new File(nxtPropertiesPath).toURI().toURL());
        } catch (Exception e) {
            return new Properties();
        }
    }

    public static Properties loadProperties(String moduleName) {
        URL propertiesURL = getConfOrResourceFile(String.format("%s.properties", moduleName));
        return loadPropertiesFromPath(moduleName, propertiesURL);
    }

    private static Properties loadPropertiesFromPath(String moduleName, URL propertiesURL) {
        Properties properties = new Properties();
        if (propertiesURL != null) {
            try (InputStream resourceStream = propertiesURL.openStream()) {
                properties.load(resourceStream);
                log.info("Loading {} properties from file: {}", moduleName.toUpperCase(), propertiesURL);
            } catch (Exception e) {
                log.error("Couldn't load {} properties from file: {}", moduleName.toUpperCase(), propertiesURL, e);
            }
        } else {
            log.info("Couldn't find {} properties file", moduleName.toUpperCase());
        }
        return properties;
    }

    private static URL getResource(String fileName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader.getResource(fileName);
    }

    public static String getResourceString(String name) {
        try {
            final File resourceFile = getConfFile(name);
            if (resourceFile.exists()) {
                try {
                    byte[] encoded = Files.readAllBytes(Paths.get(resourceFile.getPath()));
                    log.debug("getResourceString. Resource ({}) found: {}", name, resourceFile.getAbsolutePath());
                    return new String(encoded);
                } catch (IOException e) {
                    log.warn("getResourceString. Couldn't read resource from file: {}. error={}", resourceFile.getAbsolutePath(), e.getMessage());
                }
            }

            log.info("getResourceString. Loading resource from jar: {}", name);
            final URL resource = getResource(name);
            if (resource == null) {
                log.warn("getResourceString. Couldn't load resource from jar file: {}.", name);
                return "";
            }
            return IOUtils.toString(resource.openStream());
        } catch (Exception e) {
            log.error("getResourceString. Couldn't read a resource file.", e);
        }
        return "";
    }

    private static File getConfFile(String fileName) {
        String parentPath = getParentFile(PropertiesHelper.class).getAbsolutePath();
        return new File(new File(parentPath), fileName);
    }

    public static File getParentFile(Class cls) {
        return new File(cls.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
    }

    public static URL getConfOrResourceFile(String fileName) {
        File confFile = getConfFile(fileName);
        log.debug("Configuration path: {}", confFile.getAbsolutePath());
        if (confFile.exists()) {
            try {
                return confFile.toURI().toURL();
            } catch (MalformedURLException e) {
                log.error("Couldn't get URL from file: {}", confFile, e);
            }
        }
        return getResource(fileName);
    }
}
