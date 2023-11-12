public class Main {
    public static void main(String[] args){
        OS.startup(new MemoryTest());
        OS.createProcess(new Ping());
        OS.createProcess(new Pong());
        OS.createProcess(new RandomNumber());
        OS.createProcess(new FileTester());
    }
}
