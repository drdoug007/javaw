package one.dastec.javaw.wasi;

/**
 * Object-oriented file abstraction over WASI Virtual File System (VFS).
 */
public class WASIFile {

    private final String path;

    public WASIFile(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public boolean exists() {
        return WASI.fileExists(path);
    }

    public int length() {
        return WASI.fileLength(path);
    }

    public String readString() {
        return WASI.readString(path);
    }

    public boolean writeString(String content) {
        return WASI.writeString(path, content);
    }

    public boolean appendString(String content) {
        return WASI.appendString(path, content);
    }

    public boolean delete() {
        return WASI.deleteFile(path);
    }

    public boolean mkdir() {
        return WASI.createDirectory(path);
    }
}
