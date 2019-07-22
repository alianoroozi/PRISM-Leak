//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

#include "PrismSparse.h"
#include "NDSparseMatrix.h"
#include <cstdio>
#include <cstdarg>
#include <climits>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include "sparse.h"
#include "PrismSparseGlob.h"
#include "jnipointer.h"

#define MAX_LOG_STRING_LEN 1024
#define MAX_ERR_STRING_LEN 1024

//------------------------------------------------------------------------------
// sparse engine global variables
//------------------------------------------------------------------------------

// cudd manager
DdManager *ddman;

// logs
// global refs to log classes
static jclass main_log_cls = NULL;
static jclass tech_log_cls = NULL;
// global refs to log objects
static jobject main_log_obj = NULL;
static jobject tech_log_obj = NULL;
// method ids for print method in logs
static jmethodID main_log_mid = NULL;
static jmethodID main_log_warn = NULL;
static jmethodID tech_log_mid = NULL;

// export stuff
int export_type;
FILE *export_file;
JNIEnv *export_env;
static bool exportIterations = false;

// error message
static char error_message[MAX_ERR_STRING_LEN];

//------------------------------------------------------------------------------
// cudd manager
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetCUDDManager(JNIEnv *env, jclass cls, jlong __jlongpointer ddm)
{
	ddman = jlong_to_DdManager(ddm);
}

//------------------------------------------------------------------------------
// logs
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetMainLog(JNIEnv *env, jclass cls, jobject log)
{
	// if main log has been set previously, we need to delete existing global refs first
	if (main_log_obj != NULL) {
		env->DeleteGlobalRef(main_log_cls);
		main_log_cls = NULL;
		env->DeleteGlobalRef(main_log_obj);
		main_log_obj = NULL;
	}
	
	// make a global reference to the log object
	main_log_obj = env->NewGlobalRef(log);
	// get the log class and make a global reference to it
	main_log_cls = (jclass)env->NewGlobalRef(env->GetObjectClass(main_log_obj));
	// get the method id for the print method
	main_log_mid = env->GetMethodID(main_log_cls, "print", "(Ljava/lang/String;)V");
    main_log_warn = env->GetMethodID(main_log_cls, "printWarning", "(Ljava/lang/String;)V");
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetTechLog(JNIEnv *env, jclass cls,  jobject log)
{
	// if tech log has been set previously, we need to delete existing global refs first
	if (tech_log_obj != NULL) {
		env->DeleteGlobalRef(tech_log_cls);
		tech_log_cls = NULL;
		env->DeleteGlobalRef(tech_log_obj);
		tech_log_obj = NULL;
	}
	
	// make a global reference to the log object
	tech_log_obj = env->NewGlobalRef(log);
	// get the log class and make a global reference to it
	tech_log_cls = (jclass)env->NewGlobalRef(env->GetObjectClass(tech_log_obj));
	// get the method id for the print method
	tech_log_mid = env->GetMethodID(tech_log_cls, "print", "(Ljava/lang/String;)V");
}

//------------------------------------------------------------------------------

void PS_PrintToMainLog(JNIEnv *env, const char *str, ...)
{
	va_list argptr;
	char full_string[MAX_LOG_STRING_LEN];
	
	va_start(argptr, str);
	vsnprintf(full_string, MAX_LOG_STRING_LEN, str, argptr);
	va_end(argptr);
	
	if (env)
		env->CallVoidMethod(main_log_obj, main_log_mid, env->NewStringUTF(full_string));
	else
		printf("%s", full_string);
}

//------------------------------------------------------------------------------

void PS_PrintWarningToMainLog(JNIEnv *env, const char *str, ...)
{
	va_list argptr;
	char full_string[MAX_LOG_STRING_LEN];
	
	va_start(argptr, str);
	vsnprintf(full_string, MAX_LOG_STRING_LEN, str, argptr);
	va_end(argptr);
	
	if (env)
		env->CallVoidMethod(main_log_obj, main_log_warn, env->NewStringUTF(full_string));
	else
		printf("\nWarning: %s\n", full_string);
}

//------------------------------------------------------------------------------

void PS_PrintToTechLog(JNIEnv *env, const char *str, ...)
{
	va_list argptr;
	char full_string[MAX_LOG_STRING_LEN];
	
	va_start(argptr, str);
	vsnprintf(full_string, MAX_LOG_STRING_LEN, str, argptr);
	va_end(argptr);
	
	if (env)
		env->CallVoidMethod(tech_log_obj, tech_log_mid, env->NewStringUTF(full_string));
	else
		printf("%s", full_string);
}

//------------------------------------------------------------------------------

// Print formatted memory info to main log

void PS_PrintMemoryToMainLog(JNIEnv *env, const char *before, double mem, const char *after)
{
	char full_string[MAX_LOG_STRING_LEN];
	
	if (mem > 1048576)
		snprintf(full_string, MAX_LOG_STRING_LEN, "%s%.1f GB%s", before, mem/1048576.0, after);
	else if (mem > 1024)
		snprintf(full_string, MAX_LOG_STRING_LEN, "%s%.1f MB%s", before, mem/1024.0, after);
	else
		snprintf(full_string, MAX_LOG_STRING_LEN, "%s%.1f KB%s", before, mem, after);
	
	if (env) {
		env->CallVoidMethod(main_log_obj, main_log_mid, env->NewStringUTF(full_string));
	}
	else {
		printf("%s", full_string);
	}
}

//------------------------------------------------------------------------------
// export stuff
//------------------------------------------------------------------------------

// store export info globally
// returns 0 on failure, 1 otherwise

int store_export_info(int type, jstring fn, JNIEnv *env)
{
	export_type = type;
	if (fn) {
		const char *filename = env->GetStringUTFChars(fn, 0);
		export_file = fopen(filename, "w");
		if (!export_file) {
			env->ReleaseStringUTFChars(fn, filename);
			return 0;
		}
		env->ReleaseStringUTFChars(fn, filename);
	} else {
		export_file = NULL;
	}
	export_env = env;
	return 1;
}

//------------------------------------------------------------------------------

// export string (either to file or main log)

void export_string(const char *str, ...)
{
	va_list argptr;
	char full_string[MAX_LOG_STRING_LEN];
	
	va_start(argptr, str);
	vsnprintf(full_string, MAX_LOG_STRING_LEN, str, argptr);
	va_end(argptr);
	
	if (export_file) {
		fprintf(export_file, "%s", full_string);
	} else {
		PS_PrintToMainLog(export_env, "%s", full_string);
	}
}

//------------------------------------------------------------------------------
// use steady-state detection?
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetDoSSDetect(JNIEnv *env, jclass cls, jboolean b)
{
	do_ss_detect = b;
}

//------------------------------------------------------------------------------
// error message handling
//------------------------------------------------------------------------------

void PS_SetErrorMessage(const char *str, ...)
{
	va_list argptr;
	
	va_start(argptr, str);
	vsnprintf(error_message, MAX_ERR_STRING_LEN, str, argptr);
	va_end(argptr);
}

char *PS_GetErrorMessage()
{
	return error_message;
}

JNIEXPORT jstring JNICALL Java_sparse_PrismSparse_PS_1GetErrorMessage(JNIEnv *env, jclass cls)
{
	return env->NewStringUTF(error_message);
}


JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetExportIterations(JNIEnv *env, jclass cls, jboolean value)
{
	exportIterations = value;
}

bool PS_GetFlagExportIterations()
{
	return exportIterations;
}

//------------------------------------------------------------------------------
// tidy up
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1FreeGlobalRefs(JNIEnv *env, jclass cls)
{
	// delete all global references
	env->DeleteGlobalRef(main_log_cls);
	main_log_cls = NULL;
	env->DeleteGlobalRef(tech_log_cls);
	tech_log_cls = NULL;
	env->DeleteGlobalRef(main_log_obj);
	main_log_obj = NULL;
	env->DeleteGlobalRef(tech_log_obj);
	tech_log_obj = NULL;
}

//------------------------------------------------------------------------------
// Sparse matrix
//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_sparse_NDSparseMatrix_PS_1NDGetActionIndex
(JNIEnv *env, jclass cls,
 jlong __jlongpointer _ndsm, // NDSparseMatrix ptr
 jint __jlongpointer s, // state index
 jint __jlongpointer i // choice index
 )
{
    NDSparseMatrix * ndsm = (NDSparseMatrix *) jlong_to_NDSparseMatrix(_ndsm);
	bool use_counts = ndsm->use_counts;
	int *row_starts = (int *)ndsm->row_counts;
	unsigned char *row_counts = ndsm->row_counts;
	
	int l1 = 0;
	if (!use_counts) { l1 = row_starts[s]; }
	else { for (int i = 0; i < s; i++) l1 += row_counts[i]; }
	return ndsm->actions[l1];
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_NDSparseMatrix_PS_1BuildNDSparseMatrix
(JNIEnv *env, jclass cls,
 jlong __jlongpointer t,    // trans
 jlong __jlongpointer od, // odd
 jlong __jlongpointer rv, // row vars
 jint num_rvars,
 jlong __jlongpointer cv, // col vars
 jint num_cvars,
 jlong __jlongpointer ndv,  // nondet vars
 jint num_ndvars
 )
{
    NDSparseMatrix *ndsm = NULL;
    
    DdNode *trans = jlong_to_DdNode(t); //trans/reward matrix
    DdNode **rvars = jlong_to_DdNode_array(rv);   // row vars
    DdNode **cvars = jlong_to_DdNode_array(cv);   // col vars
    DdNode **ndvars = jlong_to_DdNode_array(ndv); // nondet vars
    ODDNode *odd = jlong_to_ODDNode(od);      // reachable states
    
	ndsm = build_nd_sparse_matrix(ddman, trans, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
    
    return ptr_to_jlong(ndsm);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_NDSparseMatrix_PS_1BuildSubNDSparseMatrix
(JNIEnv *env, jclass cls,
 jlong __jlongpointer t,    // trans
 jlong __jlongpointer od, // odd
 jlong __jlongpointer rv, // row vars
 jint num_rvars,
 jlong __jlongpointer cv, // col vars
 jint num_cvars,
 jlong __jlongpointer ndv,  // nondet vars
 jint num_ndvars,
 jlong __jlongpointer r    // reward
 )
{
    NDSparseMatrix *ndsm = NULL;
    
    DdNode *trans = jlong_to_DdNode(t); //trans/reward matrix
    DdNode **rvars = jlong_to_DdNode_array(rv);   // row vars
    DdNode **cvars = jlong_to_DdNode_array(cv);   // col vars
    DdNode **ndvars = jlong_to_DdNode_array(ndv); // nondet vars
    ODDNode *odd = jlong_to_ODDNode(od);      // reachable states
    DdNode *rewards = jlong_to_DdNode(r);
    
	ndsm = build_sub_nd_sparse_matrix(ddman, trans, rewards, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
    
    return ptr_to_jlong(ndsm);
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_NDSparseMatrix_PS_1AddActionsToNDSparseMatrix
(JNIEnv *env, jclass cls,
 jlong __jlongpointer t,    // trans
 jlong __jlongpointer ta,    // trans action labels
 jlong __jlongpointer od, // odd
 jlong __jlongpointer rv, // row vars
 jint num_rvars,
 jlong __jlongpointer cv, // col vars
 jint num_cvars,
 jlong __jlongpointer ndv,  // nondet vars
 jint num_ndvars,
 jlong __jlongpointer nd    // sparse matrix
 )
{
    DdNode *trans = jlong_to_DdNode(t); 			//trans/reward matrix
	DdNode *trans_actions = jlong_to_DdNode(ta);	// trans action labels
    ODDNode *odd = jlong_to_ODDNode(od);      // reachable states
    DdNode **rvars = jlong_to_DdNode_array(rv);   // row vars
    DdNode **cvars = jlong_to_DdNode_array(cv);   // col vars
    DdNode **ndvars = jlong_to_DdNode_array(ndv); // nondet vars
	NDSparseMatrix *ndsm = (NDSparseMatrix *) jlong_to_NDSparseMatrix(nd); // sparse matrix
    
	jstring *action_names_jstrings;
	const char** action_names = NULL;
	int num_actions;
	
	if (trans_actions != NULL) {
		build_nd_action_vector(ddman, trans, trans_actions, ndsm, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	}
}

//------------------------------------------------------------------------------

JNIEXPORT void __jlongpointer JNICALL Java_sparse_NDSparseMatrix_PS_1DeleteNDSparseMatrix
(JNIEnv *env, jclass cls,
 jlong __jlongpointer _ndsm)
{
    NDSparseMatrix * ndsm = (NDSparseMatrix *) jlong_to_NDSparseMatrix(_ndsm);
    if (ndsm) delete ndsm;
}

//------------------------------------------------------------------------------


//----------------------------------------------------------------------------------------------
// PRISM-Leak stuff
//----------------------------------------------------------------------------------------------

// sparse matrix
RMSparseMatrix *rmsm11 = NULL;
CMSRSparseMatrix *cmsrsm11 = NULL;

// flags
bool compact_tr;

int n, nnz;
double *non_zeros;
unsigned char *row_counts;
int *row_starts;
bool use_counts;
unsigned int *cols;
double *dist;
int dist_shift;
int dist_mask;
int *h, *l;
int i;


JNIEXPORT jint JNICALL Java_sparse_PrismSparse_PS_1CreateSparseMatrix
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer m,	// matrix
jstring na,		// matrix name
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer od	// odd
)
{
	DdNode *matrix = jlong_to_DdNode(m);		// matrix
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars
	ODDNode *odd = jlong_to_ODDNode(od);

	// sparse matrix
	rmsm11 = NULL;
	cmsrsm11 = NULL;

	// exception handling around whole function
	try {
	
	// build sparse matrix
	// if requested, try and build a "compact" version
	compact_tr = true;
	cmsrsm11 = NULL;
	if (compact) cmsrsm11 = build_cmsr_sparse_matrix(ddman, matrix, rvars, cvars, num_rvars, odd);
	if (cmsrsm11 != NULL) {
		n = cmsrsm11->n;
		nnz = cmsrsm11->nnz;
	}
	// if not or if it wasn't possible, built a normal one
	else {
		compact_tr = false;
		rmsm11 = build_rm_sparse_matrix(ddman, matrix, rvars, cvars, num_rvars, odd);
		n = rmsm11->n;
		nnz = rmsm11->nnz;
	}
	
	// first get data structure info
	if (!compact_tr) {
		non_zeros = rmsm11->non_zeros;
		row_counts = rmsm11->row_counts;
		row_starts = (int *)rmsm11->row_counts;
		use_counts = rmsm11->use_counts;
		cols = rmsm11->cols;
	} else {
		row_counts = cmsrsm11->row_counts;
		row_starts = (int *)cmsrsm11->row_counts;
		use_counts = cmsrsm11->use_counts;
		cols = cmsrsm11->cols;
		dist = cmsrsm11->dist;
		dist_shift = cmsrsm11->dist_shift;
		dist_mask = cmsrsm11->dist_mask;
	}

    // then traverse data structure
    
    h = new int[n];
    l = new int[n];
    h[0] = 0;
	for (i = 0; i < n; i++) {
	if (!use_counts) { l[i] = row_starts[i]; h[i] = row_starts[i+1]; }
		else {
            if (i==0) {
                l[0] = 0;
                h[0] = row_counts[0]; 
            }
            else{
                l[i] = h[i-1]; 
                h[i] = h[i-1] + row_counts[i]; 
            }
        }
	}
    

    // catch exceptions: return (undocumented) error code for memout
	} catch (std::bad_alloc e) {
		return -2;
	}
    
    return 0;
}

JNIEXPORT jint JNICALL Java_sparse_PrismSparse_PS_1FreeSparseMatrix
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer m,	// matrix
jstring na,		// matrix name
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer od	// odd
)
{
    // free memory
	if (rmsm11) delete rmsm11;
	if (cmsrsm11) delete cmsrsm11;
    if (h) delete h;
    if (l) delete l;
    if (non_zeros) delete non_zeros;
    if (row_counts) delete row_counts;
    if (row_starts) delete row_starts;
    if (cols) delete cols;
    if (dist) delete dist;    

    return 0;
}


JNIEXPORT jdouble JNICALL Java_sparse_PrismSparse_PS_1GetTransitionProb
(
JNIEnv *env,
jclass cls,
jint src,
jint dst,
jlong __jlongpointer m,	// matrix
jstring na,		// matrix name
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer od	// odd
)
{
    // traverse data structure
	int j, c = 0;
    double d = -1;
    for (j = l[src]; j < h[src]; j++) {
	    // "row major" version
	    if (!compact_tr) {
		    c = cols[j];
		    d = non_zeros[j];
	    }
	    // "compact msr" version
	    else {
		    c = (int)(cols[j] >> dist_shift);
		    d = dist[(int)(cols[j] & dist_mask)];
	    }
        if ( c == dst)
            return d;		    
    }
    return -1;
}

// return successor states of src. Succesor states of src contain src itself, if it has a self-loop
JNIEXPORT jintArray JNICALL Java_sparse_PrismSparse_PS_1SuccessorStates
(
JNIEnv *env,
jclass cls,
jint src,
jlong __jlongpointer m,	// matrix
jstring na,		// matrix name
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer od	// odd
)
{
    // traverse data structure
	int j, c;
    double d = -1;
    int succ_size = h[src] - l[src];
    jintArray result = env->NewIntArray(succ_size);
    jint *succ = env->GetIntArrayElements(result, NULL);

    int k = 0;
    for (j = l[src]; j < h[src]; j++) {
	    // "row major" version
	    if (!compact_tr) {
		    c = cols[j];
		    d = non_zeros[j];
	    }
	    // "compact msr" version
	    else {
		    c = (int)(cols[j] >> dist_shift);
		    d = dist[(int)(cols[j] & dist_mask)];
	    }
        succ[k] = c; k++;
    }
    env->ReleaseIntArrayElements(result, succ, NULL);
    return result;
}

// a state is considered final, if it has no successor or the only successor is itself
JNIEXPORT jboolean JNICALL Java_sparse_PrismSparse_PS_1isFinalState
(
JNIEnv *env,
jclass cls,
jint src,
jlong __jlongpointer m,	// matrix
jstring na,		// matrix name
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer od	// odd
)
{
    // traverse data structure
	int j, c;
    int succ_size = h[src] - l[src];
    if (succ_size == 1) {
        j = l[src];
        // "row major" version
	    if (!compact_tr) {
		    c = cols[j];
	    }
	    // "compact msr" version
	    else {
		    c = (int)(cols[j] >> dist_shift);
	    }
        if ( c == src)
            return true;		    
    }
    
    if (succ_size == 0) return true;

    return false;
}

//------------------------------------------------------------------------------

