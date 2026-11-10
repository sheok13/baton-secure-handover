FROM python:3.12-slim
COPY --from=ghcr.io/astral-sh/uv:0.12.23 /uv /uvx /bin/
WORKDIR /app

# UID 고정: EC2에서 bind mount를 쓸 때 chown 1000:1000으로 맞추기 쉬움
RUN useradd -m -u 1000 aiapp \
 && mkdir -p /data/chroma \
 && chown -R aiapp:aiapp /app /data/chroma

COPY --chown=aiapp:aiapp pyproject.toml uv.lock ./
USER aiapp
RUN uv sync --locked --no-dev
COPY --chown=aiapp:aiapp . .

ENV VECTOR_STORE_PATH=/data/chroma
EXPOSE 8000
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health', timeout=4)" || exit 1
CMD ["uv", "run", "--no-sync", "uvicorn", "app.api.main:app", "--host", "0.0.0.0", "--port", "8000"]