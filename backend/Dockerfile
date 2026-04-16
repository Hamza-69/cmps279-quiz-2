FROM python:3.12 AS builder
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1
WORKDIR /app
RUN python -m venv .venv
COPY pyproject.toml ./
RUN .venv/bin/pip install torch --index-url https://download.pytorch.org/whl/cpu
RUN .venv/bin/pip install . sentence-transformers
RUN .venv/bin/python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('all-MiniLM-L6-v2')"

FROM python:3.12-slim
WORKDIR /app
COPY --from=builder /app/.venv .venv/
COPY --from=builder /root/.cache /root/.cache
COPY . .
CMD ["/app/.venv/bin/fastapi", "run"]