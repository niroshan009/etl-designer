package com.kd.etl.designer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ETLRequest implements Serializable {

    private List<Edge> edges;

    private List<Node> nodes;
}
