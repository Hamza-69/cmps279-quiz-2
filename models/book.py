from enum import Enum
from pydantic import BaseModel
from datetime import datetime, date
from typing import List

class Category(str, Enum):
  Fiction = "Fiction"
  NonFiction = "Non-Fiction"
  Science = "Science"
  History = "History"
  Biography = "Biography"
  Fantasy = "Fantasy"
  Mystery = "Mystery"
  Romance = "Romance"
  
class SortBy(str, Enum):
  Title = "title"
  Author = "author"
  Year = "year"

class SortOrder(str, Enum):
  Ascending = "asc"
  Descending = "desc"

class Book(BaseModel):
  id: str
  
  title: str
  description: str
  
  author: str
  year: int
  category: Category
  cover_image: str | None = None
  
  is_borrowed: bool = False
  due_date: date | None = None
  borrow_date: date | None = None
  
  created_at: datetime
  updated_at: datetime
  
class BookCreate(BaseModel):
  title: str
  description: str
  author: str
  year: int
  category: Category
  cover_image: str | None = None

class BookUpdate(BaseModel):
  title: str | None = None
  description: str | None = None
  author: str | None = None
  year: int | None = None
  category: Category | None = None
  cover_image: str | None = None
  is_borrowed: bool | None = None
  due_date: date | None = None
  borrow_date: date | None = None
  
class BookSearch(BaseModel):
  query: str | None = None
  sorted_by: SortBy = SortBy.Title
  sort_order: SortOrder = SortOrder.Ascending
  category_filter_by: List[Category] | None = None
  year_from: int | None = None
  year_to: int | None = None
  cursor: str | None = None
  limit: int | None = None

def serialize_book(book) -> dict:
  return {
      "id": str(book["_id"]),
      "title": book["title"],
      "description": book["description"],
      "author": book["author"],
      "year": book["year"],
      "category": book["category"],
      "cover_image": book["cover_image"],
      "is_borrowed": book["is_borrowed"],
      "due_date": book["due_date"].isoformat() if book["due_date"] else None,
      "borrow_date": book["borrow_date"].isoformat() if book["borrow_date"] else None,
      "created_at": book["created_at"].isoformat(),
      "updated_at": book["updated_at"].isoformat(),
  }

class BookListResponse(BaseModel):
  books: List[Book]
  next_cursor: str | None = None
