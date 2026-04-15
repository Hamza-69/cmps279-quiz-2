from fastapi import FastAPI
from routers.book import router as books_router

app = FastAPI(title="Books API with MongoDB Storage")

app.include_router(books_router)

@app.get("/")
def read_root():
    return {"message": "Welcome to the Books API!"}