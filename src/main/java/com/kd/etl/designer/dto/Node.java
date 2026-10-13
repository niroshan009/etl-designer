package com.kd.etl.designer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Node {
    private String id;
    private String type;
    private String description;
    private List<Configuration> configurations;
}
