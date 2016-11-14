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
package com.github.pfmiles.dropincc.impl.kleene;

import com.github.pfmiles.dropincc.DropinccException;
import com.github.pfmiles.dropincc.impl.EleType;

import java.io.Serializable;
import java.util.List;

/**
 * @author pf-miles
 * 
 */
public class CKleeneNode implements Serializable {

    private static final long serialVersionUID = 2432393334358498343L;
    private final List<EleType> contents;

    public CKleeneNode(List<EleType> contents) {
        if (contents == null || contents.isEmpty()) {
            throw new DropinccException("Cannot create empty kleene node.");
        }
        this.contents = contents;
    }

    public List<EleType> getContents() {
        return contents;
    }

    // this class requires the same hashCode method to the Object class
    public int hashCode() {
        return super.hashCode();
    }

    public String toString() {
        return "CKleeneNode(" + contents + ')';
    }

}
