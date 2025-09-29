from sqlalchemy import Column, Integer, String, ForeignKey, Boolean
from sqlalchemy.orm import relationship
from app.database import Base

class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String(255), unique=True, index=True)
    password = Column(String(255))

class VPNConfig(Base):
    __tablename__ = "vpn_configs"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), unique=True)
    private_key = Column(String(255))
    public_key = Column(String(255))
    address = Column(String(255))
    dns = Column(String(255))
    endpoint = Column(String(255))
    allowed_ips = Column(String(255))

    user = relationship("User")

class VPNServer(Base):
    __tablename__ = "vpn_servers"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255), unique=True, index=True)
    location = Column(String(255))
    endpoint = Column(String(255), unique=True)
    load = Column(Integer, default=0)
    active = Column(Boolean, default=True)

