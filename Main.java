public class Main {
    public static void main(String[] args){
        OS.startup(new Ping());
        OS.createProcess(new Pong());
        OS.createProcess(new RandomNumber());
        OS.createProcess(new FileTester());
    }
}
