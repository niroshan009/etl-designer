package com.kd.etl.designer.service;

import com.kd.etl.designer.dto.Configuration;
import com.kd.etl.designer.dto.ETLRequest;
import com.kd.etl.designer.dto.Edge;
import com.kd.etl.designer.dto.Node;
import com.kd.etl.designer.model.ETLGraph;
import com.kd.etl.designer.model.ETLVertex;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import org.springframework.ai.chat.messages.Message;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ETLServiceImpl implements ETLService {

    private final OllamaChatModel chatModel;
    private final ChatClient client;

    @Autowired
    public ETLServiceImpl(OllamaChatModel chatModel, ChatClient.Builder chatClientBuilder){
        this.chatModel = chatModel;
        this.client = chatClientBuilder.build();
    }

    @Override
    public String generateGraph(ETLRequest etlRequest) {

        List<Node> nodes = etlRequest.getNodes();
        List<Edge> edges = etlRequest.getEdges();
        log.info("nodes size {}", nodes.size());
        log.info("edges size {}", edges.size());

        ETLGraph<ETLVertex> etlGraph = new ETLGraph<>();

        for(Edge edge : edges){
            String label = edge.getLabel();
            String source = edge.getSource();
            String target = edge.getTarget();

            log.info("checking edge: {}, source: {}, target: {}", label, source, target);
            Node sourceNode = nodes.stream().filter(n -> n.getId().equals(source)).findFirst().orElse(null);
            Node targetNode = nodes.stream().filter(n -> n.getId().equals(target)).findFirst().orElse(null);

            ETLVertex sourceVertex = null;
            ETLVertex targetVertex = null;

            if(sourceNode != null){
                log.info("adding a source node. node id: {} ", sourceNode.getId());

                sourceVertex = new ETLVertex();
                sourceVertex.setId(sourceNode.getId());

                if (null != etlGraph.getVertex(sourceVertex)) {
                    ETLVertex existingVertex = etlGraph.getVertex(sourceVertex);
                    if(existingVertex != null) {
                        existingVertex.setOutputDataset(label);
                    }

                } else {
                    sourceVertex.setOutputDataset(label);
                    sourceVertex.setType(sourceNode.getType());
                    sourceVertex.setDescription(sourceNode.getDescription());
                    etlGraph.addVertex(sourceVertex);
                }
            }

            if(targetNode != null){

                log.info("adding a target node {}",targetNode.getId());
                targetVertex = new ETLVertex();
                targetVertex.setId(targetNode.getId());

                if (null != etlGraph.getVertex(targetVertex)) {
                    log.info("target node exists as source node");
                    ETLVertex existingVertex = etlGraph.getVertex(targetVertex);
                    if(null == existingVertex.getInputDataset())
                    {
                        List<String> inputDs = new ArrayList<>();
                        inputDs.add(label);
                        existingVertex.setInputDataset(inputDs);
                    } else {
                        existingVertex.getInputDataset().add(label);
                    }
                    targetVertex = existingVertex;
                } else {
                    log.info("target node not added as source node");
                    List<String> inputDs = new ArrayList<>();
                    inputDs.add(label);
                    targetVertex.setType(targetNode.getType());
                    targetVertex.setInputDataset(inputDs);
                    targetVertex.setDescription(targetNode.getDescription());
                    etlGraph.addVertex(targetVertex);
                }

            }

            if(null != sourceVertex && null != targetVertex) {
                etlGraph.addEdge(sourceVertex,targetVertex);
            }
        }


        Set<ETLVertex> sourceNodes = getSourceNodes(etlGraph);
        Set<ETLVertex> terminalNodes = getTerminalNodes(etlGraph);
        Set<ETLVertex> intermediateNodes = etlGraph.getAdjacencyList().keySet()
                        .stream()
                        .filter(e -> !sourceNodes.contains(e))
                        .filter(e -> !terminalNodes.contains(e))
                        .collect(Collectors.toSet());
        StringBuilder generation = new StringBuilder();

        List<Message> userMessages = new ArrayList<>();
        userMessages.add(new UserMessage("Set below configuration for spark job"));

        for(Configuration configuration : etlRequest.getConfiguration()){
            userMessages.add(new UserMessage("Key: "+configuration.getConfig()+", Value: "+configuration.getValue()));
        }
        userMessages.add(new UserMessage("Below is the spark dataflow"));

        for(ETLVertex e : sourceNodes) {
            String instruction = "Create a dataset %s reading data using %s. from %s\n".formatted(e.getOutputDataset(), e.getType(), e.getDescription());
            userMessages.add(new UserMessage(instruction));
        }

        for(ETLVertex e : intermediateNodes){
            String action = switch (e.getType()){
                case "filter" -> "filter %s dataset with condition %s, and produce %s dataset.\n".formatted(e.getInputDataset(), e.getDescription(), e.getOutputDataset());
                case "join" -> "join %s datasets on condition %s, and produce dataset %s.\n".formatted(e.getInputDataset(),e.getDescription(), e.getOutputDataset());
                default -> "";
            };
            userMessages.add(new UserMessage(action));
        }


        for(ETLVertex e : terminalNodes) {
            String instruction = "Save a dataset %s to %s.\n".formatted(e.getInputDataset(), e.getType());
            userMessages.add(new UserMessage(instruction));
        }

        String script = generateScript(userMessages);

        log.info("finished adding data");

//        etlGraph.printGraph();

        return script;

    }



    private Set<ETLVertex> getSourceNodes(ETLGraph<ETLVertex> etlGraph){

        Set<ETLVertex> etlVertices =  etlGraph.getAdjacencyList().keySet().stream()
                .filter(e -> e.getInputDataset() == null)
                .collect(Collectors.toSet());
        return etlVertices;
    }

    private Set<ETLVertex> getTerminalNodes(ETLGraph<ETLVertex> etlGraph){
        Set<ETLVertex> etlVertices = etlGraph.getAdjacencyList().keySet().stream()
                .filter(e -> e.getOutputDataset() == null && etlGraph.getNeighbors(e).size() == 0)
                .collect(Collectors.toSet());

        return etlVertices;
    }


    private String generateScript(List<Message> messages){


        UserMessage[] userMessages = messages.toArray(new UserMessage[0]);

        SystemMessage[] systemMessages = new SystemMessage[] {
                new SystemMessage("You are a most efficient Apache Spark Developer"),
                new SystemMessage("You only return the code. no explanation. don't add extensive comments. if explanation necessary add single line of comment"),
                new SystemMessage("You only write a python script using given instruction utilising spark framework."),
                new SystemMessage("You don't need to create test data"),
                new SystemMessage("Some instruction might not be in order so see if one step depends on dataset from another later step. if so execute those steps first"),
        };
        Message[] finalMessages = new Message[systemMessages.length + userMessages.length];

        System.arraycopy(systemMessages,0,finalMessages, 0, systemMessages.length);
        System.arraycopy(userMessages, 0, finalMessages, systemMessages.length, userMessages.length);

        String finalMessage =  client.prompt()
                .messages(finalMessages)
                .stream().content()
                .collect(StringBuilder::new, StringBuilder::append)
                .map(StringBuilder::toString)
                .block();

        return finalMessage;

    }

}
