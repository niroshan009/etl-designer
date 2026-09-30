package com.kd.etl.designer.service;

import com.kd.etl.designer.dto.SparkSubmitJobRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkAppHandle;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;


import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class ETLSubmitServiceImpl implements ETLSubmitService {


    @Autowired
    private S3Client s3Client;

    @Override
    public void submitEtlService(SparkSubmitJobRequest sparkSubmitJobRequest) throws IOException {

        System.setProperty("aws.accessKeyId", "rustfsadmin");
        System.setProperty("aws.secretAccessKey", "rustfsadmin");
        System.setProperty("aws.region", "us-east-1");


        Path outputPath = Paths.get(System.getProperty("user.dir"))
                .resolve("src/main/resources")
                .resolve(sparkSubmitJobRequest.getScriptName().replace(" ", "_") + ".MD");

        String filContent = Files.readString(outputPath).replaceAll("(?m)^```python\\s*$", "")
                .replaceAll("(?m)^```\\s*$", "");

        System.out.println(filContent);


        // 3. Build the request and set the ContentType to Python
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket("scripts")
                .key(sparkSubmitJobRequest.getScriptName().replace(" ", "_") + ".py")
                .contentType("text/x-python") // Tells S3/browsers it's a Python script
                .build();

        // 4. Upload the code string directly
        PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, RequestBody.fromString(filContent));

        URL reportUrl = s3Client.utilities().getUrl(GetUrlRequest.builder()
                .bucket("scripts")
                .key(sparkSubmitJobRequest.getScriptName().replace(" ", "_") + ".py")
                .build());


        log.info("submitting script {}", reportUrl.getFile());

        SparkLauncher launcher = new SparkLauncher()
                .setAppName(reportUrl.getFile())
                .setMaster("k8s://https://127.0.0.1:26443")
                .setDeployMode("cluster")
                .setSparkHome("/Users/kd/git/Tools/spark-3.5.0-bin-hadoop3-scala2.13")
                .setAppResource("s3a:/" + reportUrl.getFile())
                .setConf("spark.hadoop.fs.s3a.access.key", "rustfsadmin")
                .setConf("spark.hadoop.fs.s3a.secret.key", "rustfsadmin")
                .setConf("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
                .setConf("spark.executor.instances", "1")
                .setConf("spark.kubernetes.executor.deleteOnTermination", "false")
                .setConf("spark.kubernetes.container.image", "pyspark-iceberg:latest")
                .setConf("spark.kubernetes.container.image.pullPolicy", "IfNotPresent")
                .setConf("spark.kubernetes.namespace", "spark")
                .setConf("spark.kubernetes.authenticate.driver.serviceAccountName", "spark-sa")
                .setConf("spark.hadoop.fs.s3a.endpoint", "http://rustfs.orb.local:9000")
                .setConf("spark.hadoop.fs.s3a.path.style.access", "true")
                .setConf("spark.hadoop.fs.s3a.connection.ssl.enabled", "false")
                .setConf("spark.hadoop.fs.s3a.endpoint.region", "us-east-1")
                .setConf("spark.kubernetes.file.upload.path", "local:///tmp")
                .setConf("spark.kubernetes.driver.container.image.entrypoint.executable", "/bin/sh")
                .setConf("spark.kubernetes.executor.container.image.entrypoint.executable", "/bin/sh")
                .setConf("spark.kubernetes.driverEnv.PYSPARK_PRIMARY_RESOURCE", "s3a:/" + reportUrl.getFile());
        SparkAppHandle handle = launcher.startApplication();

        handle.addListener(new SparkAppHandle.Listener() {
            @Override
            public void stateChanged(SparkAppHandle h) {
                log.info("Logging the state {}", h.getState());
                log.info("Logging the app id {}", h.getAppId());

                h.getError()
                        .ifPresentOrElse(error -> {

                                    log.error("unable to process the file: due to error: {}", error.getMessage());
                                    log.info("sending acknowledgement after failure");
                                },
                                () -> {
                                    log.info("file processes successfully for file ");
                                });
            }

            @Override
            public void infoChanged(SparkAppHandle h) {
            }
        });


    }
}
