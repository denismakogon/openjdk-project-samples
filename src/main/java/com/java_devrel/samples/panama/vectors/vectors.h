#define vectors_h

#ifdef __cplusplus
extern "C" {
#endif

// transforms float array into void ptr because vector<T> is C++ type hense not exportable
void* toVectorPointer(float* array, int size);

// sums two vectors passed in a form of void pointers
float* mm256_add_ps(void* vectorPtrOne, void* vectorPtrTwo);

#ifdef __cplusplus
}
#endif
