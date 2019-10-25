/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package se.kth.jdbl.callgraph;

import org.apache.bcel.classfile.ClassParser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Constructs a callgraph out of a set of directories, classes and JAR archives.
 * Can combine multiple archives into a single call graph.
 */
public class JCallGraphModified {

    //--------------------------------/
    //-------- CLASS FIELD/S --------/
    //------------------------------/

    private List<String> allMethodsCalls;

    private static final Logger LOGGER = LogManager.getLogger(JCallGraphModified.class.getName());

    //--------------------------------/
    //-------- CONSTRUCTOR/S --------/
    //------------------------------/

    public JCallGraphModified() {
        this.allMethodsCalls = new LinkedList<>();
    }

    //--------------------------------/
    //------- PUBLIC METHOD/S -------/
    //------------------------------/

    public List<String> getAllMethodsCallsFromFile(String classPath) {
        processFile(new File(classPath));
        return allMethodsCalls;
    }

    public Map<String, Set<String>> runUsageAnalysis(String classPath) {
        Map<String, Set<String>> usageAnalysis = new HashMap<>();
        List<String> list = getAllMethodsCallsFromFile(classPath);
        for (String s : list) {

            // add classes with main methods
            String callerClass = s.split(" ")[0].split(":")[1];
            String callerMethod = s.split(" ")[0].split(":")[2];

            if (callerMethod.equals("main(java.lang.String[])")) {
                usageAnalysis.put(callerClass, new HashSet<>(Collections.singletonList(callerMethod)));
            }

            // consider the rest of classes
            String calledClass = s.split(" ")[1].split(":")[0].substring(3);
            String calledMethod = s.split(" ")[1].split(":")[1];

            if (!usageAnalysis.containsKey(calledClass)) { // add the class if is not in the map
                usageAnalysis.put(calledClass, new HashSet<>(Collections.singletonList(calledMethod)));
            } else { // add method if the class exists
                usageAnalysis.get(calledClass).add(calledMethod);
            }
        }
        return usageAnalysis;
    }

    //--------------------------------/
    //------ PRIVATE METHOD/S -------/
    //------------------------------/

    private void processClass(String className) throws IOException {
        ClassParser cp = new ClassParser(className);
        ClassVisitor visitor = new ClassVisitor(cp.parse());
        allMethodsCalls.addAll(visitor.start().methodCalls());

    }

    private void processClass(String jarName, String className) throws IOException {
        ClassParser cp = new ClassParser(jarName, className);
        ClassVisitor visitor = new ClassVisitor(cp.parse());
        allMethodsCalls.addAll(visitor.start().methodCalls());
    }

    private void processJar(JarFile jar) throws IOException {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            if (!entry.getName().endsWith(".class")) {
                continue;
            }
            processClass(jar.getName(), entry.getName());
        }
    }

    private void processFile(File file) {
        try {
            if (!file.exists()) {
                LOGGER.info("File " + file.getName() + " does not exist");
            } else if (file.isDirectory()) {
                for (File dfile : file.listFiles()) {
                    processFile(dfile);
                }
            } else if (file.getName().endsWith(".jar")) {
                processJar(new JarFile(file));
            } else if (file.getName().endsWith(".class")) {
                processClass(file.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("Error while processing file: " + e.getMessage());
        }
    }
}