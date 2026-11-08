# ETL Designer
This project helps to generate pySpark code based on the diagram users design using ETL-designer UI.
Furthermore, this project helps to submit the generated pySpark script to the Kubernetes cluster

## ✨ Features
This project has the following features
* Generate code using local LLM based on the diagram
* Submitting pySpark code to the Kubernetes cluster to validate ETL flow is working as expected

## 📦 Getting Started
This project depends on the etl-desginer-ui project. 


## 🔨 Building

### Building artifact
First project with below command to build artifact and copy the resource folder

```bash
mvn clean package
```


### Building docker image
build docker image with below command
```bash
docker build -t local/etl-designer:latest .
docker tag local/etl-designer:latest niroshan009/etl-designer:latest
docker push niroshan009/etl-designer:latest
```

## ▶️ Running application

```
docker push niroshan009/etl-designer:latest
```



```dockerfile
docker build -t pyspark-iceberg:lates .
docker tag niroshan009/pyspark-iceberg:latest niroshan009/pyspark-iceberg:latest
docker push niroshan009/pyspark-iceberg:latest

```

