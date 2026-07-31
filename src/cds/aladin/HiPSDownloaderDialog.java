package cds.aladin;

import java.awt.*;
import java.io.File;
import javax.swing.*;

public class HiPSDownloaderDialog extends JDialog implements HiPSDownloader.HiPSDownloaderListener {

    private JComboBox<String> urlCombo;
    private JSpinner orderSpinner;
    private JTextField dirField;
    private JCheckBox fastCheckCb;
    private JSpinner threadSpinner;
    private JButton startBtn;
    private JButton cancelBtn;
    private JLabel progressLabel;
    
    private HiPSDownloader downloader;
    private long startTime;

    public HiPSDownloaderDialog(Window owner) {
        super(owner, "HiPS Dataset Downloader", false);
        setLayout(new BorderLayout(10, 10));
        
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // URL
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("HiPS URL:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        urlCombo = new JComboBox<>(new String[] {
            "http://alasky.cds.unistra.fr/DSS/DSSColor/",
            "http://alasky.cds.unistra.fr/2MASS/Color/"
        });
        urlCombo.setEditable(true);
        inputPanel.add(urlCombo, gbc);
        
        // Max Order
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Max Order:"), gbc);
        gbc.gridx = 1;
        orderSpinner = new JSpinner(new SpinnerNumberModel(3, 0, 20, 1));
        inputPanel.add(orderSpinner, gbc);
        
        // Target Dir
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Output Dir:"), gbc);
        gbc.gridx = 1;
        dirField = new JTextField(System.getProperty("user.home") + File.separator + "HiPS");
        inputPanel.add(dirField, gbc);
        gbc.gridx = 2;
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(dirField.getText());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                dirField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        inputPanel.add(browseBtn, gbc);
        
        // Fast Check
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Fast Check:"), gbc);
        gbc.gridx = 1;
        fastCheckCb = new JCheckBox("Skip existing files");
        fastCheckCb.setSelected(true);
        inputPanel.add(fastCheckCb, gbc);
        
        // Threads
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Max Threads:"), gbc);
        gbc.gridx = 1;
        threadSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 32, 1));
        inputPanel.add(threadSpinner, gbc);
        
        add(inputPanel, BorderLayout.CENTER);
        
        // Progress & Buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        progressLabel = new JLabel("Ready.");
        bottomPanel.add(progressLabel, BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        startBtn = new JButton("Start");
        cancelBtn = new JButton("Cancel");
        cancelBtn.setEnabled(false);
        
        startBtn.addActionListener(e -> startDownload());
        cancelBtn.addActionListener(e -> cancelDownload());
        
        btnPanel.add(startBtn);
        btnPanel.add(cancelBtn);
        bottomPanel.add(btnPanel, BorderLayout.EAST);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(owner);
    }
    
    private void startDownload() {
        String url = (String) urlCombo.getSelectedItem();
        File dir = new File(dirField.getText());
        int maxOrder = (Integer) orderSpinner.getValue();
        
        if (url == null || url.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please specify a HiPS URL", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        startBtn.setEnabled(false);
        cancelBtn.setEnabled(true);
        progressLabel.setText("Starting...");
        
        boolean fastCheck = fastCheckCb.isSelected();
        int threads = (Integer) threadSpinner.getValue();
        
        startTime = System.currentTimeMillis();
        downloader = new HiPSDownloader(url.trim(), dir, maxOrder, fastCheck, threads, this);
        downloader.start();
    }
    
    private void cancelDownload() {
        if (downloader != null) {
            downloader.cancel();
        }
        startBtn.setEnabled(true);
        cancelBtn.setEnabled(false);
        progressLabel.setText("Cancelled.");
    }

    @Override
    public void onProgress(int tilesDownloaded, long totalBytesDownloaded, int activeTasks) {
        long elapsed = System.currentTimeMillis() - startTime;
        double speed = 0;
        if (elapsed > 0) {
            speed = (totalBytesDownloaded / 1024.0) / (elapsed / 1000.0);
        }
        String sizeStr = String.format("%.2f MB", totalBytesDownloaded / (1024.0 * 1024.0));
        String speedStr = String.format("%.2f KB/s", speed);
        
        progressLabel.setText(String.format("Tiles: %d | Size: %s | Speed: %s | Active Tasks: %d", 
                tilesDownloaded, sizeStr, speedStr, activeTasks));
    }

    @Override
    public void onComplete() {
        startBtn.setEnabled(true);
        cancelBtn.setEnabled(false);
        progressLabel.setText(progressLabel.getText() + " - Completed!");
        LocalHiPSManager.addLocalHiPS((String) urlCombo.getSelectedItem(), dirField.getText());
        JOptionPane.showMessageDialog(this, "Download completed!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void onError(String message) {
        startBtn.setEnabled(true);
        cancelBtn.setEnabled(false);
        progressLabel.setText("Error: " + message);
        JOptionPane.showMessageDialog(this, "Error: " + message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
