package admin.encryption;

/**
 * Created by Admin on 4/15/2018.
 */

public class FileModel {

    public FileAdapter.Status status;
    public String name;
    public long received, size;

    public FileModel(String name, long received, long size, FileAdapter.Status status) {
        this.status = status;
        this.name = name;
        this.received = received;
        this.size = size;
    }
}
