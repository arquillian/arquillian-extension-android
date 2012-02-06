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

import org.jboss.arquillian.android.configuration.AndroidSdkConfiguration;

class ProcessExecutor {

    private Map<Process, Thread> shutdownThreads;
    private AndroidSdkConfiguration configuration;

    public ProcessExecutor(AndroidSdkConfiguration configuration) {
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

        private final AndroidSdkConfiguration configuration;

        private final List<String> outputList;
        private final Map<String, String> input;

        public AndroidConsoleConsumer(Process process, AndroidSdkConfiguration configuration, Map<String, String> input) {
            this.process = process;
            this.configuration = configuration;
            this.outputList = new ArrayList<String>();
            this.input = input;
        }

        public AndroidConsoleConsumer(Process process, AndroidSdkConfiguration configuration) {
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
