import os
from dotenv import load_dotenv
from pymongo import MongoClient
from boto3 import client

# Load variables from .env
load_dotenv()

# Use variables
aws_access_key_id = os.getenv('AWS_ACCESS_KEY_ID')
aws_secret_access_key = os.getenv('AWS_SECRET_ACCESS_KEY')
aws_endpoint_url = os.getenv('AWS_ENDPOINT_URL_S3')
aws_region = os.getenv('AWS_REGION')
bucket_name = os.getenv('BUCKET_NAME')
database_url = os.getenv('DATABASE_URL')

db_client = MongoClient(database_url)
db = db_client["books_db"]
books_collection = db["books"]

s3_client = client(
    's3',
    endpoint_url=aws_endpoint_url,
    aws_access_key_id=aws_access_key_id,
    aws_secret_access_key=aws_secret_access_key,
    region_name=aws_region
)