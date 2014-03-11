/* T
 * his list represents the files on the server */

import java.util.ArrayList;
import java.util.Collections;

public class FileList implements java.io.Serializable {

    /* Serializable so it can be stored in a file for persistence */
    private static final long serialVersionUID = -8911161283900260136L;
    private final ArrayList<ShareFile> list;

    public FileList() {
        list = new ArrayList<>();
    }

    public synchronized void addFile(String owner, String group, String path) {
        ShareFile newFile = new ShareFile(owner, group, path);
        list.add(newFile);
    }

    public synchronized void removeFile(String path) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getPath().equals(path))
                list.remove(i);
        }
    }

    public synchronized boolean checkFile(String path) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getPath().equals(path))
                return true;
        }
        return false;
    }

    public synchronized ArrayList<ShareFile> getFiles() {
        Collections.sort(list);
        return list;
    }

    public synchronized ShareFile getFile(String path) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getPath().equals(path))
                return list.get(i);
        }
        return null;
    }
}
