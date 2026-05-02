import mysql.connector
from mysql.connector import pooling
import logging

logger = logging.getLogger(__name__)

_pool = None

def init_db(config: dict):
    global _pool
    jdbc_url = config.get("spring.datasource.url", "")
    username = config.get("spring.datasource.username", "root")
    password = config.get("spring.datasource.password", "")

    host = "localhost"
    port = 3306
    database = "nexlance_recommendation_ai"

    if "jdbc:mysql://" in jdbc_url:
        try:
            url_part = jdbc_url.replace("jdbc:mysql://", "").split("?")[0]
            if "/" in url_part:
                host_port, database = url_part.split("/", 1)
                if ":" in host_port:
                    host, port_str = host_port.split(":")
                    port = int(port_str)
                else:
                    host = host_port
        except Exception as e:
            logger.warning("Erreur parsing JDBC URL, usage défauts: %s", e)

    try:
        temp_conn = mysql.connector.connect(
            host=host, port=port, user=username, password=password if password else None
        )
        temp_cursor = temp_conn.cursor()
        temp_cursor.execute(f"CREATE DATABASE IF NOT EXISTS {database}")
        temp_cursor.close()
        temp_conn.close()

        _pool = pooling.MySQLConnectionPool(
            pool_name="nexlance_pool",
            pool_size=10,
            host=host,
            port=port,
            database=database,
            user=username,
            password=password if password else None,
            autocommit=True
        )
        _create_tables()
    except Exception as e:
        logger.error("❌ Erreur initiale DB: %s", e)

def _create_tables():
    conn = get_connection()
    if not conn: return
    try:
        cursor = conn.cursor()
        # CHANGEMENT ICI : VARCHAR(36) pour les UUID
        cursor.execute("""
                       CREATE TABLE IF NOT EXISTS freelancer_profiles (
                                                                          id VARCHAR(36) PRIMARY KEY,
                           skills TEXT,
                           experience_years INT DEFAULT 0,
                           hourly_rate DECIMAL(10,2) DEFAULT 0,
                           rating DECIMAL(3,2) DEFAULT 0,
                           completed_projects INT DEFAULT 0,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                           )
                       """)

        cursor.execute("""
                       CREATE TABLE IF NOT EXISTS job_offer_profiles (
                                                                         id VARCHAR(36) PRIMARY KEY,
                           required_skills TEXT,
                           budget DECIMAL(12,2) DEFAULT 0,
                           experience_required INT DEFAULT 0,
                           category VARCHAR(100),
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                           )
                       """)

        cursor.execute("""
                       CREATE TABLE IF NOT EXISTS recommendations (
                                                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                                      freelancer_id VARCHAR(36) NOT NULL,
                           job_offer_id VARCHAR(36) NOT NULL,
                           score DECIMAL(5,4) NOT NULL,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           INDEX idx_freelancer (freelancer_id),
                           INDEX idx_joboffer (job_offer_id)
                           )
                       """)
        conn.commit()
    finally:
        conn.close()

def get_connection():
    global _pool
    return _pool.get_connection() if _pool else None