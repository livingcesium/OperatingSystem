public class Main {
    public static void main(String[] args){
        System.out.println("Starting, testing memory (expect segfault)");
        OS.startup(new MemoryTest());
        
        // Defaults to page testing, can choose to deviceTest
        
        if(args.length > 0 && args[0].equals("-deviceTest")) {
            OS.createProcess(new Ping());
            OS.createProcess(new Pong());
            OS.createProcess(new RandomNumber());
            OS.createProcess(new FileTester());
        } else for (int i = 0; i < 20; i++)
            OS.createProcess(new PageTest());
    }
}
