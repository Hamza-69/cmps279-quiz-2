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
  cover_image: str
  
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
  cover_image: str

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
  sorted_by: SortBy | None = None
  sort_order: SortOrder | None = None
  category_filter_by: List[Category] | None = None
  year_from: int | None = None
  year_to: int | None = None
  cursor: str | None = None
  limit: int | None = None
