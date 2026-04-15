import base64
import uuid
from .config import s3_client
from .config import bucket_name, aws_endpoint_url

def parse_base64(base64_string: str):
    if "," in base64_string:
        header, encoded_data = base64_string.split(",", 1)
        content_type = header.split(":")[1].split(";")[0] 
        extension = content_type.split("/")[1]            
    else:
        encoded_data = base64_string
        content_type = "image/jpeg"
        extension = "jpg"
        
    return encoded_data, content_type, extension
    
def upload_base64_image(base64_string: str, folder: str = "misc") -> str:
    """
    Decodes and uploads a Base64 image to S3. 
    Returns the public URL.
    """
    encoded_data, content_type, extension = parse_base64(base64_string)
    
    try:
        image_bytes = base64.b64decode(encoded_data)
    except Exception:
        raise ValueError("Malformed Base64 string")

    file_key = f"{folder}/{uuid.uuid4()}.{extension}"
    
    try:
        s3_client.put_object(
            Bucket=bucket_name,
            Key=file_key,
            Body=image_bytes,
            ContentType=content_type
        )
    except Exception as e:
        raise RuntimeError(f"S3 Upload failed: {str(e)}")

    endpoint = str(aws_endpoint_url).rstrip("/")
    public_url = f"{endpoint}/{bucket_name}/{file_key}"
    
    return public_url