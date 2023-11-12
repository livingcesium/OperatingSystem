import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

public class KernelandProcess {
    public final static int pageBufferSize = 100;
    private int[] virtualToPhysicalPage = new int[pageBufferSize];
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
    
    KernelandProcess(UserlandProcess up){
        Arrays.fill(deviceIds, -1);
        pid = nextpid++;
        thread = new Thread(up, String.format("[Kerneland] Process, PID%d", pid));
        priority = Priority.INTERACTIVE;
        name = up.getClass().getSimpleName();
        this.up = up;
        init();
    }
    KernelandProcess(UserlandProcess up, Priority p){
        Arrays.fill(deviceIds, -1);
        pid = nextpid++;
        thread = new Thread(up, String.format("[Kerneland] Process, PID%d", pid));
        priority = p;
        name = up.getClass().getSimpleName();
        this.up = up;
        init();
    }

    private void init(){
        Arrays.fill(virtualToPhysicalPage, -1);
    }

    public void getMapping(int pageNumber){
        int physicalPageNumber = virtualToPhysicalPage[pageNumber];

        if(physicalPageNumber == -1) OS.segFault("Attempted to access virtual address %d, which is outside of this processes memory bounds".formatted(pageNumber));
        int random = new Random().nextInt(up.getTLB().length);
        up.getTLB()[random][0] = pageNumber;
        up.getTLB()[random][1] = physicalPageNumber;
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

    public int[] getVirtualToPhysicalPage(){
        return virtualToPhysicalPage;
    }
    public int toPhysicalPage(int virtualPage){
        return virtualToPhysicalPage[virtualPage];
    }

    public void removePage(int pageNumber){
        virtualToPhysicalPage[pageNumber] = -1;
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
