package com.kd.etl.designer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Edge {

    private String source;

    private String target;

    private String label;
}
