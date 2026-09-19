package com.kd.etl.designer.service;

import com.kd.etl.designer.dto.ETLRequest;
import com.kd.etl.designer.dto.Edge;
import com.kd.etl.designer.dto.Node;
import com.kd.etl.designer.model.ETLGraph;
import com.kd.etl.designer.model.ETLVertex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ETLServiceImpl implements ETLService {

    @Override
    public void generateGraph(ETLRequest etlRequest) {

        // node == vertices
        // edges ==
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
                    etlGraph.addVertex(targetVertex);
                }



//                targetVertex.setOutputDataset(); // target node does not have a output dataset

            }

            if(null != sourceVertex && null != targetVertex) {
                etlGraph.addEdge(sourceVertex,targetVertex);
            }
//            etlGraph.printGraph();
        }

        log.info("finished adding data");

        etlGraph.printGraph();


//        for(Node node : nodes){
//            log.info("adding node: {}, type: {}", node.getId(), node.getType());
//
//
//
//
//
//
//        }



    }
}
