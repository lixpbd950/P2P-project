import java.io.*;
import java.net.Socket;
import java.util.Properties;

/**
 * The thread to send chunks to the neighbour clients
 */
public class ClientListener extends Thread {

    // Downloaded file
    private final ChunkFile file;

    // Bitmap of chunks status
    private final char[] chunkStatus;

    // Neighbour connection
    private final Socket socket;

    // Input of the connection
    private final DataInputStream input;

    // Output of the connection
    private final DataOutputStream output;

    // Config of the program
    private final Properties config;

    /**
     * Constructor
     *
     * @param config      Config of the program
     * @param file        Downloaded file
     * @param chunkStatus Bitmap of chunks status
     * @param socket      Neighbour connection
     */
    public ClientListener(Properties config, ChunkFile file, char[] chunkStatus, Socket socket)
            throws IOException {
        this.config = config;
        this.file = file;
        this.chunkStatus = chunkStatus;
        this.socket = socket;
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Selects a chunk I owns and the neighbour does not own
     *
     * @param status chunk status of the neighbour
     * @return Id of the selected chunk
     *         or -1 if not found
     */
    private int selectChunk(String status) {
        for (int i = 1; i <= file.getChunkNum(); i++) {
            if (status.charAt(i) == '0' && chunkStatus[i] == '1') {
                return i;
            }
        }
        return -1;
    }


    /**
     * Runs the thread
     */
    public void run() {
        while (true) {
            try {
                // Read the chunk status of the neighbour
                String status = input.readUTF();

                // Select a chunk to send
                int chunkId = selectChunk(status);
                output.writeInt(chunkId);
                output.flush();

                if (chunkId > 0) {
                    // Send the chunk to the neighbour
                    sendChunk(chunkId);
                }
            } catch (IOException e) {
                break;
            }
        }
        closeConnection();
    }

    /**
     * Sends a chunk to the neighbour
     *
     * @param chunkId id of the chunk
     */
    private void sendChunk(int chunkId) {
        // Get chunk size and chunk directory from the config
        int chunkSize = Integer.parseInt(config.getProperty("ChunkSize"));
        String chunkDir = config.getProperty("ChunkDir");

        byte[] bytes = new byte[chunkSize];
        File file = new File(chunkDir + chunkId);
        FileInputStream input = null;
        try {
            // Read bytes from the file
            input = new FileInputStream(file);
            int length = input.read(bytes);

            // Send the number of bytes
            output.writeInt(length);

            // Send the bytes
            output.write(bytes, 0, length);
            output.flush();

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
     * Closes connection
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
