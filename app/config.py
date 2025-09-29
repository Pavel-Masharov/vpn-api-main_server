import os
from dotenv import load_dotenv

# Загружаем переменные из .env файла
load_dotenv()

# Конфигурация для подключения к базе данных
DATABASE_URL = os.getenv("DATABASE_URL", "mysql+pymysql://vpn_user:gHgdeb4*vve@localhost/vpn_service")

# Конфигурация JWT
SECRET_KEY = os.getenv("SECRET_KEY", "4bfae13398a2d5b0aef732aa1c0b3d6c4e7f8a92c123456789abcdef12345678")
ALGORITHM = os.getenv("ALGORITHM", "HS256")
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", 60))

# URL для VPN API (если нужно)
VPN_API_URL = os.getenv("VPN_API_URL", "http://localhost:5000")

