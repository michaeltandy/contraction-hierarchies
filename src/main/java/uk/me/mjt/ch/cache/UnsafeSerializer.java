
package uk.me.mjt.ch.cache;

import java.lang.reflect.Field;
import sun.misc.Unsafe;
import uk.me.mjt.ch.PartialSolution;
import uk.me.mjt.ch.Preconditions;


public class UnsafeSerializer {
    private static final Unsafe UNSAFE = getUnsafe();
    int ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    int SIZE_OF_LONG = UNSAFE.arrayIndexScale(long[].class);
    int SIZE_OF_INT = UNSAFE.arrayIndexScale(long[].class);
    
    /*public static void main(String[] args) {
        Unsafe unsafe = getUnsafe();
        
        System.out.println("long base offset: " + unsafe.arrayBaseOffset(long[].class));
        System.out.println("long index scale: " + unsafe.arrayIndexScale(long[].class));
        
        System.out.println("byte base offset: " + unsafe.arrayBaseOffset(byte[].class));
        System.out.println("byte index scale: " + unsafe.arrayIndexScale(byte[].class));
        
        long[] longs = new long[] {1, 2, 3, 4, 5};
        byte[] bytes = new byte[] {1, 2, 3, 4, 5};
        
        for (long i=-10 ; i<100 ; i++) {
            System.out.println(i + "\t" + unsafe.getByte(longs,i) + "\t" + unsafe.getByte(bytes,i));
        }
        
        int arrayBaseOffset = unsafe.arrayBaseOffset(byte[].class);
        int sizeOfLong = unsafe.arrayIndexScale(long[].class);
        int sizeOfByte = unsafe.arrayIndexScale(byte[].class);
        int bytesPerLong = sizeOfLong/sizeOfByte;
        
        long[] sourceArray = longs;
        int bytesOfLongs = bytesPerLong*sourceArray.length;
        byte[] byteCopy = new byte[bytesOfLongs];
        System.out.println("Before copy: " + Arrays.toString(byteCopy));
        
        unsafe.copyMemory(sourceArray, arrayBaseOffset, byteCopy, arrayBaseOffset, bytesOfLongs);
        
        System.out.println("After copy: " + Arrays.toString(byteCopy));
    }*/
    
    public byte[] serialize(UpAndDownPair upDownPair) {
        Preconditions.checkNoneNull(upDownPair);
        
        int upEntries = upDownPair.up.getNodeIds().length;
        int downEntries = upDownPair.down.getNodeIds().length;
        
        int sizeOfEntry = (3*SIZE_OF_LONG)+SIZE_OF_INT;
        int bufferSize = sizeOfEntry*(upEntries+downEntries) + 3*SIZE_OF_INT;
        int[] header = new int[] {1, upEntries, downEntries};
        
        byte[] result = new byte[bufferSize];
        int currentOffset = ARRAY_BASE_OFFSET;
        currentOffset = copyArrayUpdatingOffset(header, result, currentOffset);
        currentOffset = copyPartialSolutionUpdatingOffset(upDownPair.up,result, currentOffset);
        currentOffset = copyPartialSolutionUpdatingOffset(upDownPair.down,result, currentOffset);
        
        UpAndDownPair readback = deserialize(result); // REVISIT faster not to check, if we're confident :)
        if (!readback.up.equals(upDownPair.up) || !readback.down.equals(upDownPair.down)) {
            throw new RuntimeException("Serialisation problem?");
        }
        
        return result;
    }
    
    private int copyPartialSolutionUpdatingOffset(PartialSolution ps, byte[] destination, int destinationOffset) {
        destinationOffset = copyArrayUpdatingOffset(ps.getNodeIds(), destination, destinationOffset);
        destinationOffset = copyArrayUpdatingOffset(ps.getContractionOrders(), destination, destinationOffset);
        destinationOffset = copyArrayUpdatingOffset(ps.getTotalDriveTimes(), destination, destinationOffset);
        destinationOffset = copyArrayUpdatingOffset(ps.getViaEdges(), destination, destinationOffset);
        return destinationOffset;
    }
    
    private int copyArrayUpdatingOffset(int[] toCopy, byte[] destination, int destinationOffset) {
        UNSAFE.copyMemory(toCopy, ARRAY_BASE_OFFSET, destination, destinationOffset, toCopy.length*SIZE_OF_INT);
        return destinationOffset+toCopy.length*SIZE_OF_INT;
    }
    
    private int copyArrayUpdatingOffset(long[] toCopy, byte[] destination, int destinationOffset) {
        UNSAFE.copyMemory(toCopy, ARRAY_BASE_OFFSET, destination, destinationOffset, toCopy.length*SIZE_OF_LONG);
        return destinationOffset+toCopy.length*SIZE_OF_LONG;
    }
    
    public UpAndDownPair deserialize(byte[] binary) {
        if (binary == null)
            return null;
        
        int currentOffset = ARRAY_BASE_OFFSET;
        
        int[] header = new int[3];
        currentOffset = extractArrayUpdatingOffset(binary, currentOffset, header);
        int upEntries = header[1];
        int downEntries = header[2];
        
        long[] upNodeIds = new long[upEntries];
        currentOffset = extractArrayUpdatingOffset(binary, currentOffset, upNodeIds);
        long[] upContractionOrders = new long[upEntries];
        currentOffset = extractArrayUpdatingOffset(binary, currentOffset, upContractionOrders);
        int[] upDriveTimes = new int[upEntries];
        currentOffset = extractArrayUpdatingOffset(binary, currentOffset, upDriveTimes);
        long[] upViaEdges = new long[upEntries];
        currentOffset = extractArrayUpdatingOffset(binary, currentOffset, upViaEdges);
        PartialSolution.UpwardSolution up = new PartialSolution.UpwardSolution(upNodeIds, upContractionOrders, upDriveTimes, upViaEdges);
        
        long[] downNodeIds = new long[downEntries];
        currentOffset = extractArrayUpdatingOffset(binary, currentOffset, downNodeIds);
        long[] downContractionOrders = new long[downEntries];
        currentOffset = extractArrayUpdatingOffset(binary, currentOffset, downContractionOrders);
        int[] downDriveTimes = new int[downEntries];
        currentOffset = extractArrayUpdatingOffset(binary, currentOffset, downDriveTimes);
        long[] downViaEdges = new long[downEntries];
        currentOffset = extractArrayUpdatingOffset(binary, currentOffset, downViaEdges);
        PartialSolution.DownwardSolution down = new PartialSolution.DownwardSolution(downNodeIds, downContractionOrders, downDriveTimes, downViaEdges);
        
        return new UpAndDownPair(up, down);
    }
    
    private int extractArrayUpdatingOffset(byte[] source, int sourceOffset, int[] destination) {
        UNSAFE.copyMemory(source, sourceOffset, destination, ARRAY_BASE_OFFSET, destination.length*SIZE_OF_INT);
        return sourceOffset+destination.length*SIZE_OF_INT;
    }
    
    private int extractArrayUpdatingOffset(byte[] source, int sourceOffset, long[] destination) {
        UNSAFE.copyMemory(source, sourceOffset, destination, ARRAY_BASE_OFFSET, destination.length*SIZE_OF_LONG);
        return sourceOffset+destination.length*SIZE_OF_LONG;
    }
    
    private static Unsafe getUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);
            return unsafe;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
