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
package com.github.pfmiles.dropincc.impl.llstar;

import com.github.pfmiles.dropincc.impl.CAlternative;
import com.github.pfmiles.dropincc.impl.GruleType;

import java.util.List;

/**
 * Grammar rule with lookAhead DFA and alternatives.
 * 
 * @author pf-miles
 * 
 */
public class PredictingGrule {

    public final GruleType type;
    // the LL(*) look-ahead DFA
    public final LookAheadDfa dfa;
    // all alternative productions
    public final List<CAlternative> alts;

    // Non LL-regular grammar, no valid look-ahead dfa found, fallback to back
    // tracking
    private boolean backtrack;

    // if this rule is on the path of backtracking
    private final boolean onBacktrackPath;

    /**
     * Create a predicting grule with look-ahead DFA
     * 
     * @param type
     * @param dfa
     * @param alts
     */
    public PredictingGrule(GruleType type, LookAheadDfa dfa, List<CAlternative> alts, boolean onBacktrackPath) {
        super();
        this.type = type;
        this.dfa = dfa;
        this.alts = alts;
        this.onBacktrackPath = onBacktrackPath;
    }

    /**
     * Create a non-LL regular predicting grule, this kind of rule would do
     * backtracking at runtime
     * 
     * @param grule
     * @param alts
     */
    public PredictingGrule(GruleType grule, List<CAlternative> alts, boolean onBacktrackPath) {
        this.type = grule;
        this.dfa = null;
        this.alts = alts;
        this.backtrack = true;
        this.onBacktrackPath = onBacktrackPath;
    }


    public boolean isBacktrack() {
        return backtrack;
    }

    public boolean isOnBacktrackPath() {
        return onBacktrackPath;
    }

}
