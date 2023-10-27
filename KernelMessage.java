import java.util.Arrays;

public class KernelMessage {
    private int senderPid;
    private int receiverPid;
    private int what; // What this message is
    private byte[] data;
    
    public KernelMessage(KernelMessage msg){
        this.senderPid = msg.senderPid;
        this.receiverPid = msg.receiverPid;
        this.what = msg.what;
        this.data = msg.data;
    }

    public KernelMessage(KernelMessage msg, int senderPid){ // Secure copy constructor
        this.senderPid = senderPid;
        this.receiverPid = msg.receiverPid;
        this.what = msg.what;
        this.data = msg.data;
    }

    public KernelMessage(int receiverPid, int what, byte[] data){ // Untrusted process constructor
        this.senderPid = -1;
        this.receiverPid = receiverPid;
        this.what = what;
        this.data = data;
    }
    
    public KernelMessage(int senderPid, int receiverPid, int what, byte[] data){
        this.senderPid = senderPid;
        this.receiverPid = receiverPid;
        this.what = what;
        this.data = data;
    }
    
    public int getWhat(){
        return what;
    }
    public byte[] getData(){
        return data;
    }
    
    public int getSenderPid(){
        return senderPid;
    }
    public int getReceiverPid(){
        return receiverPid;
    }
    public String toString(){
        return String.format("Sender: %d, Receiver: %d, What: %d, Data: %s", senderPid, receiverPid, what, Arrays.toString(data));
    }
    
    public static KernelMessage empty(){
        return new KernelMessage(-1, -1, -1, new byte[0]);
    }
}
