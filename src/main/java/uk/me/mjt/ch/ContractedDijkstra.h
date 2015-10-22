#include <climits>

int32_t* getCommonEntryIndicesJni(int32_t* a, int32_t* b, int32_t* aTimes, int32_t* bTimes, int32_t aLength, int32_t bLength) {

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
        
        asm volatile ("cmp %1, %2; \n" 
             "cmovne %3, %0; \n"
             :"=r"(thisTime) /* outputs */
             :"r"(aValue),"r"(bValue),"r"(shortestTime),"0"(thisTime) /* inputs */
             //: /* no registers clobbered */
             );
        //thisTime = (aValue==bValue?thisTime:shortestTime);

        asm volatile ("cmp %4, %3; \n" 
             "cmovl %3, %2; \n"
             "cmovl %5, %0; \n"
             "cmovl %6, %1; \n"
             :"=r"(shortestA),"=r"(shortestB),"=r"(shortestTime) /* outputs */
             :"r"(thisTime),"r"(shortestTime),"r"(aIdx),"r"(bIdx),"0"(shortestA),"1"(shortestB),"2"(shortestTime) /* inputs */
             //: /* no registers clobbered */
             );
        //shortestA = (thisTime<shortestTime?aIdx:shortestA);
        //shortestB = (thisTime<shortestTime?bIdx:shortestB);
        //shortestTime = (thisTime<shortestTime?thisTime:shortestTime);

        int32_t aIdxNext = aIdx+1;
        int32_t bIdxNext = bIdx+1;

        asm volatile ("cmp %3, %2; \n" 
             "cmovle %4, %0; \n"
             "cmovge %5, %1; \n"
             :"=r"(aIdx), "=r"(bIdx) /* outputs */
             :"r"(aValue),"r"(bValue),"r"(aIdxNext),"r"(bIdxNext),"0"(aIdx),"1"(bIdx) /* inputs */
             //: /* no registers clobbered */
             );

        //aIdx = (aValue<=bValue ? aIdxNext : aIdx);
        //bIdx = (aValue>=bValue ? bIdxNext : bIdx);
    }

    int32_t* result = new int[2];
    result[0]=shortestA;
    result[1]=shortestB;
    return result;
}

