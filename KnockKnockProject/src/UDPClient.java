import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class UDPClient extends Thread {

    public static final int portToTCP = 10002;
    private static final int timeToTimeout = 30 * 1000;

    private DatagramSocket socket;
    DatagramSocket socketForTCPPort = null;
    private InetAddress address;
    private byte[] buf = new byte[455];
    private List<Integer> portsList;
    String received = "";
    boolean running = true;
    boolean runningTimeoutThread = true;


    Thread waitingForTimeout = new Thread(new Runnable() {
        @Override
        public void run() {
            int i = 0;
            while(runningTimeoutThread){
                try {
                    Thread.sleep(1000);
                    i++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(i == 30){
                    System.out.println("NO RECEIVED");
                    runningTimeoutThread = false;
                    running = false;
                    continue;
                }
            }
        }
    });


    public UDPClient(String[] args) {
        portsList = new ArrayList<>();
        List<String> currentTemp = new ArrayList<>();
        for(String s : args) currentTemp.add(s);
        try {
            address = InetAddress.getByName(currentTemp.get(0));
            currentTemp.remove(0);
            for(String s : currentTemp) {
                portsList.add(Integer.parseInt(s));
            }
            socket = new DatagramSocket();
            address = InetAddress.getByName(args[0]);
        }
        catch (UnknownHostException e){
            System.out.println("WRONG IP ADDRESS");
            System.exit(-1);
        }
        catch (NumberFormatException e){
            System.out.println("WRONG TYPE OF DATA");
            System.exit(-1);
        }
        catch (SocketException e){
            e.printStackTrace();
        }
    }

    private void checkingForPortTCP(DatagramPacket packet){
        try{
            socketForTCPPort = new DatagramSocket(portToTCP);
            socketForTCPPort.setSoTimeout(timeToTimeout);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        while(running){
            try {
                socketForTCPPort.receive(packet);
                socket.close();
                received = new String(packet.getData(), 0, packet.getLength());
                System.out.println(received);
                if(received != null)
                    running = false;

            } catch (Exception e) {
                running = false;
                continue;
            }
            if(received != null){
                startingTCPConnection(Integer.parseInt(received));
                running = false;
                continue;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void run() {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        char c = generateLetter();
        String temp;
        for(int port : portsList){
            temp = Character.toString(c);
            sendPacket(temp, port);
            c++;
        }
        waitingForTimeout.start();
        checkingForPortTCP(packet);

    }

    public void sendPacket(String data, int port ){
        buf = data.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startingTCPConnection(int tcpPort){
        try{
            Socket socket = new Socket(address, tcpPort);
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println("REPLY");
            String received = reader.readLine();
            if(received.equals("REQUEST")){
                running = false;
                socket.close();
                runningTimeoutThread = false;
            }
            System.out.println(received);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static char generateLetter(){
        int max = 90;
        int min = 65;
        return (char) ThreadLocalRandom.current().nextInt(min, max + 1);
    }


    public static void main(String[] args) {
        if(args.length < 2)
            return;
        UDPClient client = new UDPClient(args);
        client.run();
    }
}