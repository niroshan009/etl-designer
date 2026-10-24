package com.kd.etl.designer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "etl")
public class ETLProperties {

    private String sparkMaster;
    private String sparkHome;
    private String accessKey;
    private String secretKey;
    private String endpointRegion;
    private String scriptOutputPath;

    private Hadoop hadoop = new Hadoop();

    private Kube kube = new Kube();

    @Data
    public static class Kube {
        private String containerImage;
        private String namespace;
        private String serviceAccountName;
        private String fileUploadPath;
    }

    @Data
    public static class Hadoop{

        private S3 s3 = new S3();
        @Data
        public static class S3{

            private String accessKey;
            private String secretKey;
            private String endpoint;
            private String sslEnabled;
            private String endpointRegion;
        }
    }


}
