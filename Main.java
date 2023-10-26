public class Main {
    public static void main(String[] args){
        OS.startup(new RandomNumber());
        OS.createProcess(new FileTester());
    }
}
