from fastapi import APIRouter

router = APIRouter(
    prefix="/books",
    tags=["books"],
    responses={404: {"description": "Not found"}},
)

@router.post("/", summary="Create a new book")
def create_book():
    return {"message": "Book created"}
    
@router.get("/", summary="Search books with sorting, filtering and pagination")
def get_books():
    return {"message": "List of books"}
    
@router.get("/{id}", summary="Get a book by ID")
def get_book(id: str):
    return {"message": f"Book with ID {id}"}
    
@router.put("/{id}", summary="Update a book by ID")
def update_book(id: str):
    return {"message": f"Book with ID {id} updated"}
    
@router.delete("/{id}", summary="Delete a book by ID")
def delete_book(id: str):
    return {"message": f"Book with ID {id} deleted"}