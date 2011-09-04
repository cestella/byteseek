/*
 * Copyright Matt Palmer 2009-2011, All rights reserved.
 *
 */


package net.domesdaybook.compiler.automata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.domesdaybook.automata.AssociatedState;
import net.domesdaybook.automata.State;
import net.domesdaybook.automata.Transition;
import net.domesdaybook.automata.state.AssociatedStateFactory;
import net.domesdaybook.automata.state.SimpleAssociatedStateFactory;
import net.domesdaybook.automata.transition.TransitionFactory;
import net.domesdaybook.automata.transition.TransitionSingleByteMatcherFactory;
import net.domesdaybook.automata.wrapper.Trie;
import net.domesdaybook.compiler.CompileException;
import net.domesdaybook.compiler.ReversibleCompiler;
import net.domesdaybook.compiler.ReversibleCompiler.Direction;
import net.domesdaybook.matcher.sequence.ByteSequenceMatcher;
import net.domesdaybook.matcher.sequence.SequenceMatcher;
import net.domesdaybook.bytes.ByteUtilities;
import net.domesdaybook.matcher.singlebyte.SingleByteMatcher;


/**
 * Compiles a collection of sequence matchers into a Trie automata.
 * 
 * @author matt
 */
public final class TrieCompiler implements ReversibleCompiler<Trie, Collection<SequenceMatcher>> {

    private static TrieCompiler defaultCompiler;
    
    
    public static Trie trieFrom(final Collection<SequenceMatcher> sequences) throws CompileException {
        return trieFrom(sequences, Direction.FORWARDS);
    }
    
    public static Trie trieFrom(final List<byte[]> bytes) throws CompileException {
        return trieFrom(bytes, Direction.FORWARDS);
    }
    
    
    public static Trie trieFrom(final Collection<SequenceMatcher> sequences, final Direction direction) throws CompileException {
        defaultCompiler = new TrieCompiler();
        return defaultCompiler.compile(sequences, direction);
    }
    
    public static Trie trieFrom(final List<byte[]> bytes, final Direction direction) throws CompileException {
        defaultCompiler = new TrieCompiler();
        final List<SequenceMatcher> matchers = new ArrayList<SequenceMatcher>(bytes.size());
        for (final byte[] bytesToUse : bytes) {
            matchers.add(new ByteSequenceMatcher(bytesToUse));
        }
        return defaultCompiler.compile(matchers, direction);
    }

    
    
    
    private final AssociatedStateFactory<SequenceMatcher> stateFactory;
    private final TransitionFactory transitionFactory;
    

    public TrieCompiler() {
        this(null, null);
    }

    
    public TrieCompiler(AssociatedStateFactory<SequenceMatcher> stateFactory) {
        this(stateFactory, null);
    }

   
    public TrieCompiler(TransitionFactory transitionFactory) {
        this(null, transitionFactory);
    }
   

    public TrieCompiler(AssociatedStateFactory<SequenceMatcher> stateFactory, TransitionFactory transitionFactory) {
        if (stateFactory == null) {
            this.stateFactory = new SimpleAssociatedStateFactory<SequenceMatcher>();
        } else {
            this.stateFactory = stateFactory;
        }
        if (transitionFactory == null) {
            this.transitionFactory = new TransitionSingleByteMatcherFactory();
        } else {
            this.transitionFactory = transitionFactory;
        }
    }

    @Override
    public Trie compile(Collection<SequenceMatcher> expression) throws CompileException {
        return compile(expression, Direction.FORWARDS);
    }    
    
    
    @Override
    public final Trie compile(Collection<SequenceMatcher> sequences, final Direction direction) throws CompileException {
        AssociatedState<SequenceMatcher> initialState = stateFactory.create(State.NON_FINAL);
        int minLength = Integer.MAX_VALUE;
        int maxLength = 0;
        for (final SequenceMatcher sequence : sequences) {
            if (direction == Direction.FORWARDS) {
                addSequence(sequence, initialState);
            } else {
                addReversedSequence(sequence, initialState);
            }
            final int len = sequence.length();
            if (len < minLength) {
                minLength = len;
            }
            if (len > maxLength) {
                maxLength = len;
            }
        }
        return new Trie(initialState, minLength, maxLength);
    }

    
    private void addSequence(SequenceMatcher sequence, AssociatedState<SequenceMatcher> initialState) {
        List<AssociatedState<SequenceMatcher>> currentStates = new ArrayList<AssociatedState<SequenceMatcher>>();
        currentStates.add(initialState);
        final int lastPosition = sequence.length() - 1;
        for (int position = 0; position <= lastPosition; position++) {
            final SingleByteMatcher byteMatcher = sequence.getByteMatcherForPosition(position);
            currentStates = nextStates(currentStates, byteMatcher, position == lastPosition);
        }
        for (AssociatedState<SequenceMatcher> finalState : currentStates) {
            finalState.addObject(sequence);
        }
    }
    
    
    private void addReversedSequence(SequenceMatcher sequence, AssociatedState<SequenceMatcher> initialState) {
        List<AssociatedState<SequenceMatcher>> currentStates = new ArrayList<AssociatedState<SequenceMatcher>>();
        currentStates.add(initialState);
        final int lastPosition = sequence.length() - 1;
        for (int position = lastPosition; position >= 0; position--) {
            final SingleByteMatcher byteMatcher = sequence.getByteMatcherForPosition(position);
            currentStates = nextStates(currentStates, byteMatcher, position == 0);
        }
        for (AssociatedState<SequenceMatcher> finalState : currentStates) {
            finalState.addObject(sequence);
        }
    }      

    
    protected final List<AssociatedState<SequenceMatcher>> nextStates(List<AssociatedState<SequenceMatcher>> currentStates, SingleByteMatcher bytes, boolean isFinal) {
        final List<AssociatedState<SequenceMatcher>> nextStates = new ArrayList<AssociatedState<SequenceMatcher>>();
        final Set<Byte> allBytesToTransitionOn = ByteUtilities.toSet(bytes.getMatchingBytes());
        for (final State currentState : currentStates) {
            // make a defensive copy of the transitions of the current state:
            final List<Transition> transitions = new ArrayList<Transition>(currentState.getTransitions());
            final Set<Byte> bytesToTransitionOn = new HashSet<Byte>(allBytesToTransitionOn);
            for (final Transition transition : transitions) {
                
                final Set<Byte> originalTransitionBytes = ByteUtilities.toSet(transition.getBytes());
                final int originalTransitionBytesSize = originalTransitionBytes.size();
                final Set<Byte> bytesInCommon = subtract(originalTransitionBytes, bytesToTransitionOn);
                
                // If the existing transition is the same or a subset of the new transition bytes:
                final int numberOfBytesInCommon = bytesInCommon.size();
                if (numberOfBytesInCommon == originalTransitionBytesSize) {
                    
                    final State toState = transition.getToState();
                    
                    // Ensure that the state is final if necessary:
                    if (isFinal) {
                        toState.setIsFinal(true);
                    }
                    
                    // Add this state to the states we have to process next.
                    nextStates.add((AssociatedState<SequenceMatcher>) toState);
                    
                } else if (numberOfBytesInCommon > 0) {
                    // Only some bytes are in common.  
                    // We will have to split the existing transition to
                    // two states, and recreate the transitions to them:
                    final State originalToState = transition.getToState();
                    if (isFinal) {
                        originalToState.setIsFinal(true);
                    }
                    final State newToState = originalToState.deepCopy();                    
                    
                    // Add a transition to the bytes which are not in common:
                    final Transition bytesNotInCommonTransition = transitionFactory.createSetTransition(originalTransitionBytes, false, originalToState);
                    currentState.addTransition(bytesNotInCommonTransition);
                    
                    // Add a transition to the bytes in common:
                    final Transition bytesInCommonTransition = transitionFactory.createSetTransition(bytesInCommon, false, newToState);
                    currentState.addTransition(bytesInCommonTransition);
                   
                    // Add the bytes in common state to the next states to process:
                    nextStates.add((AssociatedState<SequenceMatcher>) newToState);
                    
                    // Clean up and optimise the current state:
                    currentState.removeTransition(transition);
                    currentState.setTransitionStrategy(State.FIRST_MATCHING_TRANSITION);
                }
                
                // If we have no further bytes to process, just break out.
                final int numberOfBytesLeft = bytesToTransitionOn.size();
                if (numberOfBytesLeft == 0) {
                    break;
                }                
            }
            
            // If there are any bytes left over, create a transition to a new state:
            final int numberOfBytesLeft = bytesToTransitionOn.size();
            if (numberOfBytesLeft > 0) {
                final AssociatedState<SequenceMatcher> newState = stateFactory.create(isFinal);
                final Transition newTransition = transitionFactory.createSetTransition(bytesToTransitionOn, false, newState);
                currentState.addTransition(newTransition);
                nextStates.add(newState);
            }
        }
        
        return nextStates;
    }

    
    
    private Set<Byte> subtract(final Set<Byte> bytes, final Set<Byte> fromSet) {
        final Set<Byte> bytesInCommon = new LinkedHashSet<Byte>();
        final Iterator<Byte> byteIterator = bytes.iterator();
        while (byteIterator.hasNext()) {
            final Byte theByte = byteIterator.next();
            if (fromSet.remove(theByte)) {
                bytesInCommon.add(theByte);
                byteIterator.remove();
            }
        }
        return bytesInCommon;
    }
    

}
