/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.videosegmenter.ffmpeg

import org.opencastproject.metadata.mpeg7.Mpeg7Catalog
import org.opencastproject.metadata.mpeg7.Segment

import java.util.LinkedList

/**
 * An optimization step is one step in the optimization of the number of segments.
 * This class stores parameters of such an optimization step and calculates error and
 * absolute error of optimization
 *
 */
class OptimizationStep : Comparable<OptimizationStep> {

    private var stabilityThreshold: Int = 0
    /**
     * get changesThreshold
     *
     * @return changesThreshold
     */
    var changesThreshold: Float = 0.toFloat()
        private set
    /**
     * get error of optimization
     *
     * @return error error of optimization
     */
    var error: Float = 0.toFloat()
        private set
    /**
     * get absolute error
     *
     * @return errorAbs absolute error
     */
    var errorAbs: Float = 0.toFloat()
        private set
    /**
     * get number of segments
     *
     * @return segmentNum number of segments
     */
    var segmentNum: Int = 0
        private set
    private var prefNum: Int = 0
    /**
     * get Mpeg7Catalog with segments
     *
     * @return mpeg7 Mpeg7Catalog with segments
     */
    var mpeg7: Mpeg7Catalog? = null
        private set
    /**
     * get list of segments
     *
     * @return segments  list of segments
     */
    var segments: LinkedList<Segment>? = null
        private set

    /**
     * creates a new optimization step with given parameters
     *
     * @param stabilityThreshold
     * @param changesThreshold
     * @param segNum
     * @param prefNum
     * @param mpeg7
     * @param segments unfiltered list of segments
     */
    constructor(stabilityThreshold: Int, changesThreshold: Float, segNum: Int, prefNum: Int, mpeg7: Mpeg7Catalog,
                segments: LinkedList<Segment>) {
        this.stabilityThreshold = stabilityThreshold
        this.changesThreshold = changesThreshold
        this.segmentNum = segNum
        this.prefNum = prefNum
        this.mpeg7 = mpeg7
        this.segments = segments
        calcErrors()
    }

    /**
     * creates a new optimization step with default values
     */
    constructor() {
        stabilityThreshold = 0
        changesThreshold = 0.0f
        segmentNum = 1
        prefNum = 1
        mpeg7 = null
        segments = null
        calcErrors()
    }

    /**
     * calculate error of optimization and absolute error of optimization
     */
    private fun calcErrors() {
        error = (segmentNum - prefNum).toFloat() / prefNum.toFloat()
        errorAbs = Math.abs(error)
    }

    /**
     * set number of segments
     *
     * @param segNum number of segments
     */
    fun setSegmentNumAndRecalcErrors(segNum: Int) {
        segmentNum = segNum
        calcErrors()
    }

    /**
     * With this method a list of OptimizationSteps can be sorted such that the smallest
     * positive error is the first element of the list and the smallest negative
     * error is the last element of the list
     *
     * @param o the OptimizationStep to be compared.
     * @return a negative integer or a positive integer as this OptimizationStep should be placed to the left or right of
     * the specified OptimizationStep or zero if their errors are equal.
     */
    override fun compareTo(o: OptimizationStep): Int {
        if (error == o.error) {
            return 0
        }
        // positive
        return if (error >= 0) {
            // if other error is negative put to the left of it
            if (o.error < 0) {
                -1
            } else {
                // if other error is also positive, compare errors, so that smaller positive error will be left
                if (error < o.error) {
                    -1
                } else {
                    1
                }
            }
            // negative
        } else {
            // if other error is positive put to the right of it
            if (o.error >= 0) {
                1
            } else {
                // if other error is also negative, compare errors, so that smaller negative error will be right
                if (error < o.error) {
                    -1
                } else {
                    1
                }
            }
        }
    }

    companion object {

        /**
         * calculates error from given number of segments and preferred number of
         * segments
         *
         * @param segmentNum number of segments
         * @param prefNum preferred number of segments
         * @return
         */
        fun calculateError(segmentNum: Int, prefNum: Int): Float {
            return (segmentNum - prefNum).toFloat() / prefNum.toFloat()
        }

        /**
         * calculates absolute error from given number of segments and preferred
         * number of segments
         *
         * @param segmentNum number of segments
         * @param prefNum preferred number of segments
         * @return
         */
        fun calculateErrorAbs(segmentNum: Int, prefNum: Int): Float {
            return Math.abs((segmentNum - prefNum).toFloat() / prefNum.toFloat())
        }
    }
}
