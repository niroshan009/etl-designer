package com.kd.etl.designer.service;

import com.kd.etl.designer.config.ETLProperties;
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

    @Autowired
    private ETLProperties etlProperties;

    @Override
    public void submitEtlService(SparkSubmitJobRequest sparkSubmitJobRequest) throws IOException {

        System.setProperty("aws.accessKeyId", etlProperties.getAccessKey());
        System.setProperty("aws.secretAccessKey", etlProperties.getSecretKey());
        System.setProperty("aws.region", etlProperties.getEndpointRegion());

        String scriptName = sparkSubmitJobRequest.getScriptName() == null || sparkSubmitJobRequest.getScriptName().isBlank() ?
                "temp_script" : sparkSubmitJobRequest.getScriptName();


        Path outputPath = Paths.get(System.getProperty("user.dir"))
                .resolve(etlProperties.getScriptOutputPath())
                .resolve(scriptName.replace(" ", "_") + ".MD");

        String filContent = Files.readString(outputPath).replaceAll("(?m)^```python\\s*$", "")
                .replaceAll("(?m)^```\\s*$", "");

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket("scripts")
                .key(scriptName.replace(" ", "_") + ".py")
                .contentType("text/x-python")
                .build();

        PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, RequestBody.fromString(filContent));

        URL reportUrl = s3Client.utilities().getUrl(GetUrlRequest.builder()
                .bucket("scripts")
                .key(scriptName.replace(" ", "_") + ".py")
                .build());

        log.info("submitting script {}", reportUrl.getFile());

        SparkLauncher launcher = new SparkLauncher()
                .setAppName(reportUrl.getFile())
                .setMaster(etlProperties.getSparkMaster())
                .setDeployMode("cluster")
                .setSparkHome(etlProperties.getSparkHome())
                .setAppResource("s3a:/" + reportUrl.getFile())
                .setConf("spark.hadoop.fs.s3a.access.key", etlProperties.getHadoop().getS3().getAccessKey())
                .setConf("spark.hadoop.fs.s3a.secret.key", etlProperties.getHadoop().getS3().getSecretKey())
                .setConf("spark.hadoop.fs.s3a.endpoint", etlProperties.getHadoop().getS3().getEndpoint())
                .setConf("spark.hadoop.fs.s3a.path.style.access", "true")
                .setConf("spark.hadoop.fs.s3a.connection.ssl.enabled", etlProperties.getHadoop().getS3().getSslEnabled())
                .setConf("spark.hadoop.fs.s3a.endpoint.region", etlProperties.getHadoop().getS3().getEndpointRegion())
                .setConf("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")

                .setConf("spark.executor.instances", "1")

                .setConf("spark.kubernetes.executor.deleteOnTermination", "false")
                .setConf("spark.kubernetes.container.image", etlProperties.getKube().getContainerImage())
                .setConf("spark.kubernetes.container.image.pullPolicy", "IfNotPresent")
                .setConf("spark.kubernetes.namespace", etlProperties.getKube().getNamespace())
                .setConf("spark.kubernetes.authenticate.driver.serviceAccountName", etlProperties.getKube().getServiceAccountName())
                .setConf("spark.kubernetes.file.upload.path", etlProperties.getKube().getFileUploadPath())
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
