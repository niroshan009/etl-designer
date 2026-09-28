package com.kd.etl.designer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SparkSubmitJobRequest implements Serializable {

    private String scriptName;

}
