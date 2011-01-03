/*
 * Copyright Matt Palmer 2009-2011, All rights reserved.
 *
 */

package net.domesdaybook.matcher.sequence.searcher;

import net.domesdaybook.matcher.sequence.SequenceMatcher;
import net.domesdaybook.searcher.Searcher;

/**
 *
 * @author matt
 */
public abstract class SequenceMatcherSearcher implements Searcher {

    protected final SequenceMatcher matcher;

    public SequenceMatcherSearcher(final SequenceMatcher matcher) {
        this.matcher = matcher;
    }

    public final SequenceMatcher getMatcher() {
        return matcher;
    }

}