import base64
import json
import re
from datetime import datetime, timezone

from bson import ObjectId
from fastapi import APIRouter, Depends, HTTPException, Response, status
from pymongo import ASCENDING, DESCENDING, ReturnDocument
from pymongo.errors import OperationFailure

from models.book import (
    Book,
    Category,
    BookCreate,
    BookListResponse,
    BookSearch,
    BookUpdate,
    serialize_book,
)
from services.config import books_collection
from services.search import embed
from services.upload import upload_base64_image

router = APIRouter(
    prefix="/books",
    tags=["books"],
    responses={404: {"description": "Not found"}},
)


def get_object_id(id: str) -> ObjectId:
    try:
        return ObjectId(id)
    except (TypeError, Exception):
        raise HTTPException(status_code=422, detail=f"Invalid ID: '{id}'")


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


@router.post("/", response_model=Book, status_code=201, summary="Create a new book")
def create_book(book: BookCreate):
    if book.cover_image:
        book.cover_image = upload_base64_image(book.cover_image, folder="covers")
    now = datetime.now(timezone.utc)
    search_text = f"{book.title} {book.description} {book.author} {book.category.value}"
    doc = {
        "title": book.title,
        "description": book.description,
        "author": book.author,
        "year": book.year,
        "category": book.category.value,
        "cover_image": cover_url if (cover_url := book.cover_image) else None,
        "search_text": search_text,
        "embedding": embed(search_text),
        "is_borrowed": False,
        "due_date": None,
        "borrow_date": None,
        "created_at": now,
        "updated_at": now,
    }
    result = books_collection.insert_one(doc)
    return Book(
        **serialize_book(books_collection.find_one({"_id": result.inserted_id}))
    )


@router.get(
    "/",
    response_model=BookListResponse,
    summary="Search books with sorting, filtering and pagination",
)
def get_books(params: BookSearch = Depends()):
    limit = params.limit
    sort_field = params.sorted_by.value
    sort_order = params.sort_order.value
    sort_dir = ASCENDING if sort_order == "asc" else DESCENDING
    query = params.query.strip() if params.query else None

    base_filter = {}
    if params.category_filter_by:
        parsed_categories = parse_category_filter(params.category_filter_by)
        if parsed_categories:
            base_filter["category"] = {"$in": parsed_categories}
    if params.year_from is not None or params.year_to is not None:
        year_filter = {}
        if params.year_from is not None:
            year_filter["$gte"] = params.year_from
        if params.year_to is not None:
            year_filter["$lte"] = params.year_to
        base_filter["year"] = year_filter

    cursor_clause = None
    if params.cursor:
        cursor_data = decode_cursor(params.cursor)
        if cursor_data.get("sort_field") != sort_field:
            raise HTTPException(
                status_code=400, detail="Cursor does not match current sort field"
            )
        if cursor_data.get("sort_order") and cursor_data["sort_order"] != sort_order:
            raise HTTPException(
                status_code=400, detail="Cursor does not match current sort order"
            )
        cursor_clause = build_cursor_filter(cursor_data, sort_order)

    filter_dict = base_filter
    if cursor_clause:
        filter_dict = (
            {"$and": [base_filter, cursor_clause]} if base_filter else cursor_clause
        )

    if query:
        vector_limit = min(max((limit + 1) * 5, 120), 1000)
        num_candidates = min(max(vector_limit * 10, 400), 10000)
        vector_stage = {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": embed(query),
                "numCandidates": num_candidates,
                "limit": vector_limit,
            }
        }
        if base_filter:
            vector_stage["$vectorSearch"]["filter"] = base_filter

        pipeline = [
            vector_stage,
            {"$addFields": {"_score": {"$meta": "vectorSearchScore"}}},
            {"$match": {"_score": {"$gte": 0.55}}},
            *([{"$match": cursor_clause}] if cursor_clause else []),
            {"$sort": {sort_field: sort_dir, "_id": sort_dir}},
            {"$limit": limit + 1},
        ]
        try:
            raw = list(books_collection.aggregate(pipeline))
        except OperationFailure:
            raw = run_regex_search(filter_dict, query, sort_field, sort_dir, limit)
    else:
        pipeline = [
            {"$match": filter_dict},
            {"$sort": {sort_field: sort_dir, "_id": sort_dir}},
            {"$limit": limit + 1},
        ]
        raw = list(books_collection.aggregate(pipeline))

    has_next = len(raw) > limit
    page = raw[:limit]
    next_cursor = None
    if has_next and page:
        last = page[-1]
        next_cursor = encode_cursor(
            sort_field, sort_order, last[sort_field], str(last["_id"])
        )

    return BookListResponse(
        books=[Book(**serialize_book(b)) for b in page], next_cursor=next_cursor
    )


@router.get("/{id}", response_model=Book, summary="Get a book by ID")
def get_book(id: str):
    oid = get_object_id(id)
    doc = books_collection.find_one({"_id": oid})
    if not doc:
        raise HTTPException(status_code=404, detail="Book not found")
    return Book(**serialize_book(doc))


@router.put("/{id}", response_model=Book, summary="Update a book by ID")
def update_book(id: str, updates: BookUpdate):
    oid = get_object_id(id)
    update_data = updates.model_dump(exclude_none=True)
    if not update_data:
        raise HTTPException(status_code=400, detail="No fields to update")

    if "cover_image" in update_data:
        update_data["cover_image"] = upload_base64_image(
            update_data["cover_image"], folder="covers"
        )

    if {"title", "description", "author", "category"} & update_data.keys():
        existing = books_collection.find_one({"_id": oid})
        if not existing:
            raise HTTPException(status_code=404, detail="Book not found")
        merged = {
            "title": update_data.get("title", existing["title"]),
            "description": update_data.get("description", existing["description"]),
            "author": update_data.get("author", existing["author"]),
            "category": update_data.get("category", existing["category"]),
        }
        cat = merged["category"]
        new_search_text = (
            f"{merged['title']} {merged['description']} "
            f"{merged['author']} {cat.value if hasattr(cat, 'value') else cat}"
        )
        update_data["search_text"] = new_search_text
        update_data["embedding"] = embed(new_search_text)

    update_data["updated_at"] = datetime.now(timezone.utc)
    updated = books_collection.find_one_and_update(
        {"_id": oid},
        {"$set": update_data},
        return_document=ReturnDocument.AFTER,
    )
    if not updated:
        raise HTTPException(status_code=404, detail="Book not found")
    return Book(**serialize_book(updated))


@router.delete("/{id}", status_code=204, summary="Delete a book by ID")
def delete_book(id: str):
    oid = get_object_id(id)
    result = books_collection.delete_one({"_id": oid})
    if result.deleted_count == 0:
        raise HTTPException(status_code=404, detail="Book not found")
    return Response(status_code=status.HTTP_204_NO_CONTENT)
