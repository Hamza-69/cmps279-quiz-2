from enum import Enum
from pydantic import BaseModel

class Category(str, Enum):
  Fiction = "Fiction"
  NonFiction = "Non-Fiction"
  Science = "Science"
  History = "History"
  Biography = "Biography"
  Fantasy = "Fantasy"
  Mystery = "Mystery"
  Romance = "Romance"

class Book(BaseModel):
  id: str
  
  title: str
  description: str
  
  author: str
  year: int
  category: Category
  cover_image: str
  
  is_borrowed: bool = False
  due_date: str
  borrow_date: str
  
  created_at: str
  updated_at: str