import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

/**
 * Client class
 */
public class Client {

    // A bitmap of chunks status ('1' for owned chunk, '0' for missed chunk)
    private char[] chunkStatus;

    // Config of the program
    private Properties config;

    // Id of the client
    private int clientId;

    // Id of the neighbour client
    private int neighbourId;

    // Downloaded file
    private ChunkFile file;

    /**
     * Constructor
     */
    public Client() throws IOException {
        // Load config from the file
        config = new Properties();
        config.load(new FileInputStream("config.properties"));

        // Get client Id from the config
        clientId = Integer.parseInt(config.getProperty("ClientId"));

        // Download the initial chunks from the server
        downloadFromServer();
        // Listen to other clients to upload chunks
        startListener();
        // Down load chunks from the neighbour client
        downloadFromNeighbour();
        // After all the chunks received, assemble them into a file
        assembleChunks();
    }

    /**
     * Listens to other clients to upload chunks
     */
    private void startListener() throws IOException {
        // Start a new thread to listen
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get port from the server
                    final int port = Integer.parseInt(config.getProperty("ClientPort-" + clientId));

                    // Create a server socket to listen
                    final ServerSocket server = new ServerSocket(port);
                    System.out.println("I'm listening on " + port);

                    // If a client node connect, start client listener thread
                    Socket socket = server.accept();
                    new ClientListener(config, file, chunkStatus, socket).start();
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * @return true if the client owns all the chunks
     *         false otherwise
     */
    private boolean finished() {
        for (int i = 1; i <= file.getChunkNum(); i++) {
            if (chunkStatus[i] == '0') {
                return false;
            }
        }
        return true;
    }

    /**
     * Downloads the initial chunks from the server
     */
    private void downloadFromServer() throws IOException {
        Socket socket = null;
        try {
            // Get server IP and port from the config
            String serverIP = config.getProperty("ServerIP");
            int serverPort = Integer.parseInt(config.getProperty("ServerPort"));

            // Connect to the server
            socket = new Socket(serverIP, serverPort);

        } catch (IOException e) {
            // Exit if the server is not available
            System.out.println("Error: Connect to server refused.");
            System.exit(0);
        }
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());

        // Send the client ID
        output.writeInt(clientId);
        output.flush();

        // Read neighbourId, filename, file size, totalChunks, sendChunks
        neighbourId = input.readInt();
        String filename = input.readUTF();
        long size = input.readLong();
        int chunkNum = input.readInt();
        file = new ChunkFile(chunkNum, size, filename);
        chunkStatus = new char[chunkNum + 1];
        for (int i = 0; i <= chunkNum; i++) {
            chunkStatus[i] = '0';
        }
        int n = input.readInt();
        System.out.println("The file name is " + filename);
        System.out.println("The file size is " + size);
        System.out.println("The total chunk number is " + chunkNum);
        System.out.println("I will receive " + n + " chunks from server");

        // Read n chunks from the server
        for (int i = 0; i < n; i++) {
            // Read chunkId
            int chunkId = input.readInt();
            // Save the chunk to the file system
            saveChunk(input, chunkId);
            System.out.println("Received chunk " + chunkId + " from server");
            // Tell the server I'm ready
            output.writeUTF("OK");
            output.flush();
        }
        // Close the connection
        input.close();
        output.close();
        socket.close();
        System.out.println("Disconnect with server !");
    }

    /**
     * Saves the chunk to the file system
     *
     * @param input   input stream  of the connection
     * @param chunkId id of the chunk
     */
    private void saveChunk(DataInputStream input, int chunkId) throws IOException {
        // Get chunk size and chunk directory from the config
        int chunkSize = Integer.parseInt(config.getProperty("ChunkSize"));
        String chunkDir = config.getProperty("ChunkDir");
        byte[] bytes = new byte[chunkSize];

        // Make directory
        File directory = new File(config.getProperty("ChunkDir"));
        if (!directory.exists()) {
            directory.mkdir();
        }

        File inputFile = new File(chunkDir + chunkId);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(inputFile);

            // Read bytes from the input stream and write to the file
            int remainLength = input.readInt();
            do {
                int length = input.read(bytes, 0, chunkSize);
                remainLength -= length;
                output.write(bytes, 0, length);
            } while (remainLength > 0);

            // Set the status
            chunkStatus[chunkId] = '1';

        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /**
     * Downloads chunks from the neighbour client
     */
    private void downloadFromNeighbour() throws IOException {

        // Get IP and port of the neighbour client from the config
        String neighbourIP = config.getProperty("ClientIP-" + neighbourId);
        int neighbourPort = Integer.parseInt(config.getProperty("ClientPort-" + neighbourId));

        // Try to connect to the neighbour
        Socket socket = null;
        while (socket == null) {
            try {
                socket = new Socket(neighbourIP, neighbourPort);
            } catch (IOException e) {
                System.out.println("connect to " + neighbourIP + ":" + neighbourPort
                        + " is refused, retry after 5 seconds");
            }
            sleep(5000);
        }
        System.out.println("Connected with client " + neighbourId);

        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());

        // While not finished
        while (!finished()) {
            // Send the chunk status to the neighbour
            output.writeUTF(new String(chunkStatus));
            output.flush();

            // Read a chunk Id that I do not own but the neighbour owns
            int chunkId = input.readInt();
            if (chunkId >= 1 && chunkId <= file.getChunkNum()) {
                // Save the chunk to the file system
                saveChunk(input, chunkId);
                System.out.println("Received chunk " + chunkId + " from client " + neighbourId);
            }
            sleep(100);
        }

        // Close the connection
        input.close();
        output.close();
        socket.close();
        System.out.println("I am done");
    }

    /**
     * Assembles all the chunks into a file
     */
    private void assembleChunks() throws IOException {
        // Get chunk directory, file directory and chunk size
        final String chunkDir = config.getProperty("ChunkDir");
        final String fileDir = config.getProperty("FileDir");
        final int chunkSize = Integer.parseInt(config.getProperty("ChunkSize"));

        // Make file diretory
        File directory = new File(fileDir);
        if (!directory.exists()) {
            directory.mkdir();
        }

        FileOutputStream output = null;
        FileInputStream input = null;
        byte[] bytes = new byte[chunkSize];
        System.out.println("Assembling chunks");
        try {
            File outputFile = new File(fileDir + file.getFilename());
            output = new FileOutputStream(outputFile);

            // Copy all the chunks to the output file
            for (int i = 1; i <= file.getChunkNum(); i++) {
                try {
                    input = new FileInputStream(chunkDir + i);
                    int length = input.read(bytes);
                    output.write(bytes, 0, length);

                } finally {
                    if (input != null) {
                        input.close();
                    }
                }
                System.out.print(".");
            }
            System.out.println("Assembling file finished!");

        } finally {
            if (output != null) {
                output.close();
            }
        }
    }


    /**
     * Sleeps for a specified milliseconds
     */
    private void sleep(long millisecond) {
        try {
            Thread.sleep(millisecond);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            new Client();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error: " + e.getMessage());
        }
    }

}
