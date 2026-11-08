FROM python:3.12-slim
COPY --from=ghcr.io/astral-sh/uv:0.12.23 /uv /uvx /bin/
WORKDIR /app
RUN useradd -m aiapp
COPY pyproject.toml uv.lock ./
RUN uv sync --locked --no-dev
COPY . .
RUN chown -R aiapp:aiapp /app
USER aiapp
EXPOSE 8000
HEALTHCHECK --interval=30s --timeout=5s CMD curl -f http://localhost:8000/health || exit 1
CMD ["uv", "run", "uvicorn", "app.api.main:app", "--host", "0.0.0.0", "--port", "8000"]