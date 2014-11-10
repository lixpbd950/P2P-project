import java.io.*;
import java.net.Socket;
import java.util.Properties;

/**
 * A server thread to handle the connection of one client
 */
public class ServerHandler extends Thread {

    // Client connection
    private final Socket socket;

    // Input of the connection
    private final DataInputStream input;

    // Output of the connection
    private final DataOutputStream output;

    // File to be distributed
    private final ChunkFile file;

    // Config of the program
    private final Properties config;

    // Client Id
    private int clientId;

    /**
     * Constructor
     *
     * @param config Config of the program
     * @param file   File to be distributed
     * @param socket Client connection
     */
    public ServerHandler(Properties config, ChunkFile file, Socket socket)
            throws IOException {
        this.config = config;
        this.file = file;
        this.socket = socket;
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Runs the thread
     */
    public void run() {
        try {
            // Get the total number of clients
            int totalClients = Integer.parseInt(config.getProperty("ClientNumber"));

            // Read client Id from the stream
            clientId = input.readInt();
            System.out.println("Client " + clientId + " is connected!");

            if (clientId < 1 || clientId > totalClients) {
                System.out.println("Error: Invalid client ID " + clientId);
                return;
            }

            // Calculates the number of chunks to send
            int num = file.getChunkNum() / totalClients;
            if (clientId == totalClients) {
                num += file.getChunkNum() % totalClients;
            }

            // Calculates id of the first chunk
            int startChunkId = (clientId - 1) * (file.getChunkNum() / totalClients) + 1;
            System.out.println("Client " + clientId + " will get " + num + " chunks");

            // Send [neighbourId, filename, size, totalChunks, sendChunks] to the client
            output.writeInt(clientId % totalClients + 1);
            output.writeUTF(file.getFilename());
            output.writeLong(file.getFileSize());
            output.writeInt(file.getChunkNum());
            output.writeInt(num);
            output.flush();

            // Send [num] chunks starting from [startChunkId]
            for (int i = startChunkId; i < startChunkId + num; i++) {
                // Send the i-th chunk to the client
                sendChunk(i);
                // Keep waiting until the client is ready
                input.readUTF();
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Close the connection
        closeConnection();
    }

    /**
     * Sends a chunk to the client
     *
     * @param chunkId the id of chunk
     */
    private void sendChunk(int chunkId) {
        // Get the size of each chunk
        int chunkSize = Integer.parseInt(config.getProperty("ChunkSize"));
        byte[] bytes = new byte[chunkSize];

        // Get the chunk directory
        String chunkDir = config.getProperty("ChunkDir");
        File inputFile = new File(chunkDir + chunkId);
        FileInputStream input = null;

        try {
            // 1. Send the chunk Id
            output.writeInt(chunkId);

            // Read the bytes of chunks from the file
            input = new FileInputStream(inputFile);
            int length = input.read(bytes);

            // 2. Send the number of bytes
            output.writeInt(length);

            // 3. Send the bytes
            output.write(bytes, 0, length);
            output.flush();
            System.out.println("Send " + chunkId + " to Client " + clientId);

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());

        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Close the connection
     */
    private void closeConnection() {
        try {
            input.close();
        } catch (IOException e) {
        }
        try {
            output.close();
        } catch (IOException e) {
        }
        try {
            socket.close();
        } catch (IOException e) {
        }
    }
}
