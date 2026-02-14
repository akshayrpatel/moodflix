import logging

from app.services.query_service import QueryService
from app.services.discover_service import DiscoverService
from app.services.onboarding_service import OnboardingService

logger = logging.getLogger(__name__)

class ServiceRegistry:
    """
    Registry for globally accessible service instances.

    Holds singletons for:
      - QueryService (embedding + ChromaDB search)
      - DiscoverService (personalized dashboard rows)
    """
    query: QueryService | None = None
    discover: DiscoverService | None = None
    onboarding: OnboardingService | None = None


service_registry = ServiceRegistry()


async def init_services() -> None:
    """
    Initialize all core services, call during startup.
    """
    logger.info("[ServiceRegistry] Initializing services")
    service_registry.query = QueryService()
    # Ensure ChromaDB collection is initialized before DiscoverService needs it
    service_registry.query._initialize()
    service_registry.discover = DiscoverService(
        collection=service_registry.query.collection,
    )
    service_registry.onboarding = OnboardingService()
    logger.info("[ServiceRegistry] Services initialized")


async def shutdown_services() -> None:
    """
    Shutdown / cleanup services if needed.
    """
    logger.info("[ServiceRegistry] Shutting down services")
    logger.info("[ServiceRegistry] Services shut down")
