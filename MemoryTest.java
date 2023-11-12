public class MemoryTest extends UserlandProcess{
    @Override
    public void run() {
        OS.allocateMemory(OS.pageSize); // Allocate 1 page
        this.write(3, (byte) 1); // Write a 1 to the third byte of the page
        byte b = this.read(3); // Read the third byte of the page
        System.out.printf("Memory access %s\n", b == 1 ? "successful" : "failed");

        OS.freeMemory(0, OS.pageSize); // Free the page
        System.out.println(this.read(3));; // Should segfault
        System.out.println("I survived a segfault! I won't forget this...");
    }
}
