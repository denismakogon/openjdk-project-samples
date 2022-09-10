
#include <vector>
#include <immintrin.h>
#include <stdio.h>


using namespace std;

vector<float> sumOf(vector<float>& one, vector<float>& two) {
    auto one_size = one.size();
    vector<float> result(one_size);

    constexpr auto floatsInAVXregister = 8u;
    const auto samples = (one_size / floatsInAVXregister) * floatsInAVXregister;

    for(int i = 0; i < samples; i += floatsInAVXregister) {
        auto loaded_one = _mm256_loadu_ps(one.data() + i);
        auto loaded_two = _mm256_loadu_ps(two.data() + i);

        _mm256_storeu_ps(result.data() + i, _mm256_add_ps(loaded_one, loaded_two));
    }

    for (int j = samples; j < result.size(); j++) {
        result[j] = one[j] + two[j];
    }

    return result;
}

void printVector(vector<float>& v) {
       printf("vector size: %lu\n", v.size());
    for (int i = 0; i < v.size(); i++) {
        printf("%f\n", v[i]);
    }
}

void printArray(float* arr, int size) {
       printf("array size: %d\n", size);
    for (int i = 0; i < size; i++) {
        printf("%f\n", arr[i]);
    }
}

extern "C" void* toVectorPointer(float* array, int size) {
    vector<float>* ptr = new vector<float>();
    for(int i = 0; i < size; i++) {
        ptr->push_back(array[i]);
    }
    return reinterpret_cast<void *>(ptr);
}

extern "C" float* mm256_add_ps(void* vectorPtrOne, void* vectorPtrTwo) {
    auto voidPtrToOne = static_cast<vector<float>*>(vectorPtrOne);
    auto voidPtrToTwo = static_cast<vector<float>*>(vectorPtrTwo);
    auto result = sumOf(*voidPtrToOne, *voidPtrToTwo);

    float* resultingArr = new float[result.size()];
    for(int i = 0; i < result.size(); i ++) {
        resultingArr[i] = result[i];
    }
    return resultingArr;
}

int main() {
    const int size = 10;
    float* _one = new float[size] {1.f, 2.f, 3.f, 4.f, 5.f, 6.f, 7.f, 8.f, 9.f, 10.f};
    float* _two = new float[size] {1.f, 2.f, 3.f, 4.f, 5.f, 6.f, 7.f, 8.f, 9.f, 10.f};

    auto voidPtrToOne = toVectorPointer(_one, size);
    auto voidPtrToTwo = toVectorPointer(_two, size);

    auto result = mm256_add_ps(voidPtrToOne, voidPtrToTwo);

    printArray(result, size);
}
