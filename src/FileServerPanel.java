
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

public class FileServerPanel extends JPanel {

    private static final long serialVersionUID = -6016321343107722251L;

    private JTextField txtIpAddress;
    private JTextField txtPort;

    protected static final String FILENAME_PATTERN = "[\\w+]{16}.[A-Za-z]{3,4}";
    private static Pattern p;

    @SuppressWarnings("rawtypes")
    private final DefaultListModel groupListModel;
    @SuppressWarnings("rawtypes")
    private final DefaultListModel fileListModel;
    private boolean connected;

    private JButton btnUpload;
    private JButton btnDownload;
    private JButton btnDelete;
    private JButton btnListGroups;
    private JButton btnListGFiles;
    private JButton btnListFiles;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public FileServerPanel() {
        connected = false;
        p = Pattern.compile(FILENAME_PATTERN);

        add(createToolBar());

        // Main pane. Buttons on left, lists on right.
        final JSplitPane contentPane = new JSplitPane();

        // Lists
        groupListModel = new DefaultListModel();
        final JList groupList = new JList(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane groupListScrollPane = new JScrollPane(groupList);

        fileListModel = new DefaultListModel();
        final JList fileList = new JList(fileListModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane fileListScrollPane = new JScrollPane(fileList);

        // Setup left panel
        contentPane.setLeftComponent(createLeftPanel(groupList, fileList));

        // Setup right panel
        contentPane.setRightComponent(createRightPanel(fileListScrollPane, groupListScrollPane));

        // Everything to parent
        add(contentPane);
    }

    private Component createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JLabel lblFServerLabel = new JLabel(" File Server ");
        toolBar.add(lblFServerLabel);

        txtIpAddress = new JTextField();
        txtIpAddress.setHorizontalAlignment(SwingConstants.CENTER);
        txtIpAddress.setText("127.0.0.1");
        toolBar.add(txtIpAddress);
        txtIpAddress.setColumns(10);

        txtPort = new JTextField();
        txtPort.setHorizontalAlignment(SwingConstants.CENTER);
        txtPort.setText("4321");
        toolBar.add(txtPort);
        txtPort.setColumns(10);

        JButton btnConnectButton = new JButton("Connect");
        btnConnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (HomePanel.connectToServer(txtIpAddress.getText(), txtPort.getText(), RunClient.fclient, "FILE")) {
                    connected = true;
                    btnUpload.setEnabled(true);
                    btnDownload.setEnabled(true);
                    btnDelete.setEnabled(true);
                    btnListGroups.setEnabled(true);
                    btnListGFiles.setEnabled(true);
                    btnListFiles.setEnabled(true);
                }
            }
        });
        toolBar.add(btnConnectButton);
        return toolBar;
    }

    private Component createRightPanel(final JScrollPane fileListScrollPane, final JScrollPane groupListScrollPane) {
        JSplitPane rightPanel = new JSplitPane();

        // Group list panel: label + list
        JPanel groupPanelContainer = new JPanel();
        groupPanelContainer.add(new JLabel("My Groups"));
        groupPanelContainer.add(groupListScrollPane);
        groupPanelContainer.setLayout(new BoxLayout(groupPanelContainer, BoxLayout.Y_AXIS));

        // files list panel: label + list
        JPanel filesPanelContainer = new JPanel();
        filesPanelContainer.add(new JLabel("Files"));
        filesPanelContainer.add(fileListScrollPane);
        filesPanelContainer.setLayout(new BoxLayout(filesPanelContainer, BoxLayout.Y_AXIS));

        rightPanel.setLeftComponent(groupPanelContainer);
        rightPanel.setRightComponent(filesPanelContainer);

        return rightPanel;
    }

    @SuppressWarnings("rawtypes")
    private Component createLeftPanel(final JList groupList, final JList fileList) {
        JPanel leftPanel = new JPanel();
        GridBagLayout gbl_leftPanel = new GridBagLayout();
        gbl_leftPanel.columnWidths = new int[]{130, 0};
        gbl_leftPanel.rowHeights = new int[]{15, 25, 25, 25, 25, 0};
        gbl_leftPanel.columnWeights = new double[]{0.0, Double.MIN_VALUE};
        gbl_leftPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        leftPanel.setLayout(gbl_leftPanel);

        // File options label
        JLabel lblGroupOptions = new JLabel("File Options");
        GridBagConstraints gbc_lblGroupOptions = new GridBagConstraints();
        gbc_lblGroupOptions.anchor = GridBagConstraints.WEST;
        gbc_lblGroupOptions.insets = new Insets(0, 0, 5, 0);
        gbc_lblGroupOptions.gridx = 0;
        gbc_lblGroupOptions.gridy = 0;
        leftPanel.add(lblGroupOptions, gbc_lblGroupOptions);

        // Upload button
        btnUpload = new JButton("Upload");
        btnUpload.setActionCommand("Upload");
        btnUpload.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (!connected)
                    return;

                int index = groupList.getSelectedIndex();
                if (index != -1) {
                    String group = groupListModel.get(index).toString();
                    String sourceFile = JOptionPane.showInputDialog("Name of file (max length 16)");
                    Matcher m = p.matcher(sourceFile);
                    if (!m.matches())
                        if (RunClient.fclient.upload(sourceFile, sourceFile, group, RunClient.userToken)) {
                            List<String> files = RunClient.fclient.listGroupFiles(RunClient.userToken, group);
                            fileListModel.removeAllElements();
                            for (String file : files) {
                                if (!fileListModel.contains(sourceFile))
                                    fileListModel.addElement(file);
                            }
                        } else
                            JOptionPane.showMessageDialog(null, "Failed to upload: " + sourceFile);
                    else
                        JOptionPane.showMessageDialog(null, "Bad file name format!", "Bad File",
                                                      JOptionPane.OK_CANCEL_OPTION);
                } else
                    JOptionPane.showMessageDialog(null, "Select a group");
            }
        });
        GridBagConstraints gbc_btnUpload = new GridBagConstraints();
        gbc_btnUpload.anchor = GridBagConstraints.WEST;
        gbc_btnUpload.insets = new Insets(0, 0, 5, 0);
        gbc_btnUpload.gridx = 0;
        gbc_btnUpload.gridy = 1;
        gbc_btnUpload.fill = GridBagConstraints.HORIZONTAL;
        btnUpload.setEnabled(false);
        leftPanel.add(btnUpload, gbc_btnUpload);

        // Download button
        btnDownload = new JButton("Download");
        btnDownload.setActionCommand("Download");
        btnDownload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!connected)
                    return;

                int index = fileList.getSelectedIndex();
                if (index != -1) {
                    String file = fileListModel.get(index).toString();
                    if (RunClient.fclient.download(file, file, RunClient.userToken))
                        JOptionPane.showMessageDialog(null, "File: " + file
                                + " successfully downloaded to your current directory");
                    else
                        JOptionPane.showMessageDialog(null, "Failed to download: " + file);
                } else
                    JOptionPane.showMessageDialog(null, "Select a file");
            }
        });
        GridBagConstraints gbc_btnDownload = new GridBagConstraints();
        gbc_btnDownload.anchor = GridBagConstraints.WEST;
        gbc_btnDownload.insets = new Insets(0, 0, 5, 0);
        gbc_btnDownload.gridx = 0;
        gbc_btnDownload.gridy = 2;
        gbc_btnDownload.fill = GridBagConstraints.HORIZONTAL;
        btnDownload.setEnabled(false);
        leftPanel.add(btnDownload, gbc_btnDownload);

        // Delete file button
        btnDelete = new JButton("Delete File");
        btnDelete.setActionCommand("Delete File");
        btnDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!connected)
                    return;

                int index = fileList.getSelectedIndex();
                if (index != -1) {
                    String file = fileListModel.get(index).toString();
                    int reply = JOptionPane.showConfirmDialog(null, "Really delete file: " + file, "WARNING",
                                                              JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION)
                        if (RunClient.fclient.delete(file, RunClient.userToken)) {
                            JOptionPane.showMessageDialog(null, "File: " + file + " successfully deleted");
                            fileListModel.removeElementAt(index);
                        } else
                            JOptionPane.showMessageDialog(null, "Failed to delete: " + file);
                } else
                    JOptionPane.showMessageDialog(null, "Select a file");
            }
        });
        GridBagConstraints gbc_btnDelete = new GridBagConstraints();
        gbc_btnDelete.anchor = GridBagConstraints.WEST;
        gbc_btnDelete.insets = new Insets(0, 0, 5, 0);
        gbc_btnDelete.gridx = 0;
        gbc_btnDelete.gridy = 3;
        gbc_btnDelete.fill = GridBagConstraints.HORIZONTAL;
        btnDelete.setEnabled(false);
        leftPanel.add(btnDelete, gbc_btnDelete);

        // List Groups button
        btnListGroups = new JButton("List Groups");
        btnListGroups.setActionCommand("List Groups");
        btnListGroups.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> groups = RunClient.getGroups();
                groupListModel.removeAllElements();
                for (String group : groups) {
                    groupListModel.addElement(group);
                }
            }
        });
        GridBagConstraints gbc_btnListGroups = new GridBagConstraints();
        gbc_btnListGroups.anchor = GridBagConstraints.WEST;
        gbc_btnListGroups.insets = new Insets(0, 0, 5, 0);
        gbc_btnListGroups.gridx = 0;
        gbc_btnListGroups.gridy = 4;
        gbc_btnListGroups.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(btnListGroups, gbc_btnListGroups);

        // List group files button
        btnListGFiles = new JButton("List Group Files");
        btnListGFiles.setActionCommand("List Group Files");
        btnListGFiles.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!connected)
                    return;

                int index = groupList.getSelectedIndex();
                if (index != -1) {
                    String selectedGroup = groupListModel.get(index).toString();
                    if (RunClient.userToken.getGroups().contains(selectedGroup)) {
                        List<String> files = RunClient.fclient.listGroupFiles(RunClient.userToken, selectedGroup);
                        fileListModel.removeAllElements();
                        for (String file : files) {
                            fileListModel.addElement(file);
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Group doesn't exist");
                        groupListModel.remove(index);
                    }
                } else
                    JOptionPane.showMessageDialog(null, "Select a group");
            }
        });
        GridBagConstraints gbc_btnListGFiles = new GridBagConstraints();
        gbc_btnListGFiles.anchor = GridBagConstraints.WEST;
        gbc_btnListGFiles.insets = new Insets(0, 0, 5, 0);
        gbc_btnListGFiles.gridx = 0;
        gbc_btnListGFiles.gridy = 5;
        gbc_btnListGFiles.fill = GridBagConstraints.HORIZONTAL;
        btnListGFiles.setEnabled(false);
        leftPanel.add(btnListGFiles, gbc_btnListGFiles);

        // List all files button
        btnListFiles = new JButton("List All Files");
        btnListFiles.setActionCommand("List All Files");
        btnListFiles.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!connected)
                    return;

                List<String> files = RunClient.fclient.listFiles(RunClient.userToken);
                fileListModel.removeAllElements();
                for (String file : files) {
                    fileListModel.addElement(file);
                }
            }
        });
        GridBagConstraints gbc_btnListFiles = new GridBagConstraints();
        gbc_btnListFiles.anchor = GridBagConstraints.WEST;
        gbc_btnListFiles.insets = new Insets(0, 0, 5, 0);
        gbc_btnListFiles.gridx = 0;
        gbc_btnListFiles.gridy = 6;
        gbc_btnListFiles.fill = GridBagConstraints.HORIZONTAL;
        btnListFiles.setEnabled(false);
        leftPanel.add(btnListFiles, gbc_btnListFiles);

        return leftPanel;
    }
}
