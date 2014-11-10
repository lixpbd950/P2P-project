import java.io.*;

public class SplitFiles {

    /**
     * Splits a file into several chunks
     */
    public static int split(int chunkSize, String chunkDir, File file)
            throws IOException {
        // File does not exist
        if (!file.exists()) {
            throw new FileNotFoundException("The input file does not exist");
        }

        // Create the chunk directory (spt/)
        File directory = new File(chunkDir);
        if (!directory.exists()) {
            directory.mkdir();
        }

        // Calculate the number of chunks
        long totalSize = file.length();
        System.out.println("File size is " + totalSize);
        System.out.println("Each chunk size is " + chunkSize);
        int n = (int) (totalSize / chunkSize);
        if (totalSize % chunkSize != 0) {
            n++;
        }
        System.out.println("There are " + n + " chunks!");


        // Split the file into n chunks
        byte[] bytes = new byte[chunkSize];
        System.out.print("Splitting");
        for (int i = 0; i < n; i++) {
            // Seek the file to the i-th chunk
            RandomAccessFile reader = new RandomAccessFile(file, "rw");
            reader.seek(i * chunkSize);

            // Read the chunk
            int length = reader.read(bytes);

            // Store the chunk to the file system
            save(chunkDir + (i + 1), bytes, length);
            System.out.print(".");
        }
        System.out.println("Splitting file finished!");

        // Return the number of chunks
        return n;
    }

    /**
     * Stores the chunk file to the file
     *
     * @param filename chunk file name
     * @param bytes    bytes array
     * @param length   number of bytes
     */
    private static void save(String filename, byte[] bytes, int length)
            throws IOException {
        FileOutputStream output = null;
        try {
            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
            }
            output = new FileOutputStream(filename);
            output.write(bytes, 0, length);

        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
}
