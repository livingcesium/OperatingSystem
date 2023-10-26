import java.io.FileNotFoundException;
import java.io.IOException;

public class Kernel implements Device{
    private static Scheduler scheduler;
    private static boolean exists = false;
    private VirtualFileSystem VFS;
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
    
    public String getFileSystemLog(){
        return VFS.getLog();
    }
}
