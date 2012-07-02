/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.parser.cellselector;

import java.io.Serializable;

/**
 * Represents a model for cell selection and creates cell selector instances using that model (see {@link CellSelector}
 * ). Implementations may constrain chart cell iteration or population by lexical analysis of the sentence or other
 * outside information.
 * 
 * Implementations of this model class should be thread-safe; i.e., after reading in or initializing the model, it must
 * be safe to call {@link #createCellSelector()} simultaneously from multiple threads. Note that the
 * {@link CellSelector} instances returned are not expected to be thread-safe. To parse multiple sentences
 * simultaneously, the user should obtain a {@link CellSelector} instance for each thread, using
 * {@link #createCellSelector()}.
 * 
 * @author Aaron Dunlop
 */
public interface CellSelectorModel extends Serializable {

    /**
     * @return a new {@link CellSelector} instance based on this model
     */
    public CellSelector createCellSelector();
}
