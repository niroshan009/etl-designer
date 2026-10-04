package com.kd.etl.designer.service;

import com.kd.etl.designer.dto.*;
import com.kd.etl.designer.model.ETLGraph;
import com.kd.etl.designer.model.ETLVertex;
import com.kd.etl.designer.utility.GraphSortUtility;
import lombok.extern.slf4j.Slf4j;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ETLServiceImpl implements ETLService {

    private final OllamaChatModel chatModel;
    private final ChatClient client;

    @Autowired
    public ETLServiceImpl(OllamaChatModel chatModel, ChatClient.Builder chatClientBuilder) {
        this.chatModel = chatModel;
        this.client = chatClientBuilder.build();
    }

    @Override
    public String generateGraph(ETLRequest etlRequest) throws IOException {

        String scriptName = etlRequest.getScriptName() == null || etlRequest.getScriptName().isBlank() ?
                "temp_script" : etlRequest.getScriptName();
        List<Node> nodes = etlRequest.getNodes();
        List<Edge> edges = etlRequest.getEdges();
        log.info("nodes size {}", nodes.size());
        log.info("edges size {}", edges.size());
        log.info("generating script {}", scriptName);

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
                    etlGraph.addVertex(targetVertex);
                }

            }

            if (null != sourceVertex && null != targetVertex) {
                etlGraph.addEdge(sourceVertex, targetVertex);
            }
        }

        List<Message> userMessages = new ArrayList<>();
        userMessages.add(new UserMessage("Set below configuration for spark job"));

        for (Configuration configuration : etlRequest.getConfiguration()) {
            userMessages.add(new UserMessage("Key: " + configuration.getConfig() + ", Value: " + configuration.getValue()));
        }
        userMessages.add(new UserMessage("Below is the spark dataflow"));

        List<ETLVertex> orderedNodes = GraphSortUtility.topologicalSort(etlGraph.getAdjacencyList());

        for (ETLVertex e : orderedNodes) {

            String sourceDataset = e.getInputDataset() != null ? String.join(" and ", e.getInputDataset().stream().filter(l -> !l.isRefData()).map(l -> l.getDataset()).collect(Collectors.toSet())) : "";
            String referenceDataset = e.getInputDataset() != null ? String.join(",", e.getInputDataset().stream().filter(l -> l.isRefData()).map(l -> l.getDataset()).collect(Collectors.toSet())) : "";
            String action = switch (e.getType()) {
                case "csvreader" -> "Create a dataset by reading data from with parameters %s and assign result dataset to %s variable.".formatted(e.getDescription(), e.getOutputDataset());
                case "filter" ->
                        "filter %s dataset with condition %s, and assign result dataset to %s variable.".formatted(sourceDataset, e.getDescription(), e.getOutputDataset());
                case "join" ->
                        "left join %s datasets on condition '%s' and assign result dataset to %s variable.".formatted(sourceDataset, e.getDescription(), e.getOutputDataset());
                case "left-anti-join" ->
                        "Do left anti join on %s with reference dataset %s on condition %s and assign result dataset to  %s variable. Don't use col function when joining.".formatted(sourceDataset, referenceDataset, e.getDescription(), e.getOutputDataset());
                case "drop-column" ->
                        "drop columns %s from the %s dataset and assign to %s dataset.".formatted(e.getDescription(), sourceDataset, e.getOutputDataset());
                case "icebergwriter" -> "Write %s dataset into Apache Iceberg table %s".formatted(sourceDataset, e.getDescription());
                default -> "";
            };
            userMessages.add(new UserMessage(action));
        }

        String script = generateScript(userMessages, scriptName);



        return script;

    }


    private Set<ETLVertex> getSourceNodes(ETLGraph<ETLVertex> etlGraph) {

        Set<ETLVertex> etlVertices = etlGraph.getAdjacencyList().keySet().stream()
                .filter(e -> e.getInputDataset() == null)
                .collect(Collectors.toSet());
        return etlVertices;
    }

    private Set<ETLVertex> getTerminalNodes(ETLGraph<ETLVertex> etlGraph) {
        Set<ETLVertex> etlVertices = etlGraph.getAdjacencyList().keySet().stream()
                .filter(e -> e.getOutputDataset() == null && etlGraph.getNeighbors(e).size() == 0)
                .collect(Collectors.toSet());

        return etlVertices;
    }


    private String generateScript(List<Message> messages, String scriptName) throws IOException {

        UserMessage[] userMessages = messages.toArray(new UserMessage[0]);

        SystemMessage[] systemMessages = new SystemMessage[]{
                new SystemMessage("You are a most efficient PySpark 3.5.4 Developer"),
                new SystemMessage("You only return the code. no explanation. don't add extensive comments. if explanation necessary add single line of comment"),
                new SystemMessage("You only write a python script using given instruction utilising spark framework."),
                new SystemMessage("You don't need to create test data"),
                new SystemMessage("Generate code in the given order"),
        };
        Message[] finalMessages = new Message[systemMessages.length + userMessages.length];

        System.arraycopy(systemMessages, 0, finalMessages, 0, systemMessages.length);
        System.arraycopy(userMessages, 0, finalMessages, systemMessages.length, userMessages.length);

        Path outputPath = Paths.get(System.getProperty("user.dir"))
                .resolve("src/main/resources")
                .resolve(scriptName.replace(" ", "_") + ".MD");

        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        client.prompt()
                .messages(finalMessages)
                .stream().content()
                // FIX: Explicitly cast the map output to the parent DataBuffer interface
                .map(chunk -> (DataBuffer) DefaultDataBufferFactory.sharedInstance.wrap(chunk.getBytes()))
                .as(streamOfBuffers -> DataBufferUtils.write(
                        streamOfBuffers,
                        outputPath,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING
                ))
                .block();
        return Files.readString(outputPath);
    }

}
