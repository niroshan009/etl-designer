package com.kd.etl.designer.service;

import com.kd.etl.designer.dto.ETLRequest;
import org.springframework.stereotype.Service;

public interface ETLService {

    public void generateGraph(ETLRequest etlRequest);
}
