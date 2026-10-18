package com.kd.etl.designer.controller;

import com.kd.etl.designer.dto.ETLRequest;
import com.kd.etl.designer.dto.SparkSubmitJobRequest;
import com.kd.etl.designer.service.ETLService;
import com.kd.etl.designer.service.ETLSubmitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Slf4j
@RestController()
@RequestMapping("/etl")
@CrossOrigin(origins = "http://localhost:5173")
public class ETLController {

    private final OllamaChatModel chatModel;
    private final ChatClient client;
    private final ETLService etlService;
    private final ETLSubmitService etlSubmitService;

    @Autowired
    public ETLController(OllamaChatModel chatModel, ChatClient.Builder chatClientBuilder, ETLService etlService,
                         ETLSubmitService etlSubmitService) {
        this.chatModel = chatModel;
        this.client = chatClientBuilder.build();
        this.etlService = etlService;
        this.etlSubmitService = etlSubmitService;
    }


    @PostMapping(value="/create", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> createJournal(@RequestBody ETLRequest etlRequest) throws IOException {
        log.info("Received script from LLM");
        return etlService.generateGraphStream(etlRequest);
    }

    @PostMapping(value = "/submitjob")
    public ResponseEntity submitJob(@RequestBody SparkSubmitJobRequest sparkSubmitJobRequest) throws IOException {
        etlSubmitService.submitEtlService(sparkSubmitJobRequest);
        return new ResponseEntity(HttpStatus.OK);
    }
}
