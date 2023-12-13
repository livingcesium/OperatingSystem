public class VirtualToPhysicalMapping {
    public int physicalPageNumber;
    public int diskPageNumber;
    
    public VirtualToPhysicalMapping(){
        physicalPageNumber = -1;
        diskPageNumber = -1;
    }
    
    public VirtualToPhysicalMapping(int physicalPageNumber, int diskPageNumber){
        this.physicalPageNumber = physicalPageNumber;
        this.diskPageNumber = diskPageNumber;
    }
}
