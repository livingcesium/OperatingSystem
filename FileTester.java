import java.io.*;
import java.time.LocalDateTime;
import java.util.Arrays;

public class FileTester extends UserlandProcess{
    private final byte[] data = new byte[4];
    
    public void run(){
        String[] names = {
                "test1.t",
                "test2.e",
                "test3.s",
                "test4.t",
                "test5.s",
        };
        final int FILES_TO_CREATE = names.length < Byte.MAX_VALUE ? names.length : Byte.MAX_VALUE; // If dev puts a ridiculous number, we'll just use the max byte value and watch the program break that way.
        int[] fds = new int[FILES_TO_CREATE];
        
        for (int j = 0; j < FILES_TO_CREATE; j++) {
            try {
                fds[j] = OS.open("file " + names[j]);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        
        int currentFd;
        byte i = 0; // This one loops. I'd put an int here, but a byte is enough. Plus it makes the next part elegant
        byte[] expectedData = new byte[4];
        
        boolean writeMode = true;
        while(true) {
            currentFd = fds[i];
            Arrays.fill(expectedData, i); // This is so we can verify things were written in the right order
                try {
                    if (writeMode) {
                        OS.write(currentFd, expectedData.clone());
                        OS.seek(currentFd, 0); // Go back to beginning of file
                    } else {
                        synchronized (data) {
                            System.arraycopy(OS.read(currentFd, 4), 0, data, 0, data.length); // Copy read data into data array
                            if (Arrays.equals(expectedData, data)) {
                                System.out.printf("Read data correctly from file %s! \n", names[i]); // I would print the data here, but it often displays the wrong data cause of race conditions. Not worth the stalling. 
                            } else {
                                System.out.printf("Incorrect data in file %s: expected %s got %s (Array may print incorrectly due to race condition)\n", names[i], Arrays.toString(expectedData), Arrays.toString(data));
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            if (i == FILES_TO_CREATE - 1) 
                writeMode = !writeMode; // If we just wrote to all files, we need to read from them. Vice versa. Ad infinitum
            else if (i == (FILES_TO_CREATE - 1) / 2) { // At the midpoint (chosen arbitrarily) close and reopen file.
                try {
                    OS.close(currentFd);
                    fds[i] = OS.open("file " + names[i]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                log(names[i]);
                OS.sleep(500);
            } catch (Exception ignored) {}

            i = (byte) ((i + 1) % (FILES_TO_CREATE)); // Byte convert should be safe.
        }
    }
    
    private void log(String name) throws IOException {
        String path = String.format("./FakeFsLogs/FakeFile[%s].log", name);
        File logFile = new File(path);
        logFile.getParentFile().mkdirs(); // Make dir if needed
        
        RandomAccessFile file = new RandomAccessFile(logFile, "rw");
        
        file.setLength(0); // Clear file
        file.write(String.format("///////////////// %s /////////////////\n", LocalDateTime.now()).getBytes());
        file.write(OS.getFileSystemLog().getBytes());
        file.close();
    }
}
