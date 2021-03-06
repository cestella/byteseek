/*
 * Copyright Matt Palmer 2009-2012, All rights reserved.
 *
 * This code is licensed under a standard 3-clause BSD license:
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 * 
 *  * The names of its contributors may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.byteseek.matcher.bytes;

/**
 * An abstract base class for SingleByteMatchers which can invert
 * the results of matching.
 * <p>
 * For example, if a ByteMatcher had a rule to match all the odd bytes, but
 * it was inverted, it would match all the even bytes.
 * <p>
 * When extending this base class, careful attention must be paid to
 * the other interface methods, in particular getNumberOfMatchingBytes() and
 * getMatchingBytes(), as it is easy to forget to invert the number and set
 * of bytes returned by those methods, if the instance happens to be inverted.
 *
 * @author Matt Palmer
 */
public abstract class InvertibleMatcher extends AbstractByteMatcher {

    /**
     * A public constant indicating that the results of a match should be inverted.
     */
    public static final boolean INVERTED = true;
    
    
    /**
     * A public constant indicating that the results of a match should not be inverted.
     */
    public static final boolean NOT_INVERTED = false;
    
    
    /**
     * Whether the results of a match should be inverted.
     */
    protected final boolean inverted;

    
    /**
     * Constructs an InvertibleMatcher.
     * 
     * @param inverted Whether the matcher results are inverted or not.
     */
    public InvertibleMatcher(final boolean inverted) {
        this.inverted = inverted;
    }


    /**
     * Returns true if the matcher inverts its results.
     * 
     * @return Whether the matcher results are inverted or not.
     */
    public final boolean isInverted() {
        return inverted;
    }

}
