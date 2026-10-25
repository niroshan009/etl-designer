FROM spark:3.5.7-java17
LABEL authors="kd"
VOLUME /tmp
WORKDIR /app

ENV PORT=8081
ENV OLLAMA_BASE_URL=http://localhost:11434
ENV OLLAMA_CHAT_MODEL=qwen2.5-coder:7b
ENV OLLAMA_CHAT_TEMPRATURE=0.2
ENV OLLAMA_TOP_P=0.15
ENV S3_ENDPOINT=http://localhost:9001
ENV S3_PATH_STYLE_ENABLED=true
ENV S3_ACCESS_KEY=rustfsadmin
ENV S3_SECRET_KEY=rustfsadmin
ENV S3_REGION=us-east-1
ENV ETL_SPARK_MASTER=k8s://https://127.0.0.1:26443
ENV SPARK_HOME=/opt/spark
ENV ETL_ACCESS_KEY=rustfsadmin
ENV ETL_SECRET_KEY=rustfsadmin
ENV ETL_REGION=us-east-1
ENV ETL_LOCAL_SCRIPT_PATH=/app/resources/scripts
ENV ETL_HADOOP_S3_ACCESS_KEY=rustfsadmin
ENV ETL_HADOOP_S3_SECRET_KEY=rustfsadmin
ENV ETL_HADOOP_S3_ENDPOINT=http://rustfs.orb.local:9000
ENV ETL_HADOOP_S3_REGION=us-east-1
ENV ETL_KUBE_BASE_IMAGE=pyspark-iceberg:latest
ENV ETL_KUBE_NAMESPACE=spark
ENV ETL_KUBE_SERVICE_ACCOUNT=spark-sa
ENV ETL_KUBE_FILE_UPLOAD_PATH=local:///tmp

COPY ./target/etl-designer-0.0.1.jar /app/etl-designer-0.0.1.jar
COPY ./target/resources/application.yaml /app/resources/application.yaml

USER root
RUN mkdir -p /app/resources/scripts && chown -R 1000:1000 /app/resources
USER 1000

ENTRYPOINT java -jar /app/etl-designer-0.0.1.jar
