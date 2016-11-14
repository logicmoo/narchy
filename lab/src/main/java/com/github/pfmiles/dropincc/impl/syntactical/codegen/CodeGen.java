/*******************************************************************************
 * Copyright (c) 2012 pf_miles.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     pf_miles - initial API and implementation
 ******************************************************************************/
package com.github.pfmiles.dropincc.impl.syntactical.codegen;

import com.github.pfmiles.dropincc.DropinccException;
import com.github.pfmiles.dropincc.impl.util.ByteAppender;

import java.io.IOException;
import java.io.InputStream;

/**
 * Common interface of all code rendering elements of a parser.
 * 
 * @author pf-miles
 * 
 */
public abstract class CodeGen {

    // use java built-in messageFormats as templates
    protected static String getTemplate(String name, Class<? extends CodeGen> cls) {
        return readStrStream(cls.getResourceAsStream(name));
    }

    private static String readStrStream(InputStream stm) {
        if (stm == null)
            return null;
        ByteAppender ba = new ByteAppender();
        try {
            byte[] buf = new byte[1024];
            int count = stm.read(buf);
            while (count != -1) {
                ba.append(buf, 0, count);
                count = stm.read(buf);
            }
            return new String(ba.toByteArray(), "UTF-8");
        } catch (IOException e) {
            throw new DropinccException(e);
        }
    }

    public abstract <T> T render(CodeGenContext context);
}
