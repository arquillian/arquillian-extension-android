/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.android.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.android.configuration.AndroidExtensionConfiguration;

/**
 * An utility which encapsules external process execution
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 *
 */
class ProcessExecutor {

    private Map<Process, Thread> shutdownThreads;
    private AndroidExtensionConfiguration configuration;

    public ProcessExecutor(AndroidExtensionConfiguration configuration) {
        this.configuration = configuration;
        this.shutdownThreads = new HashMap<Process, Thread>();
    }

    public Process spawn(String... command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(sanitizeArguments(command));
        final Process p = builder.start();
        AndroidConsoleConsumer consumer = new AndroidConsoleConsumer(p, configuration);
        new Thread(consumer).start();
        Thread shutdownThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (p != null) {
                    p.destroy();
                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        shutdownThreads.put(p, shutdownThread);

        return p;
    }

    public List<String> execute(Map<String, String> input, String... command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(sanitizeArguments(command));
        builder.redirectErrorStream(true);
        final Process p = builder.start();

        AndroidConsoleConsumer consumer = new AndroidConsoleConsumer(p, configuration, input);
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();

        try {
            p.waitFor();
            consumerThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return consumer.getOutputList();
    }

    public List<String> execute(String... command) throws IOException {
        return execute(Collections.<String, String> emptyMap(), command);
    }

    public ProcessExecutor removeShutdownHook(Process p) {
        shutdownThreads.remove(p);
        return this;
    }

    private List<String> sanitizeArguments(String... command) {
        List<String> cmd = new ArrayList<String>(command.length);
        for (String c : command) {
            if (c != null && c.length() > 0) {
                cmd.add(c);
            }
        }

        return cmd;
    }

    /**
     * Runnable that consumes the output of the process.
     *
     * @author Stuart Douglas
     * @author Karel Piwko
     */
    private static class AndroidConsoleConsumer implements Runnable {

        private static final String NL = System.getProperty("line.separator");

        private final Process process;

        private final AndroidExtensionConfiguration configuration;

        private final List<String> outputList;
        private final Map<String, String> input;

        public AndroidConsoleConsumer(Process process, AndroidExtensionConfiguration configuration, Map<String, String> input) {
            this.process = process;
            this.configuration = configuration;
            this.outputList = new ArrayList<String>();
            this.input = input;
        }

        public AndroidConsoleConsumer(Process process, AndroidExtensionConfiguration configuration) {
            this(process, configuration, Collections.<String, String> emptyMap());
        }

        @Override
        public void run() {
            final InputStream stream = process.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            final boolean writeOutput = configuration.isVerbose();

            try {

                // read character by character
                int i;
                StringBuilder line = new StringBuilder();
                while ((i = reader.read()) != -1) {
                    char c = (char) i;

                    // add the character
                    line.append(c);
                    if (writeOutput) {
                        System.out.print(c);
                        System.out.flush();
                    }

                    // check if we are have to respond with an input
                    String key = line.toString();
                    if (input.containsKey(key)) {
                        OutputStream ostream = process.getOutputStream();
                        ostream.write(input.get(key).getBytes());
                        ostream.flush();
                    }

                    // save output
                    if (line.indexOf(NL) != -1) {
                        outputList.add(line.toString());
                        line = new StringBuilder();
                    }
                }
                if (line.length() > 1) {
                    outputList.add(line.toString());
                }
            } catch (IOException e) {
            }
        }

        public List<String> getOutputList() {
            return outputList;
        }

    }

}
