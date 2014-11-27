/*
 * Created on 24.01.2013
 *
 * Copyright(c) 1995 - 2013 T-Systems Multimedia Solutions GmbH
 * Riesaer Str. 5, 01129 Dresden
 * All rights reserved.
 */
package com.t_systems_mms.health.deployment.groovy;

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.SystemUtils

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * File support...
 *
 * @author Stephan Schnabel (ssc@mms-dresden.de)
 * @author Stefan Schubert (stsh@mms-dresden.de)
 */
public class FileUtil {

    static void copyFile(String source, String destination) {
        File sourceFile = new File(source)
        File destinationFile = new File(destination)
        System.out.println('[FileUtil] Copy file ' + sourceFile.getAbsolutePath() + ' to ' + destinationFile.getAbsolutePath())
        FileUtils.copyFile(sourceFile, destinationFile)
    }

    static void copyDirectory(String source, String destination) {
        File sourceFile = new File(source)
        File destinationFile = new File(destination)
        System.out.println('[FileUtil] Copy directory ' + sourceFile.getAbsolutePath() + ' to ' + destinationFile.getAbsolutePath())
        FileUtils.copyDirectory(sourceFile, destinationFile)
    }

    static void deleteDirectory(String directory) {
        File directoryFile = new File(directory)
        System.out.println('[FileUtil] Delete ' + directoryFile.getAbsolutePath())
        FileUtils.deleteDirectory(directoryFile)
    }

    static void deleteFile(String filename) {
        File file = new File(filename)
        System.out.println('[FileUtil] Delete ' + file.getAbsolutePath())
        file.delete()
    }

    static void unzipArchive(String archive, String destination) {
        File archiveFile = new File(archive)
        File destinationFile = new File(destination)
        System.out.println('[FileUtil] Unzipping file ' + archiveFile.getAbsolutePath() + ' to ' + destinationFile.getAbsolutePath())
        try {
            ZipFile zipfile = new ZipFile(archiveFile);
            for (Enumeration e = zipfile.entries(); e.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                unzipEntry(zipfile, entry, destinationFile);
            }
        } catch (Exception e) {
            System.err.println('[FileUtil] Error while unzipping file ' + archiveFile.getAbsolutePath())
            throw e
        }
    }

    private static void unzipEntry(ZipFile zipFile, ZipEntry entry, File destination) {
        if (entry.isDirectory()) {
            createDirectory(new File(destination, entry.getName()));
            return;
        }
        File destinationFile = new File(destination, entry.getName());
        if (!destinationFile.getParentFile().exists()) {
            createDirectory(destinationFile.getParentFile());
        }
        BufferedInputStream inputStream = new BufferedInputStream(zipFile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(destinationFile));
        try {
            IOUtils.copy(inputStream, outputStream);
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }

    static void createDirectory(File directory) {
        if (directory.exists()) return;
        if (!directory.mkdirs()) throw new RuntimeException('Can not create dir ' + directory);
    }

    static void createFile(String filename) {
        if (new File(filename).exists()) return;
        if (!new File(filename).createNewFile()) throw new RuntimeException('Can not create file ' + filename);
    }

    // chmod and chown for files and directories
    static void chownDirectory(String directory, String user) {
        // there's no chown on windows
        if (SystemUtils.IS_OS_UNIX) {
            File directoryFile = new File(directory)
            if (!directoryFile.isDirectory()) {
                System.err.println('[FileUtil] chownDirectory: this is not a directory, cant change its owner ' + directory)
                return
            }
            String command = "chown -R " + user + ":" + user + " " + directoryFile.getAbsolutePath()
            executeOnShell(command)
        }
    }

    static void chownFile(String filename, String user) {
        // there's no chown on windows
        if (SystemUtils.IS_OS_UNIX) {
            File file = new File(filename)
            if (!file.isFile()) {
                System.err.println('[FileUtil] chownFile: this is not a file or cant find it, hence cant change its owner ' + filename)
                return
            }
            String command = "chown " + user + ":" + user + " " + file.getAbsoluteFile()
            executeOnShell(command)
        }
    }

    static void chmodDirectory(String directory, String permission) {
        // there's no chmod on windows
        if (SystemUtils.IS_OS_UNIX) {
            File directoryFile = new File(directory)
            if (!directoryFile.isDirectory()) {
                System.err.println('[FileUtil] chmodDirectory: this is not a directory, cant change permissions ' + directory)
                return
            }
            String command = "chmod -R " + permission + " " + directoryFile.getAbsolutePath()
            executeOnShell(command)
        }
    }

    static void chmodFile(String filename, String permission) {
        // there's no chmod on windows
        if (SystemUtils.IS_OS_UNIX) {
            File file = new File(filename)
            if (!file.isFile()) {
                System.err.println('[FileUtil] chmodDirectory: this is not a file or cant find it, hence cant change permissions ' + filename)
                return
            }
            String command = "chmod " + permission + " " + file.getAbsoluteFile()
            executeOnShell(command)
        }
    }

    static void chmodSetExecuteToShellFiles(String directory) {
        // there's no chmod on windows
        if (SystemUtils.IS_OS_UNIX) {
            File directoryFile = new File(directory)
            if (!directoryFile.isDirectory()) {
                System.err.println('[FileUtil] chmodSetExecuteToShellFiles: this is not a directory, cant change permissions ' + directory)
                return
            }
            directoryFile.listFiles()
            for (File file : directoryFile.listFiles())  {
                if(file.name.endsWith(".sh")){
                    file.setExecutable(true,false)
                }
            }
        }
    }

    //helper
    static void executeOnShell(String command) {
        executeOnShell(command, new File(System.properties.'user.dir'))
    }

    static void executeOnShell(String command, File workingDir) {
        println '[FileUtil] executing command ' + command

        List<String> commandList = command.split(' ').toList()
        def process = new ProcessBuilder(commandList).directory(workingDir).redirectErrorStream(true).start()
        process.inputStream.eachLine {println it}
        process.waitFor();
        if (process.exitValue() != 0) {
            System.err.println('[FileUtil] Error while executing' + command + ' in Directory ' + workingDir)
            throw new Exception()
        }
    }
}