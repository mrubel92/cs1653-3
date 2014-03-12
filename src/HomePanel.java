
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

public class HomePanel extends JPanel {

    private static final long serialVersionUID = -8742357999390766582L;
    private JTextField username;
    private JPasswordField password;
    private JTextField ipAddress;
    private JTextField portNum;
    private final JButton btnLogin;
    private final JButton btnSwitchUser;
    protected static final String IP_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    protected static final String PORT_PATTERN = "\\d{4,5}";
    protected static final String PASSWORD_PATTERN = "\\w{6,16}";
    private static Pattern p;
    private static Pattern p2;
    private static Pattern p3;

    public HomePanel(final JTabbedPane tabbedPanel) {
        p = Pattern.compile(IP_PATTERN);
        p2 = Pattern.compile(RunClient.NAME_PATTERN);
        p3 = Pattern.compile(PORT_PATTERN);

        GridBagLayout gridBagLayout = new GridBagLayout();
        setLayout(gridBagLayout);

        // Title
        JLabel lblCryptoshare = new JLabel("CryptoShare");
        lblCryptoshare.setFont(new Font("Dialog", Font.BOLD, 20));
        GridBagConstraints gbc_lblCryptoshare = new GridBagConstraints();
        gbc_lblCryptoshare.insets = new Insets(0, 0, 10, 0);
        gbc_lblCryptoshare.gridx = 5;
        gbc_lblCryptoshare.gridy = 1;
        add(lblCryptoshare, gbc_lblCryptoshare);

        // Username label
        JLabel lblUsername = new JLabel("Username");
        GridBagConstraints gbc_lblUsername = new GridBagConstraints();
        gbc_lblUsername.insets = new Insets(10, 0, 0, 0);
        gbc_lblUsername.gridx = 5;
        gbc_lblUsername.gridy = 2;
        add(lblUsername, gbc_lblUsername);

        // Username text field
        username = new JTextField();
        username.setText("tdoshea90");
        GridBagConstraints gbc_username = new GridBagConstraints();
        gbc_username.insets = new Insets(0, 0, 10, 0);
        gbc_username.fill = GridBagConstraints.HORIZONTAL;
        gbc_username.gridx = 5;
        gbc_username.gridy = 3;
        add(username, gbc_username);
        
        // Password label
        JLabel lblPassword = new JLabel("Password");
        GridBagConstraints gbc_lblPassword = new GridBagConstraints();
        gbc_lblPassword.gridx = 5;
        gbc_lblPassword.gridy = 4;
        add(lblPassword, gbc_lblPassword);

        // Password text field
        password = new JPasswordField();
        GridBagConstraints gbc_password = new GridBagConstraints();
        gbc_password.insets = new Insets(0, 0, 10, 0);
        gbc_password.fill = GridBagConstraints.HORIZONTAL;
        gbc_password.gridx = 5;
        gbc_password.gridy = 5;
        add(password, gbc_password);

        // IP Label
        JLabel lblGroupServerIp = new JLabel("Group Server IP");
        GridBagConstraints gbc_lblGroupServerIp = new GridBagConstraints();
        gbc_lblGroupServerIp.gridx = 5;
        gbc_lblGroupServerIp.gridy = 6;
        add(lblGroupServerIp, gbc_lblGroupServerIp);

        // IP address text field
        ipAddress = new JTextField();
        ipAddress.setText("127.0.0.1");
        ipAddress.setFont(new Font("Monospaced", Font.PLAIN, 10));
        GridBagConstraints gbc_ipAddress = new GridBagConstraints();
        gbc_ipAddress.insets = new Insets(0, 0, 10, 0);
        gbc_ipAddress.fill = GridBagConstraints.HORIZONTAL;
        gbc_ipAddress.gridx = 5;
        gbc_ipAddress.gridy = 7;
        add(ipAddress, gbc_ipAddress);

        // Port label
        JLabel lblPort = new JLabel("Port");
        GridBagConstraints gbc_lblPort = new GridBagConstraints();
        gbc_lblPort.gridx = 5;
        gbc_lblPort.gridy = 8;
        add(lblPort, gbc_lblPort);

        // Port text field
        portNum = new JTextField();
        portNum.setText("8765");
        GridBagConstraints gbc_portNum = new GridBagConstraints();
        gbc_portNum.insets = new Insets(0, 0, 10, 0);
        gbc_portNum.fill = GridBagConstraints.HORIZONTAL;
        gbc_portNum.gridx = 5;
        gbc_portNum.gridy = 9;
        add(portNum, gbc_portNum);

        // Login button
        btnLogin = new JButton("Login");
        GridBagConstraints gbc_btnLogin = new GridBagConstraints();
        gbc_btnLogin.insets = new Insets(0, 0, 5, 0);
        gbc_btnLogin.gridx = 5;
        gbc_btnLogin.gridy = 10;
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                String un = username.getText();
                Matcher m2 = p2.matcher(un);
                if (!m2.matches()) {
                    JOptionPane.showMessageDialog(null, "Bad username format!", "Bad Username",
                                                  JOptionPane.OK_CANCEL_OPTION);
                    return;
                }

                if (connectToServer(ipAddress, portNum, RunClient.gclient)) {
                    RunClient.userToken = RunClient.gclient.getToken(un.toUpperCase());
                    if (RunClient.userToken == null) {
                        JOptionPane.showMessageDialog(null,
                                                      "User does not exist! Only administrators can create users.", "Bad User",
                                                      JOptionPane.OK_CANCEL_OPTION);
                        return;
                    }
                    btnLogin.setEnabled(false);
                    username.setEnabled(false);
                    password.setEnabled(false);
                    ipAddress.setEnabled(false);
                    portNum.setEnabled(false);
                    btnSwitchUser.setEnabled(true);
                    tabbedPanel.setEnabledAt(1, true);
                    tabbedPanel.setEnabledAt(2, true);
                }
            }
        });
        add(btnLogin, gbc_btnLogin);

        // Switch user button
        btnSwitchUser = new JButton("Switch User");
        GridBagConstraints gbc_btnSwitchUser = new GridBagConstraints();
        gbc_btnSwitchUser.gridx = 5;
        gbc_btnSwitchUser.gridy = 11;
        btnSwitchUser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                btnLogin.setEnabled(true);
                username.setEnabled(true);
                password.setEnabled(true);
                ipAddress.setEnabled(true);
                portNum.setEnabled(true);
                btnSwitchUser.setEnabled(false);

                tabbedPanel.remove(1);
                tabbedPanel.remove(1);
                JComponent groupPanel = new GroupPanel();
                tabbedPanel.addTab("Groups", groupPanel);
                JComponent fserverPanel = new FileServerPanel();
                tabbedPanel.addTab("File Servers", fserverPanel);
                tabbedPanel.setEnabledAt(1, false);
                tabbedPanel.setEnabledAt(2, false);

                if (RunClient.gclient.isConnected())
                    RunClient.gclient.disconnect();
                if (RunClient.fclient.isConnected())
                    RunClient.fclient.disconnect();
            }
        });
        add(btnSwitchUser, gbc_btnSwitchUser);
        btnSwitchUser.setEnabled(false);
    }

    protected static boolean connectToServer(JTextField ipText, JTextField portText, Client client) {
        String ip = ipText.getText();
        Matcher m = p.matcher(ip);
        if (!m.matches()) {
            JOptionPane.showMessageDialog(null, "Bad IP address format!", "Bad IP",
                                          JOptionPane.OK_CANCEL_OPTION);
            return false;
        }

        String port = portText.getText();
        Matcher m3 = p3.matcher(port);
        if (!m3.matches()) {
            JOptionPane.showMessageDialog(null, "Bad port format!", "Bad port", JOptionPane.OK_CANCEL_OPTION);
            return false;
        }

        if (!client.connect(ip, Integer.parseInt(port))) {
            JOptionPane.showMessageDialog(null, "Failed to connect to server!", "Connection Failed",
                                          JOptionPane.OK_CANCEL_OPTION);
            System.out.println("Failed to connect to: " + ip + ":" + port);
            return false;
        }
        JOptionPane.showMessageDialog(null, "Successfully connected to: " + ip + ":" + port);
        System.out.println("Successfully connected to: " + ip + ":" + port);
        return true;
    }
}
