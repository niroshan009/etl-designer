package com.kd.etl.designer.service;

import com.kd.etl.designer.dto.ETLRequest;
import reactor.core.publisher.Flux;

import java.io.IOException;

public interface ETLService {

    Flux<String> generateGraphStream(ETLRequest etlRequest) throws IOException;
}
