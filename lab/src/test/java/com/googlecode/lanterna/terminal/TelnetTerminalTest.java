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

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminal;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminalServer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;

/**
 *
 * @author martin
 */
public class TelnetTerminalTest {
    public static void main(String[] args) throws IOException {
        TelnetTerminalServer server = new TelnetTerminalServer(1024, Charset.forName("utf-8"));
        //noinspection InfiniteLoopStatement
        while(true) {
            TelnetTerminal telnetTerminal = server.acceptConnection();
            if(telnetTerminal != null) {
                spawnColorTest(telnetTerminal);
            }
        }
    }

    private static void spawnColorTest(final TelnetTerminal terminal) {
        new Thread() {
            
            private volatile TerminalPosition size;
            
            @Override
            public void run() {
                try {
                    final String string = "Hello!";
                    Random random = new Random();
                    terminal.enterPrivateMode();
                    terminal.clearScreen();
                    terminal.addResizeListener(new TerminalResizeListener() {
                        @Override
                        public void onResized(Terminal terminal, TerminalPosition newSize) {
                            System.err.println("Resized to " + newSize);
                            size = newSize;
                        }
                    });
                    size = terminal.terminalSize();

                    while(true) {
                        KeyStroke key = terminal.pollInput();
                        if(key != null) {
                            System.out.println(key);
                            if(key.getKeyType() == KeyType.Escape) {
                                terminal.exitPrivateMode();
                                return;
                            }
                        }

                        TextColor.Indexed foregroundIndex = TextColor.Indexed.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255));
                        TextColor.Indexed backgroundIndex = TextColor.Indexed.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255));

                        terminal.fore(foregroundIndex);
                        terminal.back(backgroundIndex);
                        terminal.moveCursorTo(random.nextInt(size.column - string.length()), random.nextInt(size.row));
                        printString(terminal, string);

                        try {
                            Thread.sleep(200);
                        }
                        catch(InterruptedException e) {
                        }
                    }
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
                finally {
                    try {
                        terminal.close();
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
    
    private static void printString(Terminal terminal, String string) throws IOException {
        for(int i = 0; i < string.length(); i++)
            terminal.put(string.charAt(i));
        terminal.flush();
    }
}
