package org.backendbrilliance.aitutor;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Push
public class AiTutorApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(AiTutorApplication.class, args);
    }

}
