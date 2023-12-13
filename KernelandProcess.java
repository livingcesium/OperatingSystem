import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

public class KernelandProcess {
    public final static int pageBufferSize = 100;
    private VirtualToPhysicalMapping[] virtualToPhysicalPage = new VirtualToPhysicalMapping[pageBufferSize];
    private String name;
    private static int nextpid = 0;
    private final int pid;
    private boolean started;
    private Thread thread;
    private Priority priority;
    private final Object priorityLock = new Object();
    private int demoteFuse = 5;
    public static final int MAXDEVICES = 10;
    private final int[] deviceIds = new int[MAXDEVICES];
    private final LinkedList<KernelMessage> kernelMessages = new LinkedList<>();
    private final UserlandProcess up;
    private final Scheduler scheduler;
    
    KernelandProcess(UserlandProcess up, Scheduler scheduler){
        this.scheduler = scheduler;
        Arrays.fill(deviceIds, -1);
        pid = nextpid++;
        thread = new Thread(up, String.format("[Kerneland] Process, PID%d", pid));
        priority = Priority.INTERACTIVE;
        name = up.getClass().getSimpleName();
        this.up = up;
        init();
    }
    KernelandProcess(UserlandProcess up, Priority p, Scheduler scheduler){
        this.scheduler = scheduler;
        Arrays.fill(deviceIds, -1);
        pid = nextpid++;
        thread = new Thread(up, String.format("[Kerneland] Process, PID%d", pid));
        priority = p;
        name = up.getClass().getSimpleName();
        this.up = up;
        init();
    }

    private void init(){
        // From a bygone era
    }

    public void getMapping(int pageNumber){
        if(virtualToPhysicalPage[pageNumber] == null)
            OS.segFault("Attempted to access virtual address %d, which is outside of this processes memory bounds".formatted(pageNumber * OS.pageSize));
        
        int physicalPageNumber = virtualToPhysicalPage[pageNumber].physicalPageNumber;
        
        if(physicalPageNumber == -1){
            // Allocate a new (maybe stolen) physical page
            int nextPage = OS.nextFreePhysicalPage();
            if (nextPage != -1)
                virtualToPhysicalPage[pageNumber].physicalPageNumber = nextPage;
            else{
                // Page swap
                KernelandProcess randomProcess;
                int takenPhysicalPage = -1;
                int pageFileFD;
                int outOfMemoryFuse = Integer.MAX_VALUE;
                while(takenPhysicalPage == -1 && outOfMemoryFuse != 0){
                    randomProcess = scheduler.getRandomProcess();
                    for(VirtualToPhysicalMapping mapping : randomProcess.virtualToPhysicalPage){
                        ///
                        if(mapping != null && (takenPhysicalPage = mapping.physicalPageNumber) != -1){
                            mapping.physicalPageNumber = -1;
                            if(mapping.diskPageNumber == -1)
                                mapping.diskPageNumber = OS.pageFileEnd++;
                            try{
                                pageFileFD = OS.open("file swap.page");
                                OS.seek(pageFileFD, mapping.diskPageNumber * OS.pageSize);
                                OS.write(pageFileFD, Arrays.copyOfRange(UserlandProcess.memory, takenPhysicalPage * OS.pageSize, takenPhysicalPage * OS.pageSize + OS.pageSize));
                            } catch (Exception e) {
                                throw new RuntimeException("Broken page file, got exception: \n", e);
                            }
                            virtualToPhysicalPage[pageNumber].physicalPageNumber = takenPhysicalPage;
                            break;
                        } else
                            outOfMemoryFuse--;
                    }
                }
                if(outOfMemoryFuse == 0)
                    OS.segFault("Out of memory");
            }
            
            // If there is data on the page file, load it
            if(virtualToPhysicalPage[pageNumber].diskPageNumber != -1){
                int pageFileFD;
                try{
                    pageFileFD = OS.open("file swap.page");
                    OS.seek(pageFileFD, virtualToPhysicalPage[pageNumber].diskPageNumber * OS.pageSize);
                    System.arraycopy(OS.read(pageFileFD, OS.pageSize), 0, UserlandProcess.memory, virtualToPhysicalPage[pageNumber].physicalPageNumber * OS.pageSize, OS.pageSize);
                } catch (Exception e) {
                    throw new RuntimeException("Broken page file, got exception: \n", e);
                }
            } else
                // Zero out the page
                Arrays.fill(UserlandProcess.memory, virtualToPhysicalPage[pageNumber].physicalPageNumber * OS.pageSize, virtualToPhysicalPage[pageNumber].physicalPageNumber * OS.pageSize + OS.pageSize, (byte) 0);
        }
        
        
        int random = new Random().nextInt(up.getTLB().length);
        up.getTLB()[random][0] = pageNumber;
        up.getTLB()[random][1] = virtualToPhysicalPage[pageNumber].physicalPageNumber;
    }

    public void stop(){
        
        
        if(started) thread.suspend();
            else throw new UnsupportedOperationException(String.format("Process PID%d could not be stopped, was not running", pid));
    }
    
    public boolean isDone(){
        if(started && !thread.isAlive()) return true;
            else return false;
    }
    
    public void run(){
        if(started) thread.resume();
            else {
                started = true;
                thread.start();
            }
   }
   
   public boolean burnFuse(){
        demoteFuse--;
        if(Debug.flag)
           System.out.printf("$> Process PID%d burned %d/5 fuses%n", pid, 5 - demoteFuse);
        return checkDemotion();
   }
   private boolean checkDemotion(){
        synchronized (priorityLock) {
            if (demoteFuse == 0) {
                demoteFuse = 5;
                if (priority == Priority.REALTIME) priority = Priority.INTERACTIVE;
                else if (priority == Priority.INTERACTIVE) priority = Priority.BACKGROUND;
                // Backgrounds can't get demoted further
                if (Debug.flag)
                    System.out.printf("$> Process PID%d demoted to %s%n", pid, priority);
                return true;
            }
            return false;
        }
   }
   
   public void receiveMessage(KernelMessage msg){
         synchronized(kernelMessages){
              kernelMessages.add(msg);
         }
   }
   public boolean hasMessage(){
         synchronized(kernelMessages){
              return !kernelMessages.isEmpty();
         }
   }
    public KernelMessage takeMessage(){
            synchronized(kernelMessages){
                  return kernelMessages.removeFirst();
            }
    }
   public String getName(){ return name; }
   public int getDeviceId(int i){ return deviceIds[i]; }
    public void setDeviceId(int i, int id){ deviceIds[i] = id; }
   public int getPid(){ return pid; }
    public Priority getPriority(){ synchronized (priorityLock) {return priority;} }
    public String toString(){
          return String.format("PID%d", pid);
    }

    public VirtualToPhysicalMapping[] getVirtualToPhysicalPage(){
        return virtualToPhysicalPage;
    }
    public boolean hasPage(int pageNumber){
        return virtualToPhysicalPage[pageNumber] != null;
    }
    public int toPhysicalPage(int virtualPage){
        return virtualToPhysicalPage[virtualPage].physicalPageNumber;
    }

    public void removePage(int pageNumber){
        virtualToPhysicalPage[pageNumber] = null;
        up.removeFromTLB(pageNumber);
    }

    public void resetTLB(){
        up.resetTLB();
    }

    public void kill(){
        started = true; // Catch all, will always kill even if not started
        thread.stop();
    }
    
}
