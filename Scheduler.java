import java.io.IOException;
import java.util.*;
import java.time.Clock;

public class Scheduler {
    private static final int QUANTUM = 100; // milliseconds
    
    private static final int REALTIME_WEIGHT = 6; 
    private static final int INTERACTIVE_WEIGHT = 3; 
    private static final int BACKGROUND_WEIGHT = 1;
    private final Random rand = new Random();

    private class SleepingProcess{ // mini class for sleepers
        public KernelandProcess process;
        public double timeToWake;
        public SleepingProcess(KernelandProcess process, double millis){this.process = process; timeToWake = millis;}

        @Override
        public String toString() {return String.format("%s", process.toString());}
    }
    private final List<SleepingProcess> sleepingProcesses = Collections.synchronizedList(new LinkedList<SleepingProcess>(){
        @Override
        public boolean add(SleepingProcess sleepingProcess) {
            // GUARANTEED SORTED, BABY
            synchronized (this){
                if(this.isEmpty())
                    return super.add(sleepingProcess);
                int index = this.size() - 1;
                while(this.get(index).timeToWake > sleepingProcess.timeToWake && index > 0){
                    index--; //n^2 in worst case BUT THAT CASE SHOULD RARELY COME UP!
                    //debug to show elements getting sorted
                    if(Debug.verbose)
                        System.out.printf("SleepingProcess PID%d (%fUTC) moved behind process PID%d (%fUTC)%n", this.get(index).process.getPid(), this.get(index).timeToWake, sleepingProcess.process.getPid(), sleepingProcess.timeToWake);
                }

                super.add(index, sleepingProcess);
                return true; // This should never not modify, so always true
            }
            
        }
    });
    private final List<KernelandProcess> realTimes;
    private final List<KernelandProcess> interactives;
    private final List<KernelandProcess> backgrounds;
    private final Map<Integer, KernelandProcess> messageWaiters;
    private Map<Integer, KernelandProcess> processes;
    
    private Timer timer;
    private KernelandProcess currentProcess;
    private final Object currentProcessLock = new Object();
    private final Kernel kernel;
    
    public Scheduler(Kernel kernel){
        this.kernel = kernel;
        timer = new Timer();

        class ProcessSwitcher extends TimerTask  {
            public void run() {
                try{
                    synchronized (currentProcessLock){
                        switchProcess();
                    }
                } catch (Exception ignored){
                    if(Debug.verbose)
                        System.out.println("!> No processes to switch to, lets try next time");
                }
            }
        }
        
        timer.schedule(new ProcessSwitcher(), 0, QUANTUM);
        
        realTimes = Collections.synchronizedList(new LinkedList<KernelandProcess>());
        interactives = Collections.synchronizedList(new LinkedList<KernelandProcess>());
        backgrounds = Collections.synchronizedList(new LinkedList<KernelandProcess>());
        processes = Collections.synchronizedMap(new HashMap<Integer, KernelandProcess>());
        messageWaiters = Collections.synchronizedMap(new HashMap<Integer, KernelandProcess>());
    }
    
    public int createProcess(UserlandProcess up){
        KernelandProcess kernelProcess = new KernelandProcess(up);
        addToQueue(kernelProcess, false);
        processes.put(kernelProcess.getPid(), kernelProcess);
        synchronized (currentProcessLock) {
            if (currentProcess == null)
                switchProcess();
        }
        
        return kernelProcess.getPid();
    }
    
    public int createProcess(UserlandProcess up, Priority priority){
        KernelandProcess kernelProcess = new KernelandProcess(up, priority);
        addToQueue(kernelProcess, false);
        processes.put(kernelProcess.getPid(), kernelProcess);
        
        synchronized (currentProcessLock) {
            if (currentProcess == null)
                switchProcess();
        }
        return kernelProcess.getPid();
    }
    
    // Should be ran with the lock on currentProcess
    public void switchProcess(){

        handleSleepers();
        
        if (currentProcess != null) {
            pauseExecution();
        }
        
        runNextProcess(); // May not run anything if there are no processes to run
        
    }

    private void handleSleepers(){
        synchronized (sleepingProcesses){
            if(!sleepingProcesses.isEmpty()){
                double currentTime = Clock.systemUTC().millis();
                double diff;
                for (SleepingProcess sleeper : sleepingProcesses) {
                    if ((diff = sleeper.timeToWake - currentTime) <= 0) { // if negative, we passed the time to wake up
                        if(Debug.flag)
                            System.out.printf("\tProcess PID%d woke up %f milliseconds after scheduled, %s%n", sleeper.process.getPid(), -diff, sleepingProcesses);
                        addToQueue(sleeper.process, false);
                        if(Debug.flag)
                            System.out.println("ADDED TO QUEUE FROM WAKE UP LOGIC");
                        sleepingProcesses.remove(sleeper);
                    } // else break; IF THE SORTING THING WORKS, THIS SHOULD BE VALID BUT WE CAN MAKE SURE THIS WORKS LATER
                    
                    if(Debug.flag)
                        System.out.printf("\tProcess PID%d has %f milliseconds until wake up%n", sleeper.process.getPid(), sleeper.timeToWake - currentTime);

                }
            }
        }
    }

    private void pauseExecution(){
        currentProcess.burnFuse();
        
        if(!currentProcess.isDone()) { // adds unfinished or demoted processes back to the right queue
            addToQueue(currentProcess, false);
        }
        currentProcess.stop();
        if(Debug.flag)
            System.out.printf("!> Process PID%d stopped%n", currentProcess.getPid());
        
        try { 
            kernel.closeAll(); 
        } catch (IOException ex) { 
            throw new RuntimeException(String.format("Process PID%d could not close all devices, got exception \n\n%s", currentProcess.getPid(), ex)); 
        }
        processes.remove(currentProcess.getPid());
        currentProcess = null;
    }

    private void runNextProcess(){
        try {
            currentProcess = pickFromQueue();
            if(Debug.flag) {
                
                System.out.printf("!> Process PID%d started%n", currentProcess.getPid());
            }
            beheadQueue(currentProcess.getPriority());
            currentProcess.run();

        } catch (ProcessNotFoundException ex) {
            if(Debug.flag)
                System.out.printf("!> No processes to switch to, lets try next time \t realtimes:%s interactives:%s backgrounds:%s%n", realTimes, interactives, backgrounds);
        }
    }
    
    public void sleep(int milliseconds){
        double timeToFinish = Clock.systemUTC().millis() + milliseconds;
        
        KernelandProcess temp;
        synchronized (currentProcessLock) {
            temp = currentProcess;
            currentProcess = null;
            switchProcess();
        }
        
        sleepingProcesses.add(new SleepingProcess(temp, timeToFinish));
        
        if(Debug.flag)
            System.out.printf("\tProcess PID%d sleeping for %d milliseconds, %s%n", temp.getPid(), milliseconds, sleepingProcesses);
        temp.stop();
    }
    private void addToQueue(KernelandProcess process, boolean first){
        List<KernelandProcess> output;
        switch (process.getPriority()){
            case REALTIME -> {
                output = realTimes;
            }
            case INTERACTIVE -> {
                output = interactives;
            }
            case BACKGROUND -> {
                output = backgrounds;
            }
            default -> {
                throw new UnsupportedOperationException("Unknown priority");
            }
        }
        synchronized (output){
            if(first)
                output.add(0, process);
            else
                output.add(process);
            if(Debug.flag)
                System.out.printf("!> Process PID%d added to %s\t%s%n", process.getPid(), process.getPriority(), output);
        }
    }
    
    private void beheadQueue(Priority p){
        List<KernelandProcess> output;
        switch (p){
            case REALTIME -> {
                output = realTimes;
            }
            case INTERACTIVE -> {
                output = interactives;
            }
            case BACKGROUND -> {
                output = backgrounds;
            }
            default -> throw new UnsupportedOperationException("Unknown priority");
        }
        
        synchronized (output) {
            if(output.isEmpty())
                throw new ProcessNotFoundException("No processes to behead");
            output.remove(0);
            if(Debug.flag)
                System.out.printf("!> Process PID%d removed from %s\t%s%n", currentProcess.getPid(), currentProcess.getPriority(), output);
        }
    }
    
    private boolean processesRemaining(){
        boolean realTimesEmpty;
        boolean interactivesEmpty;
        boolean backgroundsEmpty;
        synchronized (realTimes){
            realTimesEmpty = realTimes.isEmpty();
        }
        synchronized (interactives){
            interactivesEmpty = interactives.isEmpty();
        }
        synchronized (backgrounds){
            backgroundsEmpty = backgrounds.isEmpty();
        }
        
        return !realTimesEmpty || !interactivesEmpty || !backgroundsEmpty;
    }
    
    private KernelandProcess pickFromQueue(){
        int total = REALTIME_WEIGHT + INTERACTIVE_WEIGHT + BACKGROUND_WEIGHT;
        int i = rand.nextInt(total);
        List<KernelandProcess> queue;
        
        if(i < REALTIME_WEIGHT && !realTimes.isEmpty())
            queue = realTimes;
        else if(i < REALTIME_WEIGHT + INTERACTIVE_WEIGHT && !interactives.isEmpty())
            queue = interactives;
        else if(!backgrounds.isEmpty())
            queue = backgrounds;
        else
            throw new ProcessNotFoundException("No processes to run");
        
        
        synchronized (queue){
            if (Debug.flag)
                System.out.printf("!> Picking %s from %s... %s%n", queue,queue == realTimes ? "realtime" : queue == interactives ? "interactive" : "background", queue);
            return queue.get(0);
        }
        
    }
    
    public void sendMessage(KernelMessage msg){
        int senderPid = getCurrentlyRunning().getPid();
        int receiverPid = msg.getReceiverPid();
        if(!processes.containsKey(receiverPid))
            throw new ProcessNotFoundException(String.format("No process with PID%d to receive message", receiverPid));
        
        KernelandProcess receiver = processes.get(receiverPid);
        receiver.receiveMessage(new KernelMessage(msg, senderPid));
        
        if(messageWaiters.containsKey(receiverPid)){
            messageWaiters.remove(receiverPid);
            addToQueue(receiver, true);
        }
    }
    
    public KernelMessage awaitMessage(){
        KernelandProcess temp;
        synchronized (currentProcessLock) {
            if (currentProcess.hasMessage()) {
                return currentProcess.takeMessage();
            }

            temp = currentProcess;
            currentProcess = null;
            switchProcess();
        }
        messageWaiters.put(temp.getPid(), temp);
        temp.stop();
        
        return temp.takeMessage();
    }
    
    public int getPid(){
        synchronized (currentProcessLock) {
            return currentProcess.getPid();
        }
    }
    public int getPidByName(String name){
        KernelandProcess sought;
        if((sought = getProcess(name)) == null)
            throw new ProcessNotFoundException(String.format("No process with name %s", name));
        else
            return sought.getPid();
    }
    
    private KernelandProcess getProcess(String name){
        for(Map.Entry<Integer, KernelandProcess> entry : processes.entrySet()){
            if(entry.getValue().getName().equals(name))
                return entry.getValue();
        }
        
        return null;
    }
    
    public KernelandProcess getCurrentlyRunning(){
        synchronized (currentProcessLock) {
            return currentProcess;
        }
    }
    
    public String reportCurrentProcess(){
        synchronized (currentProcessLock) {
            if (currentProcess == null)
                return "[No process running]";
            else
                return String.format("Process PID%d", currentProcess.getPid());
        }
    }
    
    public static class ProcessNotFoundException extends RuntimeException{
        public ProcessNotFoundException(String message){
            super(message);
        }
    }
}
