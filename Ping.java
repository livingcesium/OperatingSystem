public class Ping extends UserlandProcess{

    @Override
    public void run() {
        KernelMessage msg;
        byte[] data = {1,2,3};
        int what = 0;
        System.out.printf("This is Ping%02d, coming at you!\n", OS.getPid());
        int pongPid = -1;
        
        // Get pong, in case it's sleeping give it a chance to wake up
        int attempts = 10;
        while(attempts-- > 0 || pongPid == -1)
            try {
                pongPid = OS.getPidByName("Pong");
                OS.sleep(5);
            }catch (Scheduler.ProcessNotFoundException ignored) {
                System.out.println("Pong, where are you? :(");
                pongPid = -1;
            }
        
        if (pongPid < 0)
            throw new RuntimeException("Pong not found");
        
        while(true){
            OS.sendMessage(new KernelMessage(pongPid, what++, data));
            
            msg = OS.awaitMessage();
            System.out.printf("\tPing%02d received: {%s}\n", OS.getPid(), msg);
            if(msg.getWhat() != what - 1)
                System.out.printf("Hey Pong, what the hell is this? (%d vs %d)\n", msg.getWhat(), what - 1);

            OS.sleep(50);
        }
    }
}
