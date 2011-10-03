// Copyright 2008 Deutsches Forschungszentrum fuer Kuenstliche Intelligenz
// or its licensors, as applicable.
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
// Project: ocrofst
// File: fst-io.cc
// Purpose: OpenFST-compatible I/O
// Responsible: mezhirov
// Reviewer:
// Primary Repository:
// Web Sites: www.iupr.org, www.dfki.de, www.ocropus.org
//

#include <stdio.h>
#include <stdint.h>
#include "ocr-pfst.h"

using namespace colib;
using namespace ocropus;

enum {
    // They say it also encodes endianness. But I haven't seen any BE variant.
    OPENFST_MAGIC = 2125659606,
    OPENFST_SYMBOL_TABLE_MAGIC = 2125658996,

    FLAG_HAS_ISYMBOLS = 1,
    FLAG_HAS_OSYMBOLS = 2,

    MIN_VERSION = 2,
    PROPERTIES = 3 // expanded, mutable
};

static int32_t read_int32_LE(FILE *stream) {
    int n = fgetc(stream);
    n |= fgetc(stream) << 8;
    n |= fgetc(stream) << 16;
    n |= fgetc(stream) << 24;
    return n;
}

static void write_int32_LE(FILE *stream, int32_t n) {
    fputc(n, stream);
    fputc(n >> 8, stream);
    fputc(n >> 16, stream);
    fputc(n >> 24, stream);
}

static int64_t read_int64_LE(FILE *stream) {
    int64_t n = read_int32_LE(stream);
    n |= (int64_t)(read_int32_LE(stream)) << 32;
    return n;
}

static void write_int64_LE(FILE *stream, int64_t n) {
    write_int32_LE(stream, n);
    write_int32_LE(stream, n >> 32);
}

static bool read_magic_string(FILE *stream, const char *s) {
    int n = read_int32_LE(stream);
    if(strlen(s) != n)
        return false;
    for(int i = 0; i < n; i++) {
        if(fgetc(stream) != s[i])
            return false;
    }
    return true;
}

static void skip_string(FILE *stream) {
    int n = read_int32_LE(stream);
    fseek(stream, n, SEEK_CUR);
}

static void write_string(FILE *stream, const char *s) {
    int n = strlen(s);
    write_int32_LE(stream, n);
    for(int i = 0; i < n; i++)
        fputc(s[i], stream);
}

// This is probably not a good way but that's what OpenFST does anyway.
static bool write_float(FILE *stream, float f) {
    return fwrite(&f, 1, sizeof(f), stream) == sizeof(f);
}

static float read_float(FILE *stream) {
    float result;
    if(fread(&result, 1, sizeof(result), stream) != sizeof(result)) {
        // cry
    }
    return result;
}

// _______________________   high-level functions   ___________________________

static bool skip_symbol_table(FILE *stream) {
    if(read_int32_LE(stream) != OPENFST_SYMBOL_TABLE_MAGIC)
        return false;
    skip_string(stream); // name
    read_int64_LE(stream); // available key
    int64_t n = read_int64_LE(stream);
    for(int i = 0; i < n; i++) {
        skip_string(stream);    // key
        read_int64_LE(stream);  // value
    }
    return !ferror(stream) && !feof(stream);
}

static const char *read_header_and_symbols(IGenericFst &fst, FILE *stream) {
    if(read_int32_LE(stream) != OPENFST_MAGIC)
        return "invalid magic number";
    read_magic_string(stream, "vector");
    read_magic_string(stream, "standard");
    int version = read_int32_LE(stream);
    if(version < MIN_VERSION)
        return "file has too old version";
    int flags = read_int32_LE(stream);
    read_int64_LE(stream); // properties
    int64_t start = read_int64_LE(stream);
    int64_t nstates = read_int64_LE(stream);
    if(nstates < 0)
        return false;   // to prevent creating 2^31 nodes in case of sudden EOF
    fst.clear();
    for(int i = 0; i < nstates; i++)
        fst.newState();
    fst.setStart(start);

    read_int64_LE(stream); // narcs

    if(flags & FLAG_HAS_ISYMBOLS)
        skip_symbol_table(stream);
    if(flags & FLAG_HAS_OSYMBOLS)
        skip_symbol_table(stream);

    if(ferror(stream))
        return "error in the stream";
    if(feof(stream))
        return "unexpected EOF";
    return NULL;
}

/*static int64_t narcs(IGenericFst &fst) {
    intarray inputs;
    intarray targets;
    intarray outputs;
    floatarray costs;
    int64_t result = 0;
    for(int i = 0; i < fst.nStates(); i++) {
        fst.arcs(inputs, targets, outputs, costs, i);
        result += targets.length();
    }
    return result;
}*/

static void write_header_and_symbols(FILE *stream, IGenericFst &fst) {
    write_int32_LE(stream, OPENFST_MAGIC);
    write_string(stream, "vector");
    write_string(stream, "standard");
    write_int32_LE(stream, MIN_VERSION);
    write_int32_LE(stream, /* flags: */ 0);
    write_int64_LE(stream, PROPERTIES);
    write_int64_LE(stream, fst.getStart());
    write_int64_LE(stream, fst.nStates());
    write_int64_LE(stream, /* narcs (seems to be unused): */ 0);
}

static void write_node(FILE *stream, IGenericFst &fst, int index) {
    intarray inputs;
    intarray targets;
    intarray outputs;
    floatarray costs;
    fst.arcs(inputs, targets, outputs, costs, index);
    int narcs = targets.length();

    write_float(stream, fst.getAcceptCost(index));
    write_int64_LE(stream, narcs);
    for(int i = 0; i < narcs; i++) {
        write_int32_LE(stream, inputs[i]);
        write_int32_LE(stream, outputs[i]);
        write_float(stream, costs[i]);
        write_int32_LE(stream, targets[i]);
    }
}

static void read_node(FILE *stream, IGenericFst &fst, int index) {
    fst.setAccept(index, read_float(stream));
    int narcs = read_int64_LE(stream);
    for(int i = 0; i < narcs; i++) {
        int input = read_int32_LE(stream);
        int output = read_int32_LE(stream);
        float cost = read_float(stream);
        int target = read_int32_LE(stream);
        fst.addTransition(index, target, output, cost, input);
    }
}

namespace ocropus {

    void fst_write(FILE *stream, IGenericFst &fst) {
        write_header_and_symbols(stream, fst);
        for(int i = 0; i < fst.nStates(); i++)
            write_node(stream, fst, i);
    }

    void fst_read(IGenericFst &fst, FILE *stream) {
        const char *errmsg = read_header_and_symbols(fst, stream);
        if(errmsg)
            throw errmsg;
        for(int i = 0; i < fst.nStates(); i++)
            read_node(stream, fst, i);
    }

    void fst_write(const char *path, IGenericFst &fst) {
        fst_write(stdio(path, "wb"), fst);
    }

    void fst_read(IGenericFst &fst, const char *path) {
        fst_read(fst, stdio(path, "rb"));
    }
}
