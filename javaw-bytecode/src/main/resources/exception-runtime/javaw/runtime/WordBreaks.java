/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 *
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 2002 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 */

package javaw.runtime;

/** Adapted Java 17 boundary cursor for lowercase contexts; public java.text APIs are excluded. */
public final class WordBreaks {
    private final String text;
    private int at,cached=-1;
    public WordBreaks(String text){this.text=text;}
    private int current(){return at==text.length()?65535:text.codePointAt(at);}
    private int nextIndex(){return Math.min(text.length(),at+Character.charCount(current()));}
    private int next(){int next=nextIndex();if(next>=text.length())return 65535;at=next;return current();}
    private int previous(){if(at==0)return 65535;char last=text.charAt(--at);if(at>0 && Character.isLowSurrogate(last) && Character.isHighSurrogate(text.charAt(at-1))){char first=text.charAt(--at);return Character.toCodePoint(first,last);}return last;}
    private int scanForward(){
        if(at==text.length())return -1;
        int accepted=nextIndex(),look=0,state=1,cp=current();
        while(cp!=65535 && state!=0){
            int category=StringLowerData.category(cp);
            if(category!=-1)state=StringLowerData.state(state,category);
            if(StringLowerData.look(state)){
                if(StringLowerData.end(state))accepted=look;else look=nextIndex();
            }else if(StringLowerData.end(state))accepted=nextIndex();
            cp=next();
        }
        if(cp==65535 && look==text.length())accepted=look;
        at=accepted;return accepted;
    }
    private int scanBackward(){
        int state=1,category=0,last=0,cp=current();
        while(cp!=65535 && state!=0){
            last=category;category=StringLowerData.category(cp);
            if(category!=-1)state=StringLowerData.backward(state,category);
            cp=previous();
        }
        if(cp!=65535){if(last!=-1)next();next();}
        return at;
    }
    private int following(int offset){
        at=offset;
        if(offset==0){cached=scanForward();return cached;}
        int result=cached;
        if(result>=offset || result<=-1)result=scanBackward();else at=result;
        while(result!=-1 && result<=offset)result=scanForward();
        cached=result;return result;
    }
    public boolean isBoundary(int offset){
        if(offset<0 || offset>text.length())throw new IllegalArgumentException();
        return offset==0 || following(offset-1)==offset;
    }
    public static boolean[] boundaries(String text){
        WordBreaks cursor=new WordBreaks(text);boolean[] result=new boolean[text.length()+1];
        for(int i=0;i<result.length;i++)result[i]=cursor.isBoundary(i);return result;
    }
}
