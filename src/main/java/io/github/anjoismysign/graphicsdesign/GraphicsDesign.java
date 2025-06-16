package io.github.anjoismysign.graphicsdesign;

import io.github.anjoismysign.anjo.swing.components.AnjoComboBox;
import io.github.anjoismysign.hahaswing.BubbleFactory;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.Image;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class GraphicsDesign {

    enum Job {
        REMOVE_BACKGROUND(
                "eliminar fondo",
                "Eliminar fondo de una imagen",
                RemoveBackground::job
        );

        private final String title;
        private final String description;
        private final Runnable onSelect;

        private static final Map<String, Job> DESCRIPTION_MAP = Arrays.stream(values())
                .collect(Collectors.toMap(Job::description, job -> job));

        Job(String title,
            String description,
            Runnable onSelect) {
            this.title = title;
            this.description = description;
            this.onSelect = onSelect;
        }

        public static Job fromDescription(String description) {
            return DESCRIPTION_MAP.getOrDefault(description, null);
        }

        public String title() {
            return TITLE+" - "+title.toUpperCase(Locale.ROOT);
        }

        public String description() {
            return description;
        }

        public String code() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Runnable onSelect() {
            return onSelect;
        }

        public Image getIcon() {
            Dimension dimension = ICON_DIMENSION();
            return new ImageIcon(
                    Objects.requireNonNull(
                            GraphicsDesign.class.getResource("/" + code() + ".png")))
                    .getImage().getScaledInstance(dimension.width, dimension.height, Image.SCALE_DEFAULT);
        }
    }

    public static Dimension ICON_DIMENSION() {
        return new Dimension(256, 256);
    }

    public static Dimension DIMENSION() {
        return new Dimension(300, 300);
    }

    public static final String TITLE = "calvos s.a".toUpperCase(Locale.ROOT);

    public static void main(String[] args) {
        Dimension dimension = ICON_DIMENSION();
        List<String> jobs = Arrays.stream(Job.values())
                .map(Job::description)
                .toList();
        AnjoComboBox comboBox = AnjoComboBox.build("Trabajo", jobs);
        BubbleFactory.getInstance().controller(
                        null,
                        TITLE,
                        new ImageIcon(Objects.requireNonNull(GraphicsDesign.class.getResource("/icon.png")))
                                .getImage().getScaledInstance(dimension.width, dimension.height, Image.SCALE_SMOOTH),
                        true,
                        null,
                        comboBox)
                .onBlow(anjoPane -> {
                    String selection = (String) comboBox.getComponent().getSelectedItem();
                    Job job = Job.fromDescription(selection);
                    if (job == null) {
                        JOptionPane.showMessageDialog(
                                null,
                                "Trabajo no encontrado: " + selection,
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        System.exit(0);
                        return;
                    }
                    job.onSelect().run();
                });
    }

}
