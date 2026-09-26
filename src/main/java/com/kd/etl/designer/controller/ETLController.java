package com.kd.etl.designer.controller;

import com.kd.etl.designer.dto.ETLRequest;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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


    @PostMapping(value="/create")
    public ResponseEntity createJournal(@RequestBody ETLRequest etlRequest) {
        String script = etlService.generateGraph(etlRequest);

        log.info("Received script from LLM");
        return new ResponseEntity<>(script, HttpStatus.OK);
    }

    @GetMapping(value = "/submitjob")
    public ResponseEntity submitJob() throws IOException {

        etlSubmitService.submitEtlService();
        return new ResponseEntity(HttpStatus.OK);
    }
}
