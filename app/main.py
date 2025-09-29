from fastapi import FastAPI
from app.routers import vpn

app = FastAPI()

# Подключаем роутеры
app.include_router(vpn.router)

