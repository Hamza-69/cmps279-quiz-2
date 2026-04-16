import base64
import json
import re
from datetime import date, datetime, time, timezone

from bson import ObjectId
from fastapi import HTTPException

from models.book import Category
from services.config import books_collection
from services.upload import upload_base64_image


def get_object_id(id: str) -> ObjectId:
    try:
        return ObjectId(id)
    except (TypeError, Exception):
        raise HTTPException(status_code=422, detail=f"Invalid ID: '{id}'")


def resolve_cover_image(cover_image: str | None) -> str | None:
    if cover_image is None:
        return None
    normalized = cover_image.strip()
    if not normalized:
        return None
    if normalized.startswith(("http://", "https://")):
        return normalized
    try:
        return upload_base64_image(normalized, folder="covers")
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc))
    except RuntimeError as exc:
        raise HTTPException(status_code=502, detail=str(exc))


def normalize_mongo_date_fields(payload: dict, fields: tuple[str, ...]) -> None:
    for field in fields:
        value = payload.get(field)
        if isinstance(value, date) and not isinstance(value, datetime):
            payload[field] = datetime.combine(value, time.min, tzinfo=timezone.utc)


def encode_cursor(sort_field, sort_order, sort_value, last_id) -> str:
    payload = {
        "sort_field": sort_field,
        "sort_order": sort_order,
        "sort_value": sort_value,
        "last_id": last_id,
    }
    return base64.urlsafe_b64encode(json.dumps(payload).encode()).decode()


def decode_cursor(cursor: str) -> dict:
    try:
        padded_cursor = cursor + ("=" * (-len(cursor) % 4))
        cursor_data = json.loads(
            base64.urlsafe_b64decode(padded_cursor.encode()).decode()
        )
        required_keys = {"sort_field", "sort_value", "last_id"}
        if not required_keys.issubset(cursor_data):
            raise ValueError("Missing cursor fields")
        return cursor_data
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid cursor")


def build_cursor_filter(cursor_data: dict, sort_order: str) -> dict:
    sf, sv = cursor_data["sort_field"], cursor_data["sort_value"]
    try:
        lid = ObjectId(cursor_data["last_id"])
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid cursor")
    if sort_order == "asc":
        return {"$or": [{sf: {"$gt": sv}}, {sf: sv, "_id": {"$gt": lid}}]}
    else:
        return {"$or": [{sf: {"$lt": sv}}, {sf: sv, "_id": {"$lt": lid}}]}


def parse_category_filter(raw_category_filter: str) -> list[str] | None:
    categories = [part.strip() for part in re.split(r"[|,]", raw_category_filter) if part.strip()]
    if not categories:
        raise HTTPException(status_code=422, detail="category_filter_by is empty")
    if any(category.lower() == "all" for category in categories):
        return None
    allowed = {category.value for category in Category}
    invalid_categories = [category for category in categories if category not in allowed]
    if invalid_categories:
        raise HTTPException(
            status_code=422,
            detail=f"Invalid category_filter_by values: {', '.join(invalid_categories)}",
        )
    return categories


def run_regex_search(
    filter_dict: dict, query: str, sort_field: str, sort_dir: int, limit: int
) -> list:
    regex = {"$regex": re.escape(query), "$options": "i"}
    text_filter = {
        "$or": [
            {"search_text": regex},
            {"title": regex},
            {"author": regex},
            {"description": regex},
            {"category": regex},
        ]
    }
    pipeline = [
        {
            "$match": {"$and": [filter_dict, text_filter]}
            if filter_dict
            else text_filter
        },
        {"$sort": {sort_field: sort_dir, "_id": sort_dir}},
        {"$limit": limit + 1},
    ]
    return list(books_collection.aggregate(pipeline))
