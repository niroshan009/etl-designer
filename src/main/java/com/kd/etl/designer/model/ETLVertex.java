package com.kd.etl.designer.model;

import com.kd.etl.designer.dto.InputDataset;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ETLVertex implements Comparable {

    private String id;

    private List<InputDataset> inputDataset;

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

    public Set<String> getInputDatasetNames(){
        return this.inputDataset.stream().map(e-> e.getDataset()).collect(Collectors.toSet());
    }


    @Override
    public int compareTo(Object o) {

        ETLVertex vertex = (ETLVertex) o;

        Set<String> inputDsNames = this.inputDataset.stream().map(e -> e.getDataset()).collect(Collectors.toSet());

        if(inputDsNames.contains(vertex.getOutputDataset())) {
            return 1;
        } else if (((ETLVertex) o).getInputDataset().stream().map(e-> e.getDataset()).collect(Collectors.toSet()).contains(this.outputDataset)) {
            return -1;
        }
        return -1;
    }
}
