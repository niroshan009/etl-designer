package com.kd.etl.designer.service;

import com.kd.etl.designer.config.ETLProperties;
import com.kd.etl.designer.config.PromptRegistry;
import com.kd.etl.designer.dto.*;
import com.kd.etl.designer.model.ETLGraph;
import com.kd.etl.designer.model.ETLVertex;
import com.kd.etl.designer.utility.GraphSortUtility;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ETLServiceImpl implements ETLService {

    private final OllamaChatModel chatModel;
    private final ChatClient client;
    private final PromptRegistry promptRegistry;
    private final Executor executor;
    private final ETLProperties etlProperties;

    @Autowired
    public ETLServiceImpl(OllamaChatModel chatModel, ChatClient.Builder chatClientBuilder, PromptRegistry promptRegistry, Executor executor,ETLProperties etlProperties) {
        this.chatModel = chatModel;
        this.client = chatClientBuilder.build();
        this.promptRegistry = promptRegistry;
        this.executor = executor;
        this.etlProperties = etlProperties;
    }

    @Override
    public Flux<String> generateGraphStream(ETLRequest etlRequest) throws IOException {
        String scriptName = etlRequest.getScriptName() == null || etlRequest.getScriptName().isBlank() ?
                "temp_script" : etlRequest.getScriptName();
        List<Node> nodes = etlRequest.getNodes();
        List<Edge> edges = etlRequest.getEdges();
        log.info("nodes size {}", nodes.size());
        log.info("edges size {}", edges.size());
        log.info("generating script {}", scriptName);



        List<Message> userMessages = new ArrayList<>();
        userMessages.add(new UserMessage("Set below configuration for spark job"));

        for (Configuration configuration : etlRequest.getConfiguration()) {
            userMessages.add(new UserMessage("Key: " + configuration.getConfig() + ", Value: " + configuration.getValue()));
        }
        userMessages.add(new UserMessage("Below is the spark dataflow"));

        List<ETLVertex> orderedNodes = createEtlGraph(edges, nodes);

        for (ETLVertex e : orderedNodes) {

            String sourceDataset = e.getInputDataset() != null ? String.join(" and ", e.getInputDataset().stream().filter(l -> !l.isRefData()).map(l -> l.getDataset()).collect(Collectors.toSet())) : "";
            String referenceDataset = e.getInputDataset() != null ? String.join(",", e.getInputDataset().stream().filter(l -> l.isRefData()).map(l -> l.getDataset()).collect(Collectors.toSet())) : "";
            String config = String.join(",", e.getConfigurations().stream().map(element -> element.getConfig()+"="+element.getValue()).collect(Collectors.toSet()));
            String action = getActionPrompt(e, sourceDataset, config, referenceDataset);
            log.info(action);
            userMessages.add(new UserMessage(action));
        }
        return generateScriptAsStream(userMessages, scriptName);
    }

    private @NonNull String getActionPrompt(ETLVertex e, String sourceDataset, String config, String referenceDataset) {
        String action = switch (e.getType()) {
            case "csvreader" -> {
                String path = e.getConfigurations().stream().filter(conf -> conf.getConfig().equalsIgnoreCase("filePath")).findFirst().get().getValue();
                e.getConfigurations().removeIf(conf -> conf.getConfig().equalsIgnoreCase("filePath"));
                String csvConfig = String.join(" , ", e.getConfigurations().stream().map(element -> element.getConfig()+"="+element.getValue()).collect(Collectors.toSet()));
                yield promptRegistry.get(e.getType()).formatted(path, csvConfig, e.getOutputDataset());
            }
            case "filter" -> {

                yield promptRegistry.get(e.getType()).formatted(sourceDataset, config, e.getOutputDataset());
            }
            case "left-join" -> {
                String condition = String.join(" ", e.getConfigurations().stream().map(element -> element.getValue()).collect(Collectors.toSet()));
                yield promptRegistry.get(e.getType()).formatted(sourceDataset, condition, e.getOutputDataset());
            }
            case "left-anti-join" -> {

                yield promptRegistry.get(e.getType()).formatted(sourceDataset, referenceDataset, config, e.getOutputDataset());
            }
            case "drop-column" -> {
                String columnsToDrop = String.join(" , ", e.getConfigurations().stream().map(element -> element.getValue()).collect(Collectors.toSet()));
                yield promptRegistry.get(e.getType()).formatted(columnsToDrop, sourceDataset, e.getOutputDataset());
            }
            case "icebergwriter" -> {
                yield promptRegistry.get(e.getType()).formatted(sourceDataset, config);
            }
            case "databasewriter" -> {
                String databaseType = e.getConfigurations().stream().filter(element -> element.getConfig().equalsIgnoreCase("databaseType")).findFirst().get().getValue();
                e.getConfigurations().removeIf(conf -> conf.getConfig().equalsIgnoreCase("databaseType"));
                String dbConfiguration = String.join(" , ", e.getConfigurations().stream().map(element -> element.getConfig()+"="+element.getValue()).collect(Collectors.toSet()));
                yield promptRegistry.get(e.getType()).formatted(databaseType, dbConfiguration);
            }
            default -> "";
        };
        return action;
    }

    private static @NonNull List<ETLVertex> createEtlGraph(List<Edge> edges, List<Node> nodes) {
        ETLGraph<ETLVertex> etlGraph = new ETLGraph<>();

        for (Edge edge : edges) {
            String label = edge.getLabel();
            Boolean isRefData = edge.isRefData();
            String source = edge.getSource();
            String target = edge.getTarget();

            Node sourceNode = nodes.stream().filter(n -> n.getId().equals(source)).findFirst().orElse(null);
            Node targetNode = nodes.stream().filter(n -> n.getId().equals(target)).findFirst().orElse(null);

            ETLVertex sourceVertex = null;
            ETLVertex targetVertex = null;

            if (sourceNode != null) {

                sourceVertex = new ETLVertex();
                sourceVertex.setId(sourceNode.getId());

                if (null != etlGraph.getVertex(sourceVertex)) {
                    ETLVertex existingVertex = etlGraph.getVertex(sourceVertex);
                    if (existingVertex != null) {
                        existingVertex.setOutputDataset(label);
                    }

                } else {
                    sourceVertex.setOutputDataset(label);
                    sourceVertex.setType(sourceNode.getType());
                    sourceVertex.setDescription(sourceNode.getDescription());
                    sourceVertex.setConfigurations(sourceNode.getConfigurations());
                    etlGraph.addVertex(sourceVertex);
                }
            }

            if (targetNode != null) {

                targetVertex = new ETLVertex();
                targetVertex.setId(targetNode.getId());

                if (null != etlGraph.getVertex(targetVertex)) {
                    ETLVertex existingVertex = etlGraph.getVertex(targetVertex);
                    if (null == existingVertex.getInputDataset()) {
                        List<InputDataset> inputDs = new ArrayList<>();


                        inputDs.add(new InputDataset(label, isRefData));
                        existingVertex.setInputDataset(inputDs);
                    } else {
                        existingVertex.getInputDataset().add(new InputDataset(label, isRefData));
                    }
                    targetVertex = existingVertex;
                } else {
                    List<InputDataset> inputDs = new ArrayList<>();
                    inputDs.add(new InputDataset(label, isRefData));
                    targetVertex.setType(targetNode.getType());
                    targetVertex.setInputDataset(inputDs);
                    targetVertex.setDescription(targetNode.getDescription());
                    targetVertex.setConfigurations(targetNode.getConfigurations());
                    etlGraph.addVertex(targetVertex);
                }

            }

            if (null != sourceVertex && null != targetVertex) {
                etlGraph.addEdge(sourceVertex, targetVertex);
            }
        }
        return GraphSortUtility.topologicalSort(etlGraph.getAdjacencyList());
    }

    private Flux<String> generateScriptAsStream(List<Message> messages, String scriptName) throws IOException {
        UserMessage[] userMessages = messages.toArray(new UserMessage[0]);
        SystemMessage[] systemMessages = new SystemMessage[]{
                new SystemMessage("You are a most efficient PySpark 3.5.4 Developer."),
                new SystemMessage("Only return the code. no explanation. don't add extensive comments. if explanation necessary add single line of comment."),
                new SystemMessage("Only write a python script using given instruction utilising pySpark framework."),
                new SystemMessage("Don't need to create test data."),
                new SystemMessage("Use option() chain when passing the configuration. ex: .option(\"delimiter\", \",\")."),
                new SystemMessage("Generate code in the given order."),
        };
        Message[] finalMessages = new Message[systemMessages.length + userMessages.length];

        System.arraycopy(systemMessages, 0, finalMessages, 0, systemMessages.length);
        System.arraycopy(userMessages, 0, finalMessages, systemMessages.length, userMessages.length);

        Path outputPath = Paths.get(System.getProperty("user.dir"))
                .resolve(etlProperties.getScriptOutputPath())
                .resolve(scriptName.replace(" ", "_") + ".MD");

        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            log.error("Could not create a script");
            throw new RuntimeException(e);
        }


        Flux<String> tokenStream =  client.prompt()
                .messages(finalMessages)
                .stream().content()
                .share();

        executor.execute(() -> {
                log.info("writing data to file {}", outputPath);
            try {
                tokenStream
                        .map(chunk -> (DataBuffer) DefaultDataBufferFactory.sharedInstance.wrap(chunk.getBytes()))
                        .as(streamOfBuffer -> DataBufferUtils.write(streamOfBuffer, outputPath,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE,
                                StandardOpenOption.TRUNCATE_EXISTING
                        ))
                        .block();
                log.info("Finished writing data to file.");
            } catch (Exception e) {
                log.error("Failed to write data to file", e);
            }
        });

        return tokenStream
                .bufferUntil(token -> token.contains("\n") || token.contains("\r"), true)
                .map(tokenList -> String.join("", tokenList))
                .map(line -> line);
    }
}
