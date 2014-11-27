/*
 * Created on 24.01.2013
 *
 * Copyright(c) 1995 - 2013 T-Systems Multimedia Solutions GmbH
 * Riesaer Str. 5, 01129 Dresden
 * All rights reserved.
 */
package com.t_systems_mms.health.deployment.groovy;

import groovy.io.FileType;

import java.util.regex.Pattern
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.SystemUtils;


/**
 * Filter support.
 *
 * @author Stephan Schnabel (ssc@mms-dresden.de)
 * @author Stefan Schubert (stsh@mms-dresden.de)
 */
class ResourceFilter {

    Properties data
    boolean createBackup

    final def PATTERN = Pattern.compile(/\$\{health([^\$\{]+)\}/)

    ResourceFilter(String propertyFile) {
        this(propertyFile, true)
    }

    ResourceFilter(String propertyFile, boolean createBackup) {
        data = new Properties();
        println "[ResourceFilter] using property file " + propertyFile
        new File(propertyFile).withInputStream {
            stream -> data.load(stream);
        }
        this.createBackup = createBackup;
    }

    ResourceFilter(Properties data) {
        this(data, true)
    }

    ResourceFilter(Properties data, boolean createBackup) {
        this.data = data
        this.createBackup = createBackup
    }

    def filterDir(String src, String dest) {
        File srcDir = new File(src);
        File destDir = new File(dest);
        destDir.mkdirs()
        System.out.println('[ResourceFilter] filtering dir ' + srcDir.getAbsolutePath() + ' to ' + destDir.getAbsolutePath())
        srcDir.eachFileRecurse(FileType.FILES) {
            file -> 
                String destFile = file.getAbsolutePath().substring(file.getAbsolutePath().indexOf(src) + src.length() + 1)
                if (destFile.contains(File.separator)) {
                    String subDirs = destFile.substring(0, destFile.lastIndexOf(File.separator));
                    new File(dest + File.separator + '' + subDirs).mkdirs()
                }
                this.filterFile(file.getAbsolutePath(), dest + File.separator + '' + destFile)
        }
    }

    def filterDir(String dir) {
        File sourceDir = new File(dir);
        System.out.println("[ResourceFilter] filtering dir " + sourceDir.getAbsolutePath())
        sourceDir.eachFileRecurse(FileType.FILES) {
            file ->
            if (file.getAbsolutePath().contains(".bak") && createBackup) {
                System.out.println("[ResourceFilter] Ignoring " + file.getAbsolutePath())
                return
            }
            def bak = new File(file.getAbsolutePath() + ".bak")
            if (bak.exists() && createBackup) {
                System.out.println("[ResourceFilter] " + file.getAbsolutePath() + ".bak exists!")
                return
            }
            if (bak.exists() && !createBackup) {
                bak.delete();
            }
            FileUtils.moveFile(file, bak)
            this.filterFile(bak.getAbsolutePath(), file.getAbsolutePath())
            if (!createBackup) {
                bak.delete();
            }
        }
    }

    def filterFile(String src, String dest) {
        System.out.println('[ResourceFilter] filtering file ' + src + ' to ' + dest)
        File srcFile = new File(src)
        File destFile = new File(dest)
        destFile.write("", "UTF-8")
        destFile.delete()
        destFile.withPrintWriter("UTF-8", {
            writer ->
            srcFile.eachLine {
                line ->
                writer.println(this.filterLine(line));
            }
        })
    }

    def filterLine(String line) {
        String newLine = line;
        def matcher = PATTERN.matcher(newLine);
        while (matcher.find()) {
            def group = "health" + matcher.group(1);
            if (data.containsKey(group)) {
                def replacement = "\${" + group + "}"
                def property = data.getProperty(group)
                if (StringUtils.isBlank(property)) {
                    System.out.println("[ResourceFilter] Property for " + group + " is empty!")
                } else if (SystemUtils.IS_OS_WINDOWS && (property.toLowerCase().startsWith("c:") ||  property.toLowerCase().startsWith("d:"))) {
                    property = property.replace('\\', '/')
                }
                newLine = StringUtils.replace(newLine, replacement, property)
                matcher.reset(newLine)
            } else {
                System.out.println("[ResourceFilter] Property not contained in the properties file: '" + group + "'. Is ignored!")
            }
        }
        return newLine;
    }
}