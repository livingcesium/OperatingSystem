public class GoodbyeWorld extends UserlandProcess{
    public void run(){
        int i = 0;
        while(true){
            System.out.println("Goodbye World " + i++ );

            try {
                OS.sleep(50);
            } catch (Exception ignored) { }

        }
    }
}
