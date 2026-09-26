package com.kd.etl.designer.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkAppHandle;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class ETLSubmitServiceImpl implements ETLSubmitService {


    @Override
    public void submitEtlService() throws IOException {

        System.setProperty("aws.accessKeyId", "rustfsadmin");
        System.setProperty("aws.secretAccessKey", "rustfsadmin");
        System.setProperty("aws.region", "us-east-1");

        SparkLauncher launcher = new SparkLauncher()
                .setAppName("my first pyspark")
                .setMaster("k8s://https://127.0.0.1:26443")
                .setDeployMode("cluster")
                .setSparkHome("/Users/kd/git/Tools/spark-3.5.0-bin-hadoop3-scala2.13")
                .setAppResource("s3a://scripts/enrich_student.py")
                .setConf("spark.executor.instances", "1")
                .setConf("spark.kubernetes.executor.deleteOnTermination", "false")
                .setConf("spark.kubernetes.container.image", "apache/spark:3.5.4-python3")
                .setConf("spark.kubernetes.container.image.pullPolicy", "IfNotPresent")
                .setConf("spark.kubernetes.namespace", "spark")
                .setConf("spark.kubernetes.authenticate.driver.serviceAccountName", "spark-sa")
                .setConf("spark.kubernetes.file.upload.path", "local:///tmp");

        SparkAppHandle handle = launcher.startApplication();

        // attach a listener to get the status of the spark job
        // if error in the spark job? exit with failure so scaledJob can handle retry : exit with success
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
