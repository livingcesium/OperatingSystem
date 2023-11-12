import java.io.FileNotFoundException;
import java.io.IOException;

public class OS {
    public static final int pageSize = 1024; // in bytes
    private static Kernel kernel;
    
    public static void startup(UserlandProcess init){
        kernel = new Kernel();
        kernel.createProcess(init);
    }

    public static void createProcess(UserlandProcess up){
        kernel.createProcess(up);
    }
    public static void createProcess(UserlandProcess up, Priority priority){ kernel.createProcess(up, priority); }
    
    public static void sleep(int milliseconds){
        kernel.sleep(milliseconds);
    }
    
    public static int open(String s) throws FileNotFoundException { return kernel.open(s); }
    public static void close(int id) throws IOException { kernel.close(id); }
    public static void closeAll() throws IOException { kernel.closeAll(); }
    public static void seek(int id, int to) throws IOException { kernel.seek(id, to); }
    public static int write(int id, byte[] data) throws IOException { return kernel.write(id, data); } 
    public static byte[] read(int id, int size) throws IOException { return kernel.read(id, size); }
    public static String getFileSystemLog() { return kernel.getFileSystemLog(); }
    public static int getPid() { return kernel.getPid(); }
    public static int getPidByName(String name) { return kernel.getPidByName(name); }
    public static void sendMessage(KernelMessage msg){
        kernel.sendMessage(msg);
    }
    public static KernelMessage awaitMessage(){
        return kernel.awaitMessage();
    }
    
    public static void GetMapping(int pageNumber){
        kernel.getMapping(pageNumber);
    }

    // accepts byte size, converts to page size
    public static int allocateMemory(int size){
        if (size % pageSize != 0) {
            System.out.println("<!> Size not page aligned, failed to allocate memory");
            return -1;
        }
        return kernel.allocateMemory(size/pageSize);
    }

    // accepts byte size, converts to page size
    public static boolean freeMemory(int pointer, int size){
        if (size % pageSize != 0 || pointer % pageSize != 0) {
            System.out.println("<!> Virtual address and/or size not page aligned, failed to free memory");
            return false;
        }
        return kernel.freeMemory(pointer/pageSize, size/pageSize);
    }

    public static void segFault(String message){
        kernel.segFault(message);
    }
}
