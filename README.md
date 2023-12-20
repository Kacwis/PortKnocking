## 1. Introduction
The project implements authorization using the "UDP port knocking" method, followed by establishing a TCP connection with the client.

## 2. Architecture

### Server:
1. **Start**
   - The server is launched by calling the `main()` method, which accepts a list of ports the server should listen on. For each specified port, an instance of the `ServerPort` class is created, responsible for handling that port.
   
2. **Authorization**
   - The server's logic is responsible for relaying packets from `ServerPort` directly to the server class to check their validity. `ServerPort` objects will forward received UDP packets to respective collections, while the server thread will retrieve these packets from `ServerPort` objects. When the count of received UDP packets matches the count of open ports for UDP packets, it triggers a thread to check their validity. If the packets form a sequence of consecutive alphabet letters (the initial letter is always random), this process sends a randomly generated port back to the client via TCP connection to the originating IP address and a predetermined port set by the server. If the sequence doesn't match, the validity-checking thread ends its operation.
   
3. **Opening TCP Connection**
   - After successful authorization, the sequence validation process opens the port sent via UDP to the client, waiting for a TCP connection. Once the connection is established, the TCP connection thread sends a packet containing the word "REQUEST" to the client. Upon receiving a packet with the content "REPLY" from the client, the process concludes.

### Implementation Details:
- The server can handle an arbitrary number of clients simultaneously.
- The sequence validity-checking thread operates cyclically in a loop.
- At server application startup, it verifies the arguments used for its launch. If the number of ports is incorrect, the program will notify and terminate. Additionally, if the port data type provided in the arguments doesn't match, the program will end with an appropriate message.

### Client:
1. **Start**
   - Launch the client thread by calling the `main()` method in the `UDPClient` class. It takes the client's IP, to which it sends UDP packets for authorization, as its first argument.

2. **Sending Packets**
   - Upon startup, the program initiates a thread responsible for sending packets to the specified IP addresses provided as arguments. The packets contain consecutive letters of the alphabet, starting with a randomly chosen letter. After sending a packet, a thread begins a countdown of 30 seconds. If, within this time, the client thread doesn't receive a packet with a port number, it terminates. Simultaneously, the main thread waits for a port packet from the server. If received, it initiates a TCP connection to the specified port on the server. Upon receiving a "REQUEST" packet from the server, it sends a packet containing "REPLY" and successfully terminates its operation.

3. **TCP Connection**
   - Similar to the packet sending thread, the main thread waits for a port packet from the server. If received, it establishes a TCP connection to the specified port on the server. Upon receiving a "REQUEST" packet from the server, it sends a packet containing "REPLY" and successfully terminates its operation.

### Implementation Details:
- At client program startup, it verifies the arguments used for its launch. If there is no provided IP or port, the program prints an appropriate sentence and terminates. If the IP is in an incorrect format, it also prints a corresponding message and terminates.

## 3. TESTING
- To test the server's capability to handle a single client, I initiated the server and a client process, observing the messages in the console.
- To verify if packets arrive from multiple clients, I ran the server and two clients on different computers, observing the information printed in the server's console.
- To ensure packets were stored in the correct collection locations, I initiated the server and two clients, observing the contents printed by the collections on the console.
- To test authorization for multiple clients, I started the server and two clients, monitoring the console for a packet containing "REPLY" in each client.
