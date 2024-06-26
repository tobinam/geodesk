/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature.match;

import com.clarisma.common.math.MathUtils;

import java.nio.ByteBuffer;

public abstract class TagMatcher extends Matcher
{
    protected final String[] globalStrings;
    protected final int keyMask;
    protected final int keyMin;

    // TODO: take FeatureStore, resources
    protected TagMatcher(int types, String[] globalStrings, int keyMask, int keyMin)
    {
        super(types);
        this.globalStrings = globalStrings;
        this.keyMask = keyMask;
        this.keyMin = keyMin;
    }

    @Override public boolean acceptTyped(int types, ByteBuffer buf, int pos)
    {
        types &= 1 << (buf.get(pos) >> 1);
        if((types & acceptedTypes) == 0) return false;
        return accept(buf, pos);
    }

    @Override public boolean acceptIndex(int keys)
    {
        return (keys & keyMask) >= keyMin;
    }

    protected static String doubleToString(double d)
    {
        if(d == (long)d) return Long.toString((long)d);
        return Double.toString(d);
    }

    protected static double stringToDouble(String s)
    {
        return MathUtils.doubleFromString(s);
        /*
        try
        {
            return Double.parseDouble(s);
        }
        catch(NumberFormatException ex)
        {
            return Double.NaN;
        }
         */
    }

    protected String globalString(int code)
    {
        try
        {
            return globalStrings[code];
        }
        catch(ArrayIndexOutOfBoundsException ex)
        {
            // TODO: this is a sign of an invalid FeatureStore
            throw new QueryException(String.format(
                "Invalid global string code: %d", code));
        }
    }

    /*
    protected static void debug(String msg, int value)
    {
        FeatureStoreBase.log.debug(msg, value);
    }
     */
}
