.PHONY: install dev

install:
	uv sync
	
dev:
	uv run uvicorn main:app --reload