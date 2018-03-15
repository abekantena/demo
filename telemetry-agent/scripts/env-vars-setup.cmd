:: Prepare the environment variables used by the application.

:: Connection string of the DocumentDb instance where telemetry is stored
SETX PCS_TELEMETRYAGENT_DOCUMENTDB_CONNSTRING "..."

:: URL of the telemetry web service
SETX PCS_TELEMETRY_WEBSERVICE_URL "..."

:: URL of the PCS config web service
SETX PCS_CONFIG_WEBSERVICE_URL "..."

:: URL of the IoT Hub manager web service
SETX PCS_IOTHUBMANAGER_WEBSERVICE_URL "..."

:: see: Storage Account ⇒ Access Keys
:: Connection details of the Azure Blob where checkpoints are stored
SETX PCS_IOTHUBREACT_AZUREBLOB_ACCOUNT "..."
SETX PCS_IOTHUBREACT_AZUREBLOB_KEY "..."

:: see: Endpoints ⇒ Messaging ⇒ Events ⇒ "Event Hub-compatible name"
:: e.g. "my-test-hub"
SETX PCS_IOTHUBREACT_HUB_NAME "..."

:: see: Endpoints ⇒ Messaging ⇒ Events ⇒ "Event Hub-compatible endpoint"
:: e.g. "iothub-ns-aaa-bbb-123456-abcdefghij.servicebus.windows.net"
SETX PCS_IOTHUBREACT_HUB_ENDPOINT "..."

:: see: Endpoints ⇒ Messaging ⇒ Events ⇒ Partitions
:: e.g. 4
SETX PCS_IOTHUBREACT_HUB_PARTITIONS "..."

:: see: "IoT Hub" ⇒ your hub ⇒ "Shared access policies"
SETX PCS_IOTHUBREACT_ACCESS_CONNSTRING "..."
