/*
 * Copyright Matt Palmer 2012, All rights reserved.
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
package net.domesdaybook.parser.tree;


/**
 * @author Matt Palmer
 *
 */
public enum ParseTreeType {
	
	// Value carrying tree leaf node types (have direct value, no children)
		
    BYTE("A single byte value"),

	ALL_BITMASK("A bitmask for matching all of its bits"),

	ANY_BITMASK("A bitmask for matching any of its bits"),

	SET("A set of byte values"),
	
	ANY("A wildcard matching any byte value"),

	CASE_SENSITIVE_STRING("An ASCII string to match case sensitively"),

	CASE_INSENSITIVE_STRING("An ASCII string to match case insensitively"),

	INTEGER("An integer"),
	
    // Child carrying tree node types (no direct value, has child ParseTrees)

	SEQUENCE("An ordered sequence of child ParseTrees"),

	RANGE("A range of byte values with the range defined as two child INTEGER ParseTree nodes, from 0 to 255"),

	REPEAT("Repeat the third child ParseTree from a minimum (first INTEGER child) to a maximum (second INTEGER or MANY child) number of times."),

	ALTERNATIVES("A set of alternatives as children of this ParseTree"),
	
	ZERO_TO_MANY("Repeat the child ParseTree zero to many times"),

	ONE_TO_MANY("Repeat the child ParseTree one to many times"),
	
  	OPTIONAL("The child ParseTree is optional (repeat zero to one times).");
  
  private final String description;
	
	private ParseTreeType(final String description) {
   	this.description = description;
	}
	
	public String getDescription() {
		return description;
	}

}
