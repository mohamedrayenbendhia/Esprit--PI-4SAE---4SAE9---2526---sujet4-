import logging
import py_eureka_client.eureka_client as eureka_client

logger = logging.getLogger(__name__)


async def register_with_eureka(app_name: str, port: int, eureka_url: str):
    """Enregistre le service auprès d'Eureka Server."""
    try:
        await eureka_client.init_async(
            eureka_server=eureka_url,
            app_name=app_name,
            instance_port=port,
            instance_host="127.0.0.1",
            ha_strategy=eureka_client.HA_STRATEGY_RANDOM,
        )
        logger.info(
            "Service '%s' enregistré sur Eureka (%s) au port %d",
            app_name, eureka_url, port,
        )
    except Exception as e:
        logger.error("Échec de l'enregistrement Eureka: %s", e)


async def deregister_from_eureka():
    """Désinscrit le service d'Eureka."""
    try:
        await eureka_client.stop_async()
        logger.info("Service désinscrit d'Eureka")
    except Exception as e:
        logger.warning("Erreur lors de la désinscription Eureka: %s", e)
