import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

/**
 * Server class
 */
public class Server {

    // Config of the program
    private final Properties config;

    // File to be distributed
    private final ChunkFile file;

    // Server connection
    private final ServerSocket server;

    // Number connected clients
    private int connectedClients;

    /**
     * Constructor
     *
     * @param config   Config of the program
     * @param filename Name of the file
     * @param fileSize Size of the file
     * @param chunkNum Number of chunks
     */
    public Server(Properties config, String filename, long fileSize, int chunkNum)
            throws IOException {

        this.config = config;
        this.file = new ChunkFile(chunkNum, fileSize, filename);
        this.connectedClients = 0;

        // Get server port from the config
        int port = Integer.parseInt(config.getProperty("ServerPort"));
        // Start the server socket
        this.server = new ServerSocket(port);

        // Listen to the clients
        startListening();
    }

    /**
     * Listens to the clients
     */
    public void startListening() {

        // Get the total number of clients
        int clients = Integer.parseInt(config.getProperty("ClientNumber"));

        // If all the clients connected, exit the loop
        while (connectedClients < clients) {
            System.out.println("Waiting for connection...");

            try {
                // If a client is connected, start a handle thread
                Socket socket = server.accept();
                new ServerHandler(config, file, socket).start();
                connectedClients++;

            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("The file is running.");
        Scanner input = new Scanner(System.in);
        try {
            // Load config
            Properties config = new Properties();
            config.load(new FileInputStream("config.properties"));

            int chunkSize = Integer.parseInt(config.getProperty("ChunkSize"));
            String chunkDir = config.getProperty("ChunkDir");

            // Enter the filename
            System.out.println("Enter the filename:");
            String filename = input.next();

            // Split the file into chunks
            File file = new File(filename);
            int n = SplitFiles.split(chunkSize, chunkDir, file);

            // Start the server
            new Server(config, file.getName(), file.length(), n);

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
