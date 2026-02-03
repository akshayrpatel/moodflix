from contextlib import asynccontextmanager
from fastapi import FastAPI
from starlette.middleware.cors import CORSMiddleware

from app.api.router import router
from app.config.logging_config import configure_logging
from app.services.service_registry import init_services, shutdown_services

@asynccontextmanager
async def lifespan(app: FastAPI) -> None:
	await init_services()
	yield
	await shutdown_services()

app = FastAPI(title="Cinematch AI Engine", lifespan=lifespan)

origins = [
    "http://localhost:3000",
    "http://127.0.0.1:3000",
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

configure_logging()
app.include_router(router)

@app.get("/")
def root():
	return {"status": "cinematch-ai-engine running"}

@app.get("/health")
def health():
	return {"status": "healthy"}


if __name__ == "__main__":
	import uvicorn
	uvicorn.run("main:app", host="0.0.0.0", port=8001)
