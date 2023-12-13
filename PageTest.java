import java.util.Random;
public class PageTest extends UserlandProcess{
    public int hunger = 100; // in pages
    public static int ate = 0;
    @Override
    public void run() {
        int sought = new Random().nextInt(100);
        OS.allocateMemory(OS.pageSize * hunger); // Allocate pages
        ate += OS.pageSize * hunger;
        System.out.printf("We've eaten %d bytes so far\n", ate);
        
        for(int offset = 0; offset < hunger * OS.pageSize; offset += OS.pageSize){
            this.write(4 + offset, (byte) sought); // Write a unique number to the fourth byte of every page
        }
        byte b;
        for(int offset = 0; offset < hunger * OS.pageSize; offset += OS.pageSize) {
            b = this.read(4 + offset); // Read the fourth byte of every page
            System.out.printf("\tMemory access %s. %d bytes passed during this operation.\n", b == sought ? "successful" : "failed", offset);
        }

        System.out.println("Yum! Going for seconds..."); // Should start using the page file
        
        OS.sleep(100);
        OS.allocateMemory(OS.pageSize * hunger); // Allocate more pages
        
        for(int offset = 0; offset < hunger * OS.pageSize; offset += OS.pageSize){
            this.write(4 + offset, (byte) sought);
        }
        
        for(int offset = 0; offset < hunger * OS.pageSize; offset += OS.pageSize) {
            b = this.read(4 + offset); // Read the fourth byte of every page
            System.out.printf("\tMemory access %s. %d bytes passed during this operation.\n", b == sought ? "successful" : "failed", offset);
        }

        System.out.printf("All done! Thanks a ton... wait how much memory did you say you had again? (%d bytes eaten total)\n", ate);
        
    }
}
