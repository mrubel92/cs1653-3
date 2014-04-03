
import java.util.ArrayList;

public class Envelope implements java.io.Serializable {

    private static final long serialVersionUID = -7726335089122193103L;
    private final String msg;
    private final ArrayList<Object> objContents = new ArrayList<>();
    private byte[] hmac;

    public Envelope(String text) {
        msg = text;
        hmac = null;
    }

    public void addHmac(byte[] a)
    {
        hmac = a;
    }

    public byte[] rmHmac()
    {
        byte[] hmacRet = hmac.clone();
        hmac = null;
        return hmacRet;
    }

    @Override
    public String toString() {
        return "Envelope [msg=" + msg + ", objContents=" + objContents + "]";
    }

    public String getMessage() {
        return msg;
    }

    public ArrayList<Object> getObjContents() {
        return objContents;
    }

    public void addObject(Object object) {
        objContents.add(object);
    }

}
