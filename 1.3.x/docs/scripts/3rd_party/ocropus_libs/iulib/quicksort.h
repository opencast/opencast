// -*- C++ -*-

// Copyright 2006 Deutsches Forschungszentrum fuer Kuenstliche Intelligenz
// or its licensors, as applicable.
// Copyright 1995-2005 Thomas M. Breuel
//
// You may not use this file except under the terms of the accompanying license.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you
// may not use this file except in compliance with the License. You may
// obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// Project: iulib -- image understanding library
// File: quicksort.h
// Purpose: sorting of narrays
// Responsible: tmb
// Reviewer:
// Primary Repository:
// Web Sites: www.iupr.org, www.dfki.de

/// \file quicksort.h
/// \brief Sorting of narrays

#ifndef h_quicksort__
#define h_quicksort__

#include "colib/checks.h"
#include "colib/narray.h"
#ifdef WIN32
#include "compat.h"
#endif

namespace colib {

    namespace {
        template <class T>
        void qswap_(T &a,T &b) {
            T temp = a;
            a = b;
            b = temp;
        }
    }

    template <class T>
    static void quicksort(narray<int> &index,narray<T> &values,int start,int end) {
        if(start>=end-1) return;

        // pick a pivot
        // NB: it's OK for this to be a reference pointing into values
        // since we aren't actually moving the elements of values[] around

        T &pivot = values[index[(start+end-1)/2]];

        // first, split into two parts: less than the pivot
        // and greater-or-equal

        int lo = start;
        int hi = end;
        for(;;) {
            while(lo<hi && values[index[lo]]<pivot) lo++;
            while(lo<hi && values[index[hi-1]]>=pivot) hi--;
            if(lo==hi || lo==hi-1) break;
            qswap_(index[lo],index[hi-1]);
            lo++;
            hi--;
        }
        int split1 = lo;

        // now split into two parts: equal to the pivot
        // and strictly greater.

        hi = end;
        for(;;) {
            while(lo<hi && values[index[lo]]==pivot) lo++;
            while(lo<hi && values[index[hi-1]]>pivot) hi--;
            if(lo==hi || lo==hi-1) break;
            qswap_(index[lo],index[hi-1]);
            lo++;
            hi--;
        }
        int split2 = lo;

#ifdef TEST
        for(int i=start;i<split1;i++) ASSERT(values[index[i]]<pivot);
        for(int i=split1;i<split2;i++) ASSERT(values[index[i]]==pivot);
        for(int i=split2;i<end;i++) ASSERT(values[index[i]]>pivot);
#endif

        quicksort(index,values,start,split1);
        quicksort(index,values,split2,end);
    }

    /// Quicksort an array, generating a permutation of the indexes.

    template <class T>
    inline void quicksort(narray<int> &index,narray<T> &values) {
        index.resize(values.length());
        for(int i=0;i<values.length();i++) index[i] = i;
        quicksort(index,values,0,index.length());
    }

    template <class T>
    inline void argsort(narray<int> &index,narray<T> &values) {
        index.resize(values.length());
        for(int i=0;i<values.length();i++) index[i] = i;
        quicksort(index,values,0,index.length());
    }

    template <class T>
    inline void quickrank(narray<int> &rank,narray<T> &values) {
        intarray index;
        quicksort(index,values);
        rank.resize(index.length());
        for(int i=0;i<rank.length();i++)
            rank(index(i)) = i;
    }

    template <class T>
    inline void shuffle(narray<T> &values) {
        floatarray temp(values.length());
        intarray index;
        for(int i=0;i<temp.length();i++)
            temp(i) = drand48();
        quicksort(index,temp);
        permute(values,index);
    }

    inline void rpermutation(intarray &index,int n) {
        index.resize(n);
        for(int i=0;i<n;i++) index(i) = i;
        shuffle(index);
    }

    /// Permute the elements of an array given a permutation.

    template <class T>
    static void permute(narray<T> &data,narray<int> &permutation) {
        CHECK_ARG(samedims(data,permutation));
        narray<bool> finished(data.length());
        fill(finished,false);
        for(int start=0;start<finished.length();start++) {
            if(finished(start)) continue;
            int index = start;
            T value = data(index);
            for(;;) {
                int next = permutation(index);
                if(next==start) break;
                data(index) = data(next);
                index = next;
                CHECK_ARG(!finished(index) && "not a permutation");
                finished(index) = true;
                index = next;
            }
            data(index) = value;
            finished(index) = true;
        }
    }

    /// Select elements with the given indexes.

    template <class T>
    static void pick(narray<T> &data,narray<T> &source,narray<int> &indexes) {
        data.clear();
        for(int i=0;i<indexes.length();i++)
            data.push(source(indexes(i)));
    }

    /// Permute the elements of an array given a permutation,
    /// Permute the elements of an array given a permutation,
    /// using the overloaded move function to move values around.

    template <class T>
    static void permute_move(narray<T> &data,narray<int> &permutation) {
        CHECK_ARG(samedims(data,permutation));
        narray<bool> finished(data.length());
        fill(finished,false);
        for(int start=0;start<finished.length();start++) {
            if(finished(start)) continue;
            int index = start;
            T value;
            move(value,data(index));
            for(;;) {
                int next = permutation(index);
                if(next==start) break;
                move(data(index),data(next));
                index = next;
                CHECK_ARG(!finished(index) && "not a permutation");
                finished(index) = true;
                index = next;
            }
            move(data(index),value);
            finished(index) = true;
        }
    }

    /// Quicksort the elements of an array in place.
    /// (Uses an intermediate permutation, so elements are moved around minimally.))

    template <class T>
    static void quicksort_perm(narray<T> &data) {
        narray<int> permutation;
        quicksort(permutation,data);
        permute(data,permutation);
    }

    template <class T>
    static void quicksort(narray<T> &values,int start,int end) {
        if(start>=end-1) return;

        // pick a pivot
        // NB: this cannot be a reference to the value (since we're moving values around)

        T pivot = values[(start+end-1)/2];

        // first, split into two parts: less than the pivot
        // and greater-or-equal

        int lo = start;
        int hi = end;
        for(;;) {
            while(lo<hi && values[lo]<pivot) lo++;
            while(lo<hi && values[hi-1]>=pivot) hi--;
            if(lo==hi || lo==hi-1) break;
            qswap_(values[lo],values[hi-1]);
            lo++;
            hi--;
        }
        int split1 = lo;

        // now split into two parts: equal to the pivot
        // and strictly greater.

        hi = end;
        for(;;) {
            while(lo<hi && values[lo]==pivot) lo++;
            while(lo<hi && values[hi-1]>pivot) hi--;
            if(lo==hi || lo==hi-1) break;
            qswap_(values[lo],values[hi-1]);
            lo++;
            hi--;
        }
        int split2 = lo;

#ifdef TEST
        for(int i=start;i<split1;i++) ASSERT(values[i]<pivot);
        for(int i=split1;i<split2;i++) ASSERT(values[i]==pivot);
        for(int i=split2;i<end;i++) ASSERT(values[i]>pivot);
#endif

        quicksort(values,start,split1);
        quicksort(values,split2,end);
    }

    /// Quicksort the elements of an array in place.

    template <class T>
    static void quicksort(narray<T> &values) {
        quicksort(values,0,values.length());
    }

    /// Find unique elements.

    template <class T>
    static void uniq(narray<T> &values) {
        if(values.length()==0) return;
        quicksort(values);
        int j = 1;
        for(int i=1;i<values.length();i++) {
            if(values(i)==values(j-1)) continue;
            values(j++) = values(i);
        }
        values.truncate(j);
    }


    /// Compute fractiles.

    template <class T>
    static T fractile(narray<T> &a,double f) {
        floatarray temp;
        CHECK(f>=0 && f<=1);
        copy(temp,a);
        temp.reshape(temp.length1d());
        quicksort(temp);
        return temp[int(f*temp.length())];
    }

    template <class T>
    static T median(narray<T> &a) {
        return fractile(a,0.5);
    }
}

#endif
