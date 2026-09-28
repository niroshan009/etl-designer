package com.kd.etl.designer.service;

import com.kd.etl.designer.dto.SparkSubmitJobRequest;

import java.io.IOException;

public interface ETLSubmitService {
    public void submitEtlService(SparkSubmitJobRequest sparkSubmitJobRequest) throws IOException;
}
