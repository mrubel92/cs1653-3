
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

public class RunClient extends JFrame {

    private static final long serialVersionUID = -7140891447099041238L;

    protected static GroupClient gclient;
    protected static FileClient fclient;
    protected static UserToken userToken;
    protected static final String NAME_PATTERN = "\\w{1,16}";

    // Constructor. Instantiates the GroupClient and tries to connect.
    public RunClient() {
        gclient = new GroupClient();
        fclient = new FileClient();
        initGUI();
    }

    // Main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                RunClient rc = new RunClient();
                rc.setVisible(true);
            }
        });
    }

    // Adds all the components to the main frame.
    private void initGUI() {

        JTabbedPane tabbedPanel = new JTabbedPane();

        JComponent homePanel = new HomePanel(tabbedPanel);
        tabbedPanel.addTab("Home", homePanel);

        JComponent groupPanel = new GroupPanel();
        tabbedPanel.addTab("Groups", groupPanel);

        JComponent fserverPanel = new FileServerPanel();
        tabbedPanel.addTab("File Servers", fserverPanel);

        tabbedPanel.setEnabledAt(1, false);
        tabbedPanel.setEnabledAt(2, false);

        add(tabbedPanel);

        setTitle("CryptoShare");
        setSize(800, 400);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (gclient.isConnected())
                    gclient.disconnect();
                if (fclient.isConnected())
                    fclient.disconnect();
                System.exit(0);
            }
        });
    }

    protected static String askForValidInput(String question, Component c) {
        String input;
        Pattern p = Pattern.compile(NAME_PATTERN);
        input = JOptionPane.showInputDialog(question);
        if (input != null) {
            Matcher m = p.matcher(input);
            if (!m.matches())
                JOptionPane.showMessageDialog(c, "Invalid input");
            else
                return input.toUpperCase();
        }
        return null;
    }

    protected static List<String> getGroups() {
        List<String> groups = RunClient.gclient.listGroups(userToken);
        if (groups == null)
            JOptionPane.showMessageDialog(null, "Failed to retreive member list for group: "
                    + userToken.getSubject());
        return groups;
    }
}
