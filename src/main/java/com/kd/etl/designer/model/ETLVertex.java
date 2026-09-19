package com.kd.etl.designer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ETLVertex {

    private String id;

    private List<String> inputDataset;

    private String outputDataset;

    private String type;

    private String description;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ETLVertex etlVertex = (ETLVertex) o;
        return Objects.equals(id, etlVertex.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
