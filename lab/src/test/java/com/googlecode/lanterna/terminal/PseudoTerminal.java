/*
 * This file is part of lanterna (http://code.google.com/p/lanterna/).
 *
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010-2016 Martin
 */
package com.googlecode.lanterna.terminal;

import com.googlecode.lanterna.TestTerminalFactory;
import com.googlecode.lanterna.input.WriteInput;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author martin
 */
public class PseudoTerminal {

    public static void main(String[] args) throws InterruptedException, IOException {
        final Terminal rawTerminal = new TestTerminalFactory(args).createTerminal();

        //assume bash is available
        Process bashProcess = Runtime.getRuntime().exec("bash", makeEnvironmentVariables());
        ProcessOutputReader stdout = new ProcessOutputReader(bashProcess.getInputStream(), rawTerminal);
        ProcessOutputReader stderr = new ProcessOutputReader(bashProcess.getErrorStream(), rawTerminal);
        WriteInput stdin = new WriteInput(bashProcess.getOutputStream(), rawTerminal);
        stdout.start();
        stderr.start();
        stdin.start();
        int returnCode = bashProcess.waitFor();
        stdout.stop();
        stderr.stop();
        stdin.stop();
        System.exit(returnCode);
    }

    private static String[] makeEnvironmentVariables() {
        List<String> environment = new ArrayList<String>();
        Map<String, String> env = new TreeMap<String, String>(System.getenv());
        env.put("TERM", "xterm");   //Will this make bash detect us as a proper terminal??
        for(String key : env.keySet()) {
            environment.add(key + "=" + env.get(key));
        }
        return environment.toArray(new String[environment.size()]);
    }

    private static class ProcessOutputReader {

        private final InputStreamReader inputStreamReader;
        private final Terminal terminalEmulator;
        private boolean stop;

        public ProcessOutputReader(InputStream inputStream, Terminal terminalEmulator) {
            this.inputStreamReader = new InputStreamReader(inputStream, Charset.defaultCharset());
            this.terminalEmulator = terminalEmulator;
            this.stop = false;
        }

        private void start() {
            new Thread("OutputReader") {
                @Override
                public void run() {
                    try {
                        char[] buffer = new char[1024];
                        int readCharacters = inputStreamReader.read(buffer);
                        while(readCharacters != -1 && !stop) {
                            if(readCharacters > 0) {
                                for(int i = 0; i < readCharacters; i++) {
                                    terminalEmulator.put(buffer[i]);
                                }
                                terminalEmulator.flush();
                            }
                            else {
                                try {
                                    Thread.sleep(1);
                                }
                                catch(InterruptedException e) {
                                }
                            }
                            readCharacters = inputStreamReader.read(buffer);
                        }
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                    finally {
                        try {
                            inputStreamReader.close();
                        }
                        catch(IOException e) {
                        }
                    }
                }
            }.start();
        }

        private void stop() {
            stop = true;
        }
    }

}
