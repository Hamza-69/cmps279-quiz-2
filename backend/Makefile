.PHONY: install dev

install:
	uv sync

dev:
	uv run uvicorn main:app --reload

setup-vector-db:
	cd services && uv run python search.py