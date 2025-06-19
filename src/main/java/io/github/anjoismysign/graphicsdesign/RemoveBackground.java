package io.github.anjoismysign.graphicsdesign;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.anjoismysign.aesthetic.DirectoryAssistant;
import io.github.anjoismysign.aesthetic.PropertiesConfiguration;
import io.github.anjoismysign.aesthetic.RateLimitedProcessor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public enum RemoveBackground {
    INSTANCE;

    public static void job() {
        GraphicsDesign.Job job = GraphicsDesign.Job.REMOVE_BACKGROUND;
        Map<String, String> properties;
        try {
            properties = PropertiesConfiguration.create(
                    "remove.background.apiKey", "Copia_y_pega_aqui_mismo_el_apiKey_como_te_ensene",
                    "remove.background.maxRequests", "10"
            );
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        String apiToken = properties.get("remove.background.apiKey");
        if (apiToken.startsWith("Copia_y_p")){
            JOptionPane.showMessageDialog(null, "Ingresa una 'apiKey' válida", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
            return;
        }
        int maxRequests = Integer.parseInt(properties.get("remove.background.maxRequests"));

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        FileDragAndDrop fileDragAndDrop = new FileDragAndDrop(
                job.title(),
                droppedFile -> {
                    if (!droppedFile.isDirectory()) {
                        JOptionPane.showMessageDialog(
                                null,
                                "Debes arrastrar un directorio",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    int option = JOptionPane.showConfirmDialog(
                            null,
                            "¿Estás seguro de que quieres eliminar el fondo de todas las imágenes en el directorio?\n" +
                                    "El directorio es: " + droppedFile.getAbsolutePath(),
                            "Confirmación",
                            JOptionPane.DEFAULT_OPTION);
                    if (option != JOptionPane.YES_OPTION) {
                        return;
                    }
                    progressBar.setVisible(true);
                    CompletableFuture<Void> future = RemoveBackground.INSTANCE.removeBackgroundFromDirectory(
                            droppedFile,
                            integer -> {
                                SwingUtilities.invokeLater(()->{
                                    progressBar.setValue(integer);
                                });
                            },
                            files -> {
                                if (files.isEmpty()) {
                                    return;
                                }
                                StringBuilder stringBuilder = new StringBuilder();
                                for (File file : files) {
                                    stringBuilder.append(file.getAbsolutePath()).append("\n");
                                }
                                JOptionPane.showMessageDialog(
                                        null,
                                        stringBuilder.toString(),
                                        "Generaciones fallidas",
                                        JOptionPane.ERROR_MESSAGE);
                            },
                            apiToken,
                            maxRequests
                    );
                    future.thenRun(() -> SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, "Proceso completado.");
                        progressBar.setValue(0);
                        progressBar.setVisible(false);
                    }));
                },
                GraphicsDesign.DIMENSION(),
                job.getIcon());
        fileDragAndDrop.getMainPanel().add(progressBar, BorderLayout.SOUTH);
        progressBar.setVisible(false);
        fileDragAndDrop.revalidate();
        fileDragAndDrop.repaint();
        fileDragAndDrop.setVisible(true);
    }

    public CompletableFuture<Void> removeBackgroundFromDirectory(File directory,
                                                                 Consumer<Integer> progressCallback,
                                                                 Consumer<List<File>> onComplete,
                                                                 String apiToken,
                                                                 int maxRequests) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Provided path is not a directory: " + directory.getAbsolutePath());
        }

        DirectoryAssistant directoryAssistant = DirectoryAssistant.of(directory);
        Collection<File> imageFiles = directoryAssistant.listRecursively(".jpg", ".jpeg", ".png", ".webp");

        List<File> failedFiles = new ArrayList<>();

        return RateLimitedProcessor.INSTANCE.processAuto(
                maxRequests,
                TimeUnit.SECONDS.toMillis(60),
                imageFiles,
                imageFile -> {
                    try {
                        removeBackgroundFromFile(imageFile, apiToken);
                    } catch (Throwable exception) {
                        exception.printStackTrace();
                        failedFiles.add(imageFile);
                    }
                },
                progressCallback,
                () -> {
                    onComplete.accept(failedFiles);
                });
    }

    public void removeBackgroundFromFile(File imageFile,
                                         String apiToken) {
        File outputDirectory = new File(imageFile.getParentFile().getParentFile(), "output");
        outputDirectory.mkdirs();
        File destinationFile = new File(outputDirectory, imageFile.getName());
        String imageUrl = process(imageFile, apiToken);
        save(imageUrl, destinationFile.getAbsolutePath());
    }

    private String process(File imageFile,
                           String apiToken) {
        OkHttpClient client = new OkHttpClient();
        String mimeType;
        try {
            mimeType = Files.probeContentType(imageFile.toPath());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse(mimeType));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.getName(), fileBody)
                .addFormDataPart("content_moderation", "false")
                .build();

        Request request = new Request.Builder()
                .url("https://engine.prod.bria-api.com/v1/background/remove")
                .addHeader("api_token", apiToken)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseData = response.body().string();
                return tempUrl(responseData);
            } else {
                throw new RuntimeException("Request failed: " + response.code() + " - " + response.message());
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private String tempUrl(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }
        return root.get("result_url").asText();
    }

    private void save(String imageUrl,
                      String destinationFile) {
        try {
            URI uri = URI.create(imageUrl);
            try (InputStream in = uri.toURL().openStream()) {
                Files.copy(in, Paths.get(destinationFile), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | IllegalArgumentException exception) {
            exception.printStackTrace();
        }
    }

}
