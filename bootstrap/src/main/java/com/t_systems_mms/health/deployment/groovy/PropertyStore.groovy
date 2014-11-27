/*
 * Created on 24.01.2013
 *
 * Copyright(c) 1995 - 2013 T-Systems Multimedia Solutions GmbH
 * Riesaer Str. 5, 01129 Dresden
 * All rights reserved.
 */
package com.t_systems_mms.health.deployment.groovy;

/**
 * Stores properties from different files... 
 *
 * @author Stephan Schnabel (ssc@mms-dresden.de)
 */
class PropertyStore {

    static Properties properties = System.getProperties()

    static void loadFile(String filename) {
        File file = new File(filename)
        if (!file.exists()) {
            throw new IllegalArgumentException('Properties file is not valid, filename: ' + file.getAbsolutePath())
        }
        properties.load(new FileInputStream(file))
        System.out.println('[PropertyStore] Properties file loaded: ' + filename)
    }

    static String get(String key) {
        System.out.println('[PropertyStore] Got ' + key + '=' + properties.get(key))
        return properties.get(key)
    }

    static String replace(String value) {
        int indexStart = value.indexOf('${health.')
        int indexEnd = value.indexOf('}', indexStart)
        if (indexStart != -1 && indexEnd != -1) {
            String key = value.substring(indexStart + 2, indexEnd)
            return replace(value.replace('${' + key + '}', get(key)))
        }
        return value
    }
}