import java.util.Random;

public class RandomDevice implements Device{
    public static final int MAXDEVICES = 10;
    
    private static final Random[] randoms = new Random[MAXDEVICES];
    
    @Override
    public int open(String s) { 
        Random random;
        if (s == null || s.equals(""))
            random = new Random();
        else random = new Random(Integer.parseInt(s));
        
        // put in first encountered empty space
        
        for (int i = 0; i < randoms.length; i++) {
            if(randoms[i] == null) {
                randoms[i] = random;
                return i;
            }
        }
        return -1; // if a space couldn't be opened
    }
    
    @Override
    public void close(int id) {
        if(id >= 0 && id < MAXDEVICES)
            randoms[id] = null;
    }

    @Override
    public byte[] read(int id, int size) {
        
        byte[] output = new byte[size];
        
        if(id < 0 || id >= MAXDEVICES || randoms[id] == null)
            return new byte[0];
        
        randoms[id].nextBytes(output);
        
        return output;
    }

    @Override
    public void seek(int id, int to) {
        if(id >= 0 && id < MAXDEVICES && randoms[id] != null)
            randoms[id].nextBytes(new byte[to]);
    }

    @Override
    public int write(int id, byte[] data) {
        return 0;
    }
}
