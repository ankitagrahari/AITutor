package org.backendbrilliance.aitutor.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.FileUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.apache.commons.lang3.tuple.Triple;
import org.backendbrilliance.aitutor.service.ChatService;
import org.backendbrilliance.aitutor.service.RAGService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Route("")
@PageTitle("AI Tutor")
public class HomePage extends Composite<VerticalLayout>{

    private static final Logger log = LoggerFactory.getLogger(HomePage.class);
    private final MessageList messageList = new MessageList();
    private final ChatService chatService;
    private final RAGService ragService;
    private final String chatId = UUID.randomUUID().toString();

    HomePage(ChatService chatService, RAGService ragService){
        this.chatService = chatService;
        this.ragService = ragService;

        var appLogo = VaadinIcon.ADD_DOCK.create();
        appLogo.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.IconSize.LARGE);

        var appName = new Span("AI Tutor");
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);

        var header = new Div(appLogo, appName);
        header.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Padding.MEDIUM, LumoUtility.Gap.MEDIUM, LumoUtility.AlignItems.CENTER);

        VerticalLayout mainFeatures = new VerticalLayout();
        Triple<Boolean, File, FileUploadHandler> triple = this.fileUploadHandler(mainFeatures);
        Upload docUpload = getUpload(triple.getRight());
        AtomicBoolean isDocUpload = new AtomicBoolean(false);
        HorizontalLayout buttonLayout = new HorizontalLayout();

        //Upload All button
//        Button uploadAllButton = new Button("Upload All Files");
//        uploadAllButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
//        uploadAllButton.addClickListener(event -> {
//            // No explicit Flow API for this at the moment
//            docUpload.getElement().callJsFunction("uploadFiles");
//        });

        TextField sourceURL = new TextField("Source URL");
        sourceURL.setPlaceholder("http://example.com");

        //Analyze the uploaded document
        Button analyzeButton = new Button("Analyze Uploaded Documents");
        analyzeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        analyzeButton.setEnabled(triple.getLeft());
        analyzeButton.addClickListener(event -> {
            //Call the RAGService to upload the document to Vector Database.
            ragService.uploadToVectorDB(triple.getMiddle().getName(), sourceURL.getValue());
        });

        docUpload.addAllFinishedListener(event -> {
            analyzeButton.setEnabled(true);
            isDocUpload.set(true);
        });

        sourceURL.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value != null && !value.isEmpty()) {
                if (isValidUrl(value)) {
                    // Valid URL: clear any previous error
                    sourceURL.setInvalid(false);
                    sourceURL.setErrorMessage(null);
                    analyzeButton.setEnabled(true);
                    Notification.show("URL is valid: " + value, 1000, Notification.Position.BOTTOM_END);
                } else {
                    // Invalid URL: set an error message and mark as invalid
                    sourceURL.setInvalid(true);
                    sourceURL.setErrorMessage("Please enter a valid URL (e.g., http://example.com)");
                }
            } else {
                // Handle empty or null case (optional: depending on whether the field is required)
                if(isDocUpload.get()) {
                    sourceURL.setInvalid(false);
                    sourceURL.setErrorMessage(null);
                } else {
                    sourceURL.setInvalid(true);
                    sourceURL.setErrorMessage("Either document upload or Source URL is mandatory");
                }
            }
        });

        buttonLayout.add(analyzeButton);
        buttonLayout.add(sourceURL);

        mainFeatures.add(
                docUpload, buttonLayout
        );

        Span span = new Span("Chatbot");
        Scroller scroller = new Scroller(messageList);
        scroller.setHeightFull();

        var messageInput = new MessageInput();
        messageInput.addSubmitListener(this::onSubmit);
        messageInput.setWidthFull();

        getContent().add(
                header,
                mainFeatures,
                span,
                scroller,
                messageInput
        );
    }

    private static @NonNull Upload getUpload(FileUploadHandler fileUploadHandler) {
        Upload docUpload = new Upload(fileUploadHandler);
        docUpload.setAcceptedFileTypes("application/pdf", ".pdf", ".doc");
        docUpload.setMaxFiles(1);
        int maxFileSizeInBytes = 10 * 1024 * 1024; // 10MB
        docUpload.setMaxFileSize(maxFileSizeInBytes);
//        docUpload.setAutoUpload(false);
        docUpload.addFileRejectedListener(event -> {
            String errorMsg = event.getErrorMessage();
            Notification notification = Notification.show(errorMsg, 5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });
        return docUpload;
    }

    private void onSubmit(MessageInput.SubmitEvent submitEvent) {
        //create and handle a prompt message
        var promptMessage = new MessageListItem(submitEvent.getValue(), Instant.now(), "User");
        promptMessage.setUserColorIndex(0);
        messageList.addItem(promptMessage);

        //create and handle the response message
        var responseMessage = new MessageListItem("", Instant.now(), "Bot");
        responseMessage.setUserColorIndex(1);
        messageList.addItem(responseMessage);

        //append a response message to the existing UI
        var userPrompt = submitEvent.getValue();
        var uiOptional = submitEvent.getSource().getUI();
        uiOptional.ifPresent(ui -> chatService.chatStream(userPrompt, chatId)
                .subscribe(token ->
                        ui.access(() ->
                                responseMessage.appendText(token))));

    }

    public Triple<Boolean, File, FileUploadHandler> fileUploadHandler(VerticalLayout mainFeatures){
        Paragraph uploadProgress = new Paragraph();
        File dir = new File("src/main/resources/docs/", String.valueOf(System.currentTimeMillis()));

        AtomicBoolean isUploaded = new AtomicBoolean(false);
        FileUploadHandler uploadHandler = UploadHandler.toFile(
                        (metadata, file) -> {
                            log.info("File saved to: {} of size:{}mb", file.getAbsoluteFile(), (file.length()/(1024*1024)));
                        }, uploadMetaData -> {
                            if(dir.mkdir())
                                return new File(dir, uploadMetaData.fileName());
                            throw new NoSuchFileException("Directory don't exists!");
                        })
                .whenStart(() -> log.info("Upload Started"))
                .onProgress((transferredBytes, totalBytes) -> {
                    double per = ((double) transferredBytes / totalBytes) * 100;
                    log.info("Uploaded {}% ..", Math.floor(per));
                    uploadProgress.removeAll();
                    uploadProgress.add("Uploaded " + Math.floor(per) + "% ..");

                })
                .whenComplete(success -> {
                    if (success) {
                        log.info("Upload completed!!");
                        uploadProgress.removeAll();
                        uploadProgress.add("Upload Completed!!");
                        isUploaded.set(true);
                    } else {
                        log.error("Upload failed!!");
                        uploadProgress.removeAll();
                        uploadProgress.add("Upload Failed!!");
                    }
                });
        mainFeatures.add(uploadProgress);
        return Triple.of(isUploaded.get(), dir, uploadHandler);
    }

    private boolean isValidUrl(String urlString) {
        try {
            new URL(urlString);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
