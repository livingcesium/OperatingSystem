import java.io.FileNotFoundException;
import java.io.IOException;

public class Kernel implements Device{
    private static Scheduler scheduler;
    private static boolean exists = false;
    private VirtualFileSystem VFS;
    private boolean[] freeMemory = new boolean[UserlandProcess.memorySize];
    public Kernel(){
        if(exists)
            throw new RuntimeException("Kernel is a singleton");    
        scheduler = new Scheduler(this);
        VFS = new VirtualFileSystem(10);
        exists = true;
    }
    public void createProcess(UserlandProcess up){
        scheduler.createProcess(up);
    }
    public void createProcess(UserlandProcess up, Priority priority){ scheduler.createProcess(up, priority); }
    
    public static void sleep(int milliseconds){
        scheduler.sleep(milliseconds);
        if(Debug.flag)
            System.out.printf("%s being put to sleep for %d milliseconds%n", scheduler.reportCurrentProcess(), milliseconds);
    }
    
    public int open(String s) throws FileNotFoundException {
        KernelandProcess process = scheduler.getCurrentlyRunning();
        if(process == null)
            throw new RuntimeException("No process running");
        for (int i = 0; i < KernelandProcess.MAXDEVICES; i++) {
            if(process.getDeviceId(i) == -1){
                process.setDeviceId(i, VFS.open(s));
                return i;
            }
        }
        return -1; // Fail if no empty space found
    }

    @Override
    public void close(int id) throws IOException {
        KernelandProcess process = scheduler.getCurrentlyRunning();
        if(process == null)
            throw new RuntimeException("No process running");
        int deviceId = process.getDeviceId(id);
        if(deviceId == -1)
            throw new RuntimeException("No device with that id");
        process.setDeviceId(id, -1);
        VFS.close(deviceId);
    }
    
    public void closeAll() throws IOException {
        // Closes all open devices on current process
        
        KernelandProcess process = scheduler.getCurrentlyRunning();
        if(process == null)
            throw new RuntimeException("No process running");
        for (int i = 0; i < KernelandProcess.MAXDEVICES; i++) {
            if(process.getDeviceId(i) != -1){
                VFS.close(process.getDeviceId(i));
                process.setDeviceId(i, -1);
            }
        }
    }

    @Override
    public byte[] read(int id, int size) throws IOException {
        KernelandProcess process = scheduler.getCurrentlyRunning();
        if(process == null)
            throw new RuntimeException("No process running");
        int deviceId = process.getDeviceId(id);
        if(deviceId == -1)
            throw new RuntimeException("No device with that id");
        return VFS.read(deviceId, size);
        
    }

    @Override
    public void seek(int id, int to) throws IOException {
        KernelandProcess process = scheduler.getCurrentlyRunning();
        if(process == null)
            throw new RuntimeException("No process running");
        int deviceId = process.getDeviceId(id);
        if(deviceId == -1)
            throw new RuntimeException("No device with that id");
        VFS.seek(deviceId, to);
    }

    @Override
    public int write(int id, byte[] data) throws IOException {
        KernelandProcess process = scheduler.getCurrentlyRunning();
        if(process == null)
            throw new RuntimeException("No process running");
        int deviceId = process.getDeviceId(id);
        if(deviceId == -1)
            throw new RuntimeException("No device with that id");
        return VFS.write(deviceId, data);
    }
    
    public int getPid(){
        return scheduler.getCurrentlyRunning().getPid();
    }
    
    public int getPidByName(String name){
        return scheduler.getPidByName(name);
    }
    public void sendMessage(KernelMessage msg){
        scheduler.sendMessage(msg);
    }
    public KernelMessage awaitMessage(){
        return scheduler.awaitMessage();
    }
    
    public String getFileSystemLog(){
        return VFS.getLog();
    }

    public void getMapping(int pageNumber){
        KernelandProcess process = scheduler.getCurrentlyRunning();
        process.getMapping(pageNumber);
    }

    public int allocateMemory(int size){
        KernelandProcess process = scheduler.getCurrentlyRunning();
        VirtualToPhysicalMapping[] virtualToPhysicalPage = process.getVirtualToPhysicalPage();
        int virtualPageNumber = 0;
        virtualPageNumber = -1;
        
        for (int allocated = 0; allocated < size; allocated++) {
            if(virtualToPhysicalPage[++virtualPageNumber] == null)
                virtualToPhysicalPage[virtualPageNumber] = new VirtualToPhysicalMapping();

            if(hasSpace(allocated, size)){
                int last = 0;
                for (int i = allocated; i < allocated + size; i++)
                    freeMemory[i] = true;
                for (int i = allocated * OS.pageSize; i < (allocated + size) * OS.pageSize ; i++) {
                    UserlandProcess.memory[i] = 0;
                    last = i;
                }
                System.out.println(last + 1);
                virtualToPhysicalPage[virtualPageNumber].physicalPageNumber = allocated;
            }
        }
        
        return virtualPageNumber; // If no space is found, memory is allocated but not mapped (a page file will be created)
    }

    public boolean freeMemory(int virtualPageNumber, int size){
        KernelandProcess process = scheduler.getCurrentlyRunning();
        if(!process.hasPage(virtualPageNumber))
            return false;
        int physicalPage;
        for (int i = virtualPageNumber; i < virtualPageNumber + size; i++) {
            if((physicalPage = process.toPhysicalPage(i)) != -1)
                freeMemory[physicalPage] = false;
            
            process.removePage(i);
        }
        return true;
    }

    // Returns true if there is a contiguous block of memory starting at start, with length size
    private boolean hasSpace(int start, int size){
        for (int i = start; i < start + size; i++) {
            if(freeMemory[i])
                return false;
        }
        return true;
    }
    
    public int nextFreePhysicalPage(){
        for (int i = 0; i < freeMemory.length; i++) {
            if(!freeMemory[i])
                return i;
        }
        return -1;
    }

    public void segFault(String message){
        KernelandProcess process = scheduler.getCurrentlyRunning();
        System.out.printf("<!> Process %s caused a segmentation fault: %s%n", process, message);
        System.out.printf("    Process %s is being terminated%n", process);
        process.kill();
        scheduler.switchProcess();
    }
    
}
