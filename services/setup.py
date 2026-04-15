from pymongo.operations import SearchIndexModel
from config import books_collection


def create_vector_search_index():
    index_model = SearchIndexModel(
        definition={
            "fields": [
                {
                    "type": "autoEmbed",
                    "modality": "text",
                    "path": "search_text",
                    "model": "voyage-4",
                }
            ]
        },
        name="auto_vector_index",
        type="vectorSearch",
    )
    try:
        result = books_collection.create_search_index(model=index_model)
        print(f"Index building started: {result}")
    except Exception as e:
        print(f"Index creation failed (might already exist): {e}")


if __name__ == "__main__":
    create_vector_search_index()
