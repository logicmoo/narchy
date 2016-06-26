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
package com.googlecode.lanterna.screen;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.graphics.TextImage;

import java.io.IOException;

/**
 * This class implements some of the Screen logic that is not directly tied to the actual implementation of how the
 * Screen translate to the terminal. It keeps data structures for the front- and back buffers, the cursor location and
 * some other simpler states.
 * @author martin
 */
public abstract class AbstractScreen implements Screen {
    private TerminalPosition cursorPosition;
    private ScreenBuffer backBuffer;
    private ScreenBuffer frontBuffer;
    private final TextCharacter defaultCharacter;

    //How to deal with \t characters
    private TabBehaviour tabBehaviour;

    //Current size of the screen
    private TerminalPosition terminalPosition;

    //Pending resize of the screen
    private TerminalPosition latestResizeRequest;

    public AbstractScreen(TerminalPosition initialSize) {
        this(initialSize, DEFAULT_CHARACTER);
    }

    /**
     * Creates a new Screen on top of a supplied terminal, will query the terminal for its size. The screen is initially
     * blank. You can specify which character you wish to be used to fill the screen initially; this will also be the
     * character used if the terminal is enlarged and you don't set anything on the new areas.
     *
     * @param initialSize Size to initially create the Screen with (can be resized later)
     * @param defaultCharacter What character to use for the initial state of the screen and expanded areas
     */
    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    public AbstractScreen(TerminalPosition initialSize, TextCharacter defaultCharacter) {
        this.frontBuffer = new ScreenBuffer(initialSize, defaultCharacter);
        this.backBuffer = new ScreenBuffer(initialSize, defaultCharacter);
        this.defaultCharacter = defaultCharacter;
        this.cursorPosition = new TerminalPosition(0, 0);
        this.tabBehaviour = TabBehaviour.ALIGN_TO_COLUMN_4;
        this.terminalPosition = initialSize;
        this.latestResizeRequest = null;
    }

    /**
     * @return Position where the cursor will be located after the screen has been refreshed or {@code null} if the
     * cursor is not visible
     */
    @Override
    public TerminalPosition cursorPosition() {
        return cursorPosition;
    }

    /**
     * Moves the current cursor position or hides it. If the cursor is hidden and given a new position, it will be
     * visible after this method call.
     *
     * @param position 0-indexed column and row numbers of the new position, or if {@code null}, hides the cursor
     */
    @Override
    public void moveCursorTo(TerminalPosition position) {
        if(position == null) {
            //Skip any validation checks if we just want to hide the cursor
            this.cursorPosition = null;
            return;
        }
        if(position.column < 0) {
            position = position.withColumn(0);
        }
        if(position.row < 0) {
            position = position.withRow(0);
        }
        if(position.column >= terminalPosition.column) {
            position = position.withColumn(terminalPosition.column - 1);
        }
        if(position.row >= terminalPosition.row) {
            position = position.withRow(terminalPosition.row - 1);
        }
        this.cursorPosition = position;
    }

    @Override
    public void setTabBehaviour(TabBehaviour tabBehaviour) {
        if(tabBehaviour != null) {
            this.tabBehaviour = tabBehaviour;
        }
    }

    @Override
    public TabBehaviour getTabBehaviour() {
        return tabBehaviour;
    }

    @Override
    public void set(TerminalPosition position, TextCharacter screenCharacter) {
        set(position.column, position.row, screenCharacter);
    }

    @Override
    public TextGraphics newTextGraphics() {
        return new ScreenTextGraphics(this) {
            @Override
            public TextGraphics drawImage(TerminalPosition topLeft, TextImage image, TerminalPosition sourceImageTopLeft, TerminalPosition sourceImageSize) {
                backBuffer.copyFrom(image, sourceImageTopLeft.row, sourceImageSize.row, sourceImageTopLeft.column, sourceImageSize.column, topLeft.row, topLeft.column);
                return this;
            }
        };
    }

    @Override
    public synchronized void set(int column, int row, TextCharacter screenCharacter) {
        //It would be nice if we didn't have to care about tabs at this level, but we have no such luxury
        if(screenCharacter.c == '\t') {
            //Swap out the tab for a space
            screenCharacter = screenCharacter.withCharacter(' ');

            //Now see how many times we have to put spaces...
            for(int i = 0; i < tabBehaviour.replaceTabs("\t", column).length(); i++) {
                backBuffer.set(column + i, row, screenCharacter);
            }
        }
        else {
            //This is the normal case, no special character
            backBuffer.set(column, row, screenCharacter);
        }

        //Pad CJK character with a trailing space
        if(TerminalTextUtils.isCharCJK(screenCharacter.c)) {
            backBuffer.set(column + 1, row, screenCharacter.withCharacter(' '));
        }
        //If there's a CJK character immediately to our left, reset it
        if(column > 0) {
            TextCharacter cjkTest = backBuffer.get(column - 1, row);
            if(cjkTest != null && TerminalTextUtils.isCharCJK(cjkTest.c)) {
                backBuffer.set(column - 1, row, backBuffer.get(column - 1, row).withCharacter(' '));
            }
        }
    }

    @Override
    public synchronized TextCharacter front(TerminalPosition position) {
        return front(position.column, position.row);
    }

    @Override
    public TextCharacter front(int column, int row) {
        return getCharacterFromBuffer(frontBuffer, column, row);
    }

    @Override
    public synchronized TextCharacter back(TerminalPosition position) {
        return back(position.column, position.row);
    }

    @Override
    public TextCharacter back(int column, int row) {
        return getCharacterFromBuffer(backBuffer, column, row);
    }

    @Override
    public void refresh() throws IOException {
        refresh(RefreshType.AUTOMATIC);
    }

    @Override
    public synchronized void clear() {
        backBuffer.setAll(defaultCharacter);
    }

    @Override
    public synchronized TerminalPosition doResizeIfNecessary() {
        TerminalPosition pendingResize = getAndClearPendingResize();
        if(pendingResize == null) {
            return null;
        }

        backBuffer = backBuffer.resize(pendingResize, defaultCharacter);
        frontBuffer = frontBuffer.resize(pendingResize, defaultCharacter);
        return pendingResize;
    }

    @Override
    public TerminalPosition terminalSize() {
        return terminalPosition;
    }

    /**
     * Returns the front buffer connected to this screen, don't use this unless you know what you are doing!
     * @return This Screen's front buffer
     */
    protected ScreenBuffer getFrontBuffer() {
        return frontBuffer;
    }

    /**
     * Returns the back buffer connected to this screen, don't use this unless you know what you are doing!
     * @return This Screen's back buffer
     */
    protected ScreenBuffer getBackBuffer() {
        return backBuffer;
    }

    private synchronized TerminalPosition getAndClearPendingResize() {
        if(latestResizeRequest != null) {
            terminalPosition = latestResizeRequest;
            latestResizeRequest = null;
            return terminalPosition;
        }
        return null;
    }

    /**
     * Tells this screen that the size has changed and it should, at next opportunity, resize itself and its buffers
     * @param newSize New size the 'real' terminal now has
     */
    protected void addResizeRequest(TerminalPosition newSize) {
        latestResizeRequest = newSize;
    }

    private static TextCharacter getCharacterFromBuffer(ScreenBuffer buffer, int column, int row) {
        if(column > 0) {
            //If we are picking the padding of a CJK character, pick the actual CJK character instead of the padding
            TextCharacter leftOfSpecifiedCharacter = buffer.get(column - 1, row);
            if(leftOfSpecifiedCharacter == null) {
                //If the character left of us doesn't exist, we don't exist either
                return null;
            }
            else if(TerminalTextUtils.isCharCJK(leftOfSpecifiedCharacter.c)) {
                return leftOfSpecifiedCharacter;
            }
        }
        return buffer.get(column, row);
    }
    
    @Override
    public String toString() {
        return getBackBuffer().toString();
    }

    /**
     * Performs the scrolling on its back-buffer.
     */
    @Override
    public void scrollLines(int firstLine, int lastLine, int distance) {
        getBackBuffer().scrollLines(firstLine, lastLine, distance);
    }
}
