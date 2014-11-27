package com.t_systems_mms.health.deployment;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Starting point for deployment groovy calls.
 */
public class GroovyRun {

    /**
     * Main method that expects at least two paramter: environment and 1...n goals.
     *
     * @param args - arguments
     * @throws IOException error occured
     */
    public static void main(String[] args) throws IOException {

        // parameter?
        if (args.length != 2) {
            System.err.print("[Main] Need environment and one goals.");
            System.exit(-1);
        }
        String environment = args[0];
        String goal = args[1];

        // load classes
        Class<?> deployment;
        try (GroovyClassLoader classLoader = new GroovyClassLoader()) {
            deployment = classLoader.parseClass(new File("com/t_systems_mms/health/deployment/groovy/Deployment.groovy"));
            classLoader.parseClass(new File("com/t_systems_mms/health/deployment/groovy/PropertyStore.groovy"));
            classLoader.parseClass(new File("com/t_systems_mms/health/deployment/groovy/ResourceFilter.groovy"));
            classLoader.parseClass(new File("com/t_systems_mms/health/deployment/groovy/FileUtil.groovy"));
            classLoader.close();
        }

        // execute deployment
        System.out.println("Groovy - Call deployment with environment " + environment + " and goas " + goal + ".");
        try {
            deployment.getDeclaredConstructor(String.class, String.class).newInstance(environment, goal);
        } catch (Exception exception) {
            System.out.println(ExceptionUtils.getStackTrace(exception));
            System.exit(-1);
        }
    }
}