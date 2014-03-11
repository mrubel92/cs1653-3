
import java.util.ArrayList;
import java.util.Hashtable;

public class GroupList implements java.io.Serializable {

    private static final long serialVersionUID = -2536806905954268497L;
    private final Hashtable<String, Group> list = new Hashtable<>();

    public synchronized void addGroup(String groupname) {
        Group newGroup = new Group();
        list.put(groupname, newGroup);
    }

    public synchronized void deleteGroup(String groupname) {
        list.remove(groupname);
    }

    public synchronized boolean checkGroup(String groupname) {
        return list.containsKey(groupname);
    }

    public synchronized ArrayList<String> getGroupUsers(String groupname) {
        return list.get(groupname).getUsers();
    }

    public synchronized ArrayList<String> getGroupOwnership(String groupname) {
        return list.get(groupname).getOwnership();
    }

    public synchronized void addMember(String group, String username) {
        list.get(group).addMember(username);
    }

    public synchronized void removeMember(String group, String username) {
        list.get(group).removeMember(username);
    }

    public synchronized void addOwnership(String group, String username) {
        list.get(group).addOwnership(username);
    }

    public synchronized void removeOwnership(String group, String username) {
        list.get(group).removeOwnership(username);
    }

    class Group implements java.io.Serializable {

        private static final long serialVersionUID = -6699986336399821598L;
        private final ArrayList<String> users;
        private final ArrayList<String> ownership;

        public Group() {
            users = new ArrayList<>();
            ownership = new ArrayList<>();
        }

        public ArrayList<String> getUsers() {
            return users;
        }

        public ArrayList<String> getOwnership() {
            return ownership;
        }

        public void addMember(String user) {
            users.add(user);
        }

        public void removeMember(String user) {
            if (!users.isEmpty())
                if (users.contains(user))
                    users.remove(users.indexOf(user));
        }

        public void addOwnership(String user) {
            ownership.add(user);
        }

        public void removeOwnership(String user) {
            if (!ownership.isEmpty())
                if (ownership.contains(user))
                    ownership.remove(ownership.indexOf(user));
        }

    }

}
