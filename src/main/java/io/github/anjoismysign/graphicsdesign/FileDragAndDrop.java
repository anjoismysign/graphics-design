package io.github.anjoismysign.graphicsdesign;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class FileDragAndDrop extends JFrame {

    private final JLabel imageLabel;
    private final JPanel mainPanel;

    public FileDragAndDrop(String title,
                           Consumer<File> fileConsumer,
                           Dimension dimension,
                           Image icon) {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(dimension);
        setMaximumSize(dimension);
        setMinimumSize(dimension);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        setIconImage(icon);

        imageLabel = new JLabel(new ImageIcon(icon));
        imageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(imageLabel, BorderLayout.CENTER);

        add(mainPanel);

        imageLabel.setDropTarget(createDropTarget(fileConsumer));
    }

    private DropTarget createDropTarget(Consumer<File> consumer) {
        return new DropTarget(imageLabel, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = event.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (files != null && !files.isEmpty()) {
                            File droppedFile = files.get(0);
                            consumer.accept(droppedFile);
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    JOptionPane.showMessageDialog(
                            FileDragAndDrop.this,
                            "Error handling the dropped file.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

}