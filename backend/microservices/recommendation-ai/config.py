import os
import requests
import logging

logger = logging.getLogger(__name__)

CONFIG_SERVER_URL = os.getenv("CONFIG_SERVER_URL", "http://localhost:8888")
APP_NAME = os.getenv("APP_NAME", "recommendation-ai")
PROFILE = os.getenv("PROFILE", "default")


def load_config_from_server() -> dict:
    """Charge la configuration depuis Spring Cloud Config Server."""
    url = f"{CONFIG_SERVER_URL}/{APP_NAME}/{PROFILE}"
    try:
        resp = requests.get(url, timeout=5)
        if resp.status_code == 200:
            data = resp.json()
            config = {}
            for source in data.get("propertySources", []):
                config.update(source.get("source", {}))
            logger.info("Configuration chargée depuis Config Server")
            return config
        else:
            logger.warning("Config Server a répondu avec le code %s", resp.status_code)
    except Exception as e:
        logger.warning("Impossible de contacter Config Server: %s", e)
    return {}


# Valeurs par défaut si Config Server est indisponible
DEFAULT_CONFIG = {
    "server.port": 9000,
    "spring.datasource.url": "jdbc:mysql://localhost:3306/nexlance_recommendation_ai?createDatabaseIfNotExist=true",
    "spring.datasource.username": "root",
    "spring.datasource.password": "",
    "eureka.client.service-url.defaultZone": "http://localhost:8761/eureka/",
}


def get_config() -> dict:
    """Retourne la configuration fusionnée (Config Server + défauts)."""
    config = {**DEFAULT_CONFIG}
    remote = load_config_from_server()
    config.update(remote)
    return config
