package com.kd.etl.designer.controller;

import com.kd.etl.designer.dto.ETLRequest;
import com.kd.etl.designer.service.ETLService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController()
@RequestMapping("/etl")
@CrossOrigin(origins = "http://localhost:5173")
public class ETLController {

    private final OllamaChatModel chatModel;
    private final ChatClient client;
    private final ETLService etlService;

    @Autowired
    public ETLController(OllamaChatModel chatModel, ChatClient.Builder chatClientBuilder, ETLService etlService) {
        this.chatModel = chatModel;
        this.client = chatClientBuilder.build();
        this.etlService = etlService;
    }


    @PostMapping("/create")
    public ResponseEntity createJournal(@RequestBody ETLRequest etlRequest) {


        List<String> myMessages = new ArrayList<>();

        etlService.generateGraph(etlRequest);

//        myMessages.add("Create a spark context");
//        myMessages.add("Read file from s3 bucket");
//        myMessages.add("Filter if amount less than 100");
//        myMessages.add("save data to iceberg");
//        System.out.println("going to generate spark script");
//
//        Message[] m = new Message[] {
//                new SystemMessage("You are a most efficient Apache Spark Developer"),
//                new SystemMessage("You only return the code. no explanation. just plain code and code comments"),
//                new UserMessage("Create a spark context with iceberg configuration as follow"),
//                new UserMessage("spark.sql.catalog.demo:org.apache.iceberg.spark.SparkCatalog"),
//                new UserMessage("spark.sql.catalog.demo.type:rest"),
//                new UserMessage("spark.sql.catalog.demo.uri:catalogEndpoint"),
//                new UserMessage("spark.sql.catalog.demo.io-impl:org.apache.iceberg.aws.s3.S3FileIO"),
//                new UserMessage("Read file from s3 bucket"),
//                new UserMessage("Filter if amount less than 100"),
//                new UserMessage("save data to iceberg")
//        };
//        StringBuilder lineBuffer = new StringBuilder();
//
//        client.prompt()
//                .messages(m)
//                        .stream().content()
//                .doOnNext(chunk -> {
//                    // 1. Append the new token chunk to our buffer
//                    lineBuffer.append(chunk);
//
//                    // 2. Extract full lines if the buffer contains a newline character
//                    String currentText = lineBuffer.toString();
//                    if (currentText.contains("\n")) {
//                        // Split by newline, keeping trailing empty strings
//                        String[] lines = currentText.split("\n", -1);
//
//                        // Print all complete lines to the console
//                        for (int i = 0; i < lines.length - 1; i++) {
//                            System.out.println(lines[i]);
//                        }
//
//                        // Keep the remaining incomplete line fragment in the buffer
//                        lineBuffer.setLength(0);
//                        lineBuffer.append(lines[lines.length - 1]);
//                    }
//                })
//                .doOnComplete(() -> {
//                    // 3. Print any remaining text left in the buffer when the stream ends
//                    if (lineBuffer.length() > 0) {
//                        System.out.println( lineBuffer.toString());
//                    }
//                    System.out.println("[Console Log] --- STREAM COMPLETE ---");
//                })
//                .blockLast(); // <-- CRUCIAL: Blocks this thread until the entire AI stream is fully logged
//        ;

        return null;
    }
}
