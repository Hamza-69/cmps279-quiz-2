from sentence_transformers import SentenceTransformer
from pymongo.operations import SearchIndexModel
from .config import books_collection

model = SentenceTransformer("all-MiniLM-L6-v2")
DIMS = 384


def create_index():
    index_model = SearchIndexModel(
        definition={
            "fields": [
                {
                    "type": "vector",
                    "path": "embedding",
                    "numDimensions": DIMS,
                    "similarity": "cosine",
                }
            ]
        },
        name="vector_index",
        type="vectorSearch",
    )
    try:
        result = books_collection.create_search_index(model=index_model)
        print(f"Index building started: {result}")
    except Exception as e:
        print(f"Already exists or error: {e}")


def embed(text: str) -> list[float]:
    return model.encode(text, normalize_embeddings=True).tolist()


def backfill():
    docs = list(books_collection.find({"embedding": {"$exists": False}}))
    print(f"Embedding {len(docs)} docs...")
    for doc in docs:
        books_collection.update_one(
            {"_id": doc["_id"]},
            {"$set": {"embedding": embed(doc["search_text"])}}
        )
    print("Done.")

if __name__ == "__main__":
    create_index()
    backfill()
