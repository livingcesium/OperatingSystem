import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class FakeFileSystem implements Device{
    public static final int MAXDEVICES = 10;

    private static final RandomAccessFile[] files = new RandomAccessFile[MAXDEVICES];
    @Override
    public int open(String s) throws FileNotFoundException {
        if(s == null || s.equals(""))
            throw new RuntimeException("Expected filename");

        // put into first encountered empty space

        for (int i = 0; i < files.length; i++) {
            if(files[i] == null) {
                files[i] = new RandomAccessFile(s, "rw"); // Open or create new file
                return i;
            }
        }
        
        return -1; // failed to open
    }

    @Override
    public void close(int id) throws IOException {
        RandomAccessFile file;
        if(id >= 0 && id < MAXDEVICES && (file = files[id]) != null) {
            file.close();
            files[id] = null;
        }
    }

    @Override
    public byte[] read(int id, int size) throws IOException {
        
        
        byte[] output = new byte[size];
        RandomAccessFile file;
        if(id >= 0 && id < MAXDEVICES && (file = files[id]) != null){
            int fdErr;
            if((fdErr = file.read(output)) < 0)
                return new byte[0]; // Not exactly sure what to do, but hopefully this signifies EOF well enough for now
            return output;
        } else
            return new byte[0];
    }

    @Override
    public void seek(int id, int to) throws IOException {
        RandomAccessFile file;
        if(id >= 0 && id < MAXDEVICES && (file = files[id]) != null)
            file.seek(to);
    }

    @Override
    public int write(int id, byte[] data) throws IOException {
        RandomAccessFile file;
        if(id >= 0 && id < MAXDEVICES && (file = files[id]) != null) {
            file.write(data);
            return data.length;
        } else 
            return 0;
    }
}
