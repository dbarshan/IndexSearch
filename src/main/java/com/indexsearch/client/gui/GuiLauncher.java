package com.indexsearch.client.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.support.ResourcePropertySource;
import com.indexsearch.client.config.RestClientConfig;
import com.indexsearch.client.service.ClientService;

/**
 * JavaFX entry point for the GUI client.
 */
public class GuiLauncher extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources()
                .addFirst(new ResourcePropertySource("classpath:application-client.properties"));
        context.register(RestClientConfig.class, ClientService.class, GuiController.class);
        context.refresh();

        FXMLLoader loader = new FXMLLoader(GuiLauncher.class.getResource("/client/gui/screen.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();
        GuiController controller = loader.getController();
        Scene scene = new Scene(root);
        stage.setTitle("IndexSearch");
        stage.getIcons().add(new Image(GuiLauncher.class.getResourceAsStream("/client/gui/icon.png")));
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            if (controller != null) {
                controller.shutdown();
            }
            context.close();
        });
        stage.show();
    }

    public static void launchGui(String[] args) {
        launch(GuiLauncher.class, args);
    }
}
