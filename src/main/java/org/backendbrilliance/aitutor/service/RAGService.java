package org.backendbrilliance.aitutor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class RAGService {

    private final Logger log = LoggerFactory.getLogger(RAGService.class);
    private final JdbcClient jdbcClient;
    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;

    public RAGService(JdbcClient jdbcClient, VectorStore vectorStore, ResourceLoader resourceLoader) {
        this.jdbcClient = jdbcClient;
        this.vectorStore = vectorStore;
        this.resourceLoader = resourceLoader;
    }

    public void uploadToVectorDB(String directoryName){
        //Read from directory all the files
        //Split the documents and upload to VectorStore
        //Ref https://github.com/danvega/spring-into-ai/blob/main/src/main/java/dev/danvega/rag/RagConfiguration.java
        log.info("RAG Service processing files under directory {}", directoryName);

        PdfDocumentReaderConfig readerConfig = PdfDocumentReaderConfig.builder()
                .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder().withNumberOfBottomTextLinesToDelete(0)
                        .withNumberOfTopPagesToSkipBeforeDelete(0)
                        .build())
                .withPagesPerDocument(1)
                .build();

        List<Resource> resources = getFilesUnderDirectory(directoryName);
        resources.forEach(resource -> {
            PagePdfDocumentReader pdfDocumentReader = new PagePdfDocumentReader(resource, readerConfig);
            var textSplitter = new TokenTextSplitter();
            vectorStore.accept(textSplitter.apply(pdfDocumentReader.get()));
        });
        log.info("Document uploaded to vector store!!");
    }

    private List<Resource> getFilesUnderDirectory(String directoryName) {
        File dir = new File("src/main/resources/docs/"+directoryName);
        if(dir.exists()){
            List<File> files = Arrays.asList(Objects.requireNonNull(dir.listFiles()));
            return files.stream()
                    .map(file -> resourceLoader.getResource("file:" + file.getAbsolutePath()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
