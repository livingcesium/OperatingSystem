public abstract class UserlandProcess implements Runnable{

    public static final int memorySize = 1024; // in pages

    // TLB[virtualPage][physicalPage]
    private static int[][] translationLookaside = new int[2][2];
    public static byte[] memory = new byte[OS.pageSize * memorySize];


    public byte read(int address) {
        int physicalAddress = getPhysicalAddress(address);
        return memory[physicalAddress];

    }
    public void write(int address, byte value){
        int physicalAddress = getPhysicalAddress(address);
        memory[physicalAddress] = value;
    }
    public int[][] getTLB (){
        return translationLookaside;
    }

    public void resetTLB(){
        translationLookaside = new int[2][2];
    }

    public void removeFromTLB(int virtualAddress){
        int pageNumber = virtualAddress / OS.pageSize;
        for (int i = 0; i < translationLookaside.length; i++) {
            if(translationLookaside[i][0] == pageNumber){
                translationLookaside[i][0] = -1;
                translationLookaside[i][1] = -1;
            }
        }
    }

    private int getPhysicalAddress(int virtualAddress){
        int pageNumber = virtualAddress / OS.pageSize;
        if(virtualAddress >= OS.pageSize) OS.segFault("Attempted to access virtual address %d, which is outside of this processes memory bounds".formatted(virtualAddress));
        int offset = virtualAddress % OS.pageSize;
        int physicalPage = -1;
        int attempts = 0;
        while(attempts < 100) {
            for (int i = 0; i < translationLookaside.length; i++) {
                if (translationLookaside[i][0] == pageNumber) {
                    physicalPage = translationLookaside[i][1];
                    break;
                }
            }
            if(physicalPage >= 0) {
                break;
            } else {
                attempts++;
                OS.GetMapping(pageNumber);
            }
        }

        if (physicalPage < 0) throw new RuntimeException("Physical page not found after %d attempts".formatted(attempts));
        int physicalAddress = physicalPage * OS.pageSize + offset;
        if(physicalAddress >= memory.length) OS.segFault("Attempted to access physical address %d, which is outside of system memory bounds".formatted(physicalAddress));
        return physicalAddress;
    }
}
