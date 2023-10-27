public class Pong extends UserlandProcess{

    @Override
    public void run() {
        KernelMessage msg;
        byte[] data = {3,2,1};
        System.out.printf("This is Pong%02d, ready?\n", OS.getPid());
        int pingPid = -1;

        // Get ping, in case it's sleeping give it a chance to wake up
        int attempts = 10;
        while(attempts-- > 0 || pingPid == -1)
            try {
                pingPid = OS.getPidByName("Ping");
                OS.sleep(5);
            }catch (Scheduler.ProcessNotFoundException ignored) {
                System.out.println("You're always sleeping in, Ping! >:(");
                pingPid = -1;
            }

        if (pingPid < 0)
            throw new RuntimeException("Pong not found");


        while(true){
            msg = OS.awaitMessage();
            System.out.printf("\tPong%02d received: {%s}\n", OS.getPid(), msg);
            
            OS.sleep(50);
            OS.sendMessage(new KernelMessage(pingPid, msg.getWhat(), data));
        }
    }
}
