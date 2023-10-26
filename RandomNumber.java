import java.io.*;
import java.time.LocalDateTime;

public class RandomNumber extends UserlandProcess{
    public void run(){
        final int DEVICES_TO_CREATE = 5;
        int[] fds = new int[DEVICES_TO_CREATE];
        for (int j = 0; j < DEVICES_TO_CREATE; j++) {
            try {
                fds[j] = OS.open("random");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        
        int num;
        int currentFd;
        int i = 0;
        while(true){;
            currentFd = fds[i];
            try {
                num = OS.read(currentFd, 1)[0]; // casting byte -> int
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Generated number: " + num);
            
            try {
                log(num);
                OS.sleep(500);
            } catch (Exception ignored) { }

            
            i = (i + 1) % DEVICES_TO_CREATE;

        }
    }
    
    private void log(int num) throws IOException {
        String path = String.format("./RandLogs/RandomNumber[%d].log", num);
        File logFile = new File(path);
        logFile.getParentFile().mkdirs(); // Make dir if needed
        
        RandomAccessFile file = new RandomAccessFile(logFile, "rw");
        
        file.seek(file.length()); // Go to end of file to append
        file.write(String.format("///////////////// %s /////////////////\n", LocalDateTime.now()).getBytes());
        file.write(OS.getFileSystemLog().getBytes());
        file.close();
        
    }
}
