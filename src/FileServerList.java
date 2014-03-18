
import java.io.Serializable;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class FileServerList implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, String> ipMap;
    private final Map<String, PublicKey> keyMap;
    
    public FileServerList() {
        ipMap = new HashMap<>();
        keyMap = new HashMap<>();
    }
    
    public void addFileServer(String ip, String fingerprint, PublicKey publicKey) {
        ipMap.put(ip, fingerprint);
        keyMap.put(fingerprint, publicKey);
    }
    
    public boolean checkIP(String ip) {
        return ipMap.containsKey(ip);
    }
    
    public PublicKey getPublicKey(String ip) {
        return keyMap.get(ipMap.get(ip));
    }
    
    public boolean checkFingerprint(String fingerprint) {
        return ipMap.containsValue(fingerprint);
    }
}
