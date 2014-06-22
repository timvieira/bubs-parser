/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.hash;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class BasicShortPair2IntHash implements ImmutableShortPair2IntHash {

    // TODO Vary the shift

    private final Int2IntOpenHashMap hash;

    public BasicShortPair2IntHash(final short[][] keyPairs) {
        hash = new Int2IntOpenHashMap();
        for (int i = 0; i < keyPairs[0].length; i++) {
            final int packedKey = keyPairs[0][i] << 16 | keyPairs[1][i];
            hash.put(packedKey, packedKey);
        }
        hash.defaultReturnValue(Integer.MIN_VALUE);
    }

    @Override
    public int hashcode(final short key1, final short key2) {
        return unsafeHashcode(key1, key2);
    }

    @Override
    public int unsafeHashcode(final short key1, final short key2) {
        return hash.get(key1 << 16 | key2);
    }

    @Override
    public short key1(final int hashcode) {
        return unsafeKey1(hashcode);
    }

    @Override
    public short unsafeKey1(final int hashcode) {
        final int value = hash.get(hashcode);
        return value < 0 ? Short.MIN_VALUE : (short) (value >> 16);
    }

    @Override
    public short key2(final int hashcode) {
        return unsafeKey2(hashcode);
    }

    @Override
    public short unsafeKey2(final int hashcode) {
        final int value = hash.get(hashcode);
        return value < 0 ? Short.MIN_VALUE : (short) (value & 0xffff);
    }

}
