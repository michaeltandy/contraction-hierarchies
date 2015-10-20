#include <climits>

int* getCommonEntryIndicesJni(int* a, int* b, int* aTimes, int* bTimes, int aLength, int bLength) {
    int* result = new int[2];
    result[0]=-1;
    result[1]=-1;
    if (aLength==0 || bLength==0) {
        return result;
    }

    int shortestTime = INT_MAX;
    int aIdx = 0;
    int bIdx = 0;
    int aValue = a[aIdx];
    int bValue = b[bIdx];

    while (aIdx<aLength && bIdx<bLength) {
        
        if (aValue==bValue) {
            int aTime = aTimes[aIdx];
            int bTime = bTimes[bIdx];
            int thisTime = aTime+bTime;

            if (thisTime < shortestTime) {
                shortestTime = thisTime;
                result[0]=aIdx;
                result[1]=bIdx;
            }
                
            bIdx++;
            aIdx++;
            aValue = a[aIdx];
            bValue = b[bIdx];
        } else if (aValue > bValue) {
            bIdx++;
            bValue = b[bIdx];
        } else {
            aIdx++;
            aValue = a[aIdx];
        }
    }


    return result;
}

