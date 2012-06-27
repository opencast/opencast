/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.caption.impl;

import org.opencastproject.caption.api.Caption;
import org.opencastproject.caption.api.CaptionCollection;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Implementation of {@link CaptionCollection}. Uses {@link LinkedList} to store captions is sequential order. Supports
 * adding but not inserting or deleting. Iterator that is returned with getCollectionIterator() supports only traversal
 * of collection but not its modification.
 * 
 */
public class CaptionCollectionImpl implements CaptionCollection {

  /** LinkedList that stores captions */
  private LinkedList<Caption> captionCollection;

  /**
   * Created empty caption collection.
   */
  public CaptionCollectionImpl() {
    this.captionCollection = new LinkedList<Caption>();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionCollection#addCaption(org.opencastproject.caption.api.Caption)
   */
  public void addCaption(Caption caption) {
    this.captionCollection.add(caption);
  }

  /**
   * {@inheritDoc} Does not support modifying collection. If remove() is called {@link UnsupportedOperationException} is
   * thrown.
   * 
   * @see org.opencastproject.caption.api.CaptionCollection#getCollectionIterator()
   */
  @Override
  public Iterator<Caption> getCollectionIterator() {
    return new Iterator<Caption>() {

      private Iterator<Caption> iterator = captionCollection.iterator();

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Caption next() {
        return iterator.next();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Removing captions is not supported.");
      }
    };
  }

}
