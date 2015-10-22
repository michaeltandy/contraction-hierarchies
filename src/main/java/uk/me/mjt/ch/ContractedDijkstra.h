#include <climits>

int32_t* getCommonEntryIndicesJni(int32_t* a, int32_t* b, int32_t* aTimes, int32_t* bTimes, int32_t aLength, int32_t bLength) {
    int32_t* result = new int[2];
    
    if (aLength==0 || bLength==0) {
        result[0]=-1;
        result[1]=-1;
        return result;
    }

    int32_t shortestTime = INT_MAX;
    int32_t aIdx = 0;
    int32_t bIdx = 0;
    
    int32_t shortestA = -1;
    int32_t shortestB = -1;

    while (aIdx<aLength && bIdx<bLength) {
        int32_t aValue = a[aIdx];
        int32_t bValue = b[bIdx];
        int32_t aTime = aTimes[aIdx];
        int32_t bTime = bTimes[bIdx];
        int32_t thisTime = aTime+bTime;

        bool improvement=(aValue==bValue && thisTime < shortestTime);
        shortestA = (improvement?aIdx:shortestA);
        shortestB = (improvement?bIdx:shortestB);
        shortestTime = (improvement?thisTime:shortestTime);

        aIdx = (aValue<=bValue ? aIdx+1 : aIdx);
        bIdx = (aValue>=bValue ? bIdx+1 : bIdx);
    }


    result[0]=shortestA;
    result[1]=shortestB;
    return result;
}

