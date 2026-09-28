```python
from pyspark.sql import SparkSession
from pyspark.sql.functions import col

# Set Spark configuration
spark = SparkSession.builder \
    .appName("Student Dataflow") \
    .config("spark.jars.packages", "org.apache.iceberg:iceberg-spark-runtime-3.5_2.12:1.11.0,org.apache.iceberg:iceberg-aws-bundle:1.11.0,org.apache.hadoop:hadoop-aws:3.3.4") \
    .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions") \
    .config("spark.sql.catalog.demo", "org.apache.iceberg.spark.SparkCatalog") \
    .config("spark.sql.catalog.demo.type", "rest") \
    .config("spark.sql.catalog.demo.uri", "http://iceberg-rest.orb.local:8181") \
    .config("spark.sql.catalog.demo.s3.endpoint", "http://rustfs.orb.local:9000") \
    .config("spark.sql.catalog.demo.s3.path-style-access", "true") \
    .config("spark.sql.catalog.demo.s3.access-key-id", "rustfsadmin") \
    .config("spark.sql.catalog.demo.s3.secret-access-key", "rustfsadmin") \
    .config("spark.sql.catalog.demo.warehouse", "s3://warehouse/") \
    .config("spark.hadoop.fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem") \
    .config("spark.hadoop.fs.s3a.endpoint", "http://rustfs.orb.local:9000") \
    .config("spark.hadoop.fs.s3a.access.key", "rustfsadmin") \
    .config("spark.hadoop.fs.s3a.secret.key", "rustfsadmin") \
    .config("spark.hadoop.fs.s3a.path.style.access", "true") \
    .config("spark.hadoop.fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider") \
    .getOrCreate()

print(spark.version)



# Read datasets
student_ds = spark.read.csv("s3a://inbound/student_data.csv", header=True)
ref_ds = spark.read.csv("s3a://ref/subject_score_reference.csv", header=True)

# Filter student_ds
passed_student_ds = student_ds.filter(col("score") > 50)

# Left join datasets
enriched_student_ds = passed_student_ds.join(ref_ds, passed_student_ds.score == ref_ds.subject_score, "left")

# Filter enriched_student_ds
aced_student_ds = enriched_student_ds.filter(col("grade") == "A")

# Write to Apache Iceberg table
aced_student_ds.writeTo("demo.my_database.student_v21").create()
```