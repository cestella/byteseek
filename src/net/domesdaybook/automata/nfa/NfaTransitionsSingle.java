/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.domesdaybook.automata.nfa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.domesdaybook.automata.State;
import net.domesdaybook.automata.Transition;

/**
 *
 * @author matt
 */
public class NfaTransitionsSingle implements NfaTransitionsCollection {

    private Transition transition;
    private Set<NfaState> stateSet;

    public NfaTransitionsSingle(final Transition transition) {
        setTransition(transition);
    }

    public void addTransition(final Transition transition) {
        setTransition(transition);
    }

    private void setTransition(final Transition transition) {
        this.transition = transition;
        stateSet = new HashSet<NfaState>();
        stateSet.add((NfaState) transition.getToState());
    }

    public final Set<NfaState> getStatesForByte(final byte theByte) {
        final State state = transition.getStateForByte(theByte);
        if (state != null) {
            return stateSet;
         }
        return NO_STATES;
    }

    public final int size() {
        return 1;
    }

    public Collection<Transition> getTransitions() {
        final List<Transition> result = new ArrayList<Transition>();
        result.add(transition);
        return result;
    }

}
