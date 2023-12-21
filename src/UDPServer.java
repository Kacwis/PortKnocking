import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class UDPServer extends Thread {


    private class ServerPort extends Thread{

        private DatagramSocket socket;
        private DatagramPacket packet;
        Map<String, DatagramPacket> mapOfPackets;
        InetAddress address;
        int port;
        String received;
        boolean running = true;

        public ServerPort(int port){
            super();
            mapOfPackets = new HashMap<>();
            try {
                socket = new DatagramSocket(port);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buff = new byte[445];
            while(running) {
                try {
                    packet = new DatagramPacket(buff, buff.length);
                    socket.receive(packet);
                    address = packet.getAddress();
                    port = packet.getPort();
                    packet = new DatagramPacket(buff, buff.length, address, port);
                    addingNewPacket(packet);
                    received = new String(packet.getData(), 0, packet.getLength());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void addingNewPacket(DatagramPacket packet){
            if(mapOfPackets.containsKey(packet.getAddress()))
                mapOfPackets.replace(packet.getAddress().toString(), packet);
            mapOfPackets.put(packet.getAddress().toString(), packet);
        }

        public void close(){
            socket.close();
        }

    }

    public static final int portToTCPConnection = 10002;

    List<ServerPort> portsList;
    List<Integer> portsNumbers;
    Map<String, List<String>> map;

    Thread readingPackets = new Thread(new Runnable() {
        @Override
        public void run() {
            boolean running = true;
            Map<String, DatagramPacket> currentMap;
            while(running){
                for(ServerPort sp : portsList){
                    currentMap = new HashMap<>(sp.mapOfPackets);
                    for(String address : currentMap.keySet()) {
                        addingNewPacket(address, currentMap);
                    }
                }
                for(String address : map.keySet()){
                    if(map.get(address).size() == portsList.size()){
                        checkingPackets(portsList.get(0).mapOfPackets.get(address).getAddress(), map.get(address));
                        for(ServerPort sp : portsList){
                            sp.mapOfPackets.remove(address);
                        }
                        map.remove(address);
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    public UDPServer(String[] portsString){
        super();
        portsList = new LinkedList<>();
        portsNumbers = new LinkedList<>();
        map = new HashMap<>();
        try {
            for (String s : portsString) portsNumbers.add(Integer.parseInt(s));
        }
        catch (NumberFormatException e){
            System.out.println("WRONG TYPE OF DATA");
            System.exit(-1);
        }
    }

    @Override
    public void run() {
        for(int port : portsNumbers){
            portsList.add(new ServerPort(port));
            portsList.get(portsList.size() - 1).start();
        }
        readingPackets.start();
    }


    private void sendingIPPacket(InetAddress address, int portTCP){
        byte[] buff = Integer.toString(portTCP).getBytes();
        DatagramPacket packet;
        DatagramSocket socket;
        try {
            packet = new DatagramPacket(buff, buff.length, address, portToTCPConnection);
            socket = new DatagramSocket();
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkingPackets(InetAddress address, List<String> packetSet){
        int sum = 0;
        for(String s : packetSet){
            sum += sumOfChars(s);
        }
        if(sum == gettingCorrectSum(packetSet)){
            int tcpPort = generateRandomPort();
            sendingIPPacket(address , tcpPort);
            startingTCPServer(tcpPort);
        }
    }

    private int sumOfChars(String s){
        int sum = 0;
        for(char c : s.toCharArray()){
            sum += c;
        }
        return sum;
    }

    private int gettingCorrectSum(List<String> packetList){
        int min = sumOfChars(packetList.get(0));
        for(String s : packetList){
            int currentSum = sumOfChars(s);
            if(currentSum < min)
                min = currentSum;
        }
        int sum = 0;
        int currentPacket = min;
        for (int i = 0; i < portsList.size(); i++) {
            sum += currentPacket;
            currentPacket++;
        }
        return sum;
    }

    private void addingNewPacket(String address, Map<String, DatagramPacket> currentMap ){
        byte[] currentData = currentMap.get(address).getData();
        if(map.containsKey(address)){
            map.get(address).add(new String(currentData, 0, currentMap.get(address).getLength()));
            return;
        }
        map.put(address, new ArrayList<>());
        map.get(address).add(new String(currentData, 0 , currentMap.get(address).getLength()));

    }

    private void startingTCPServer(int port){
        Thread tcpConnection = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(port);
                    boolean running = true;
                    while(running){
                        Socket socket = serverSocket.accept();
                        OutputStream output = socket.getOutputStream();
                        PrintWriter writer = new PrintWriter(output, true);
                        InputStream input = socket.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                        String received = reader.readLine();
                        System.out.println(received + " " + socket.getInetAddress().toString());
                        writer.println("REQUEST");
                        if(received.equals("REPLY")){
                            running = false;
                            socket.close();
                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        tcpConnection.start();
    }

    public void close(){
        for(ServerPort sp : portsList) sp.close();
    }

    private int generateRandomPort(){
        int min = 1024;
        int max = 10000;
        int generatedPort = ThreadLocalRandom.current().nextInt(min, max + 1);
        for(ServerPort sp : portsList){
            if(sp.port == generatedPort){
                generatedPort = generateRandomPort();
            }
        }
        return generatedPort;
    }

    public static void main(String[] args) {
        if(args.length < 1)
            return;
        for(String s : args) System.out.println(s);
        UDPServer server = new UDPServer(args);
        server.start();
        server.close();

    }
}
