/**
 * The file to be distributed
 */
public class ChunkFile {

    // Name of the file
    private final String filename;

    // Size of the file
    private final long fileSize;

    // Number of chunks
    private final int chunkNum;

    /**
     * Constructor
     *
     * @param chunkNum Number of chunks
     * @param fileSize Size of the file
     * @param filename Name of the file
     */
    public ChunkFile(int chunkNum, long fileSize, String filename) {
        this.chunkNum = chunkNum;
        this.filename = filename;
        this.fileSize = fileSize;
    }

    /**
     * @return Name of the file
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return Size of the file
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * @return Number of chunks
     */
    public int getChunkNum() {
        return chunkNum;
    }
}