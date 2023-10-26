public class HelloWorld extends UserlandProcess{
    public void run(){
        int i = 0;
        while(true){
            System.out.println("Hello World " + i++);
            
            try {
                OS.sleep(50);
            } catch (Exception ignored) { }

        }
    }
}
