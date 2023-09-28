# StorageManager Service


The StorageManager service offers a JSON-based, RESTful API designed to manage storage contexts for organizations and spaces.

It serves as an interface to the underlying cloud storage system and manages the creation and removal of the storage context, which consists of all necessary
storage resources (like storage accounts or buckets) essential for spaces and organizations to provide storage capabilities.

This includes the management of storage like Azure Blob Storage or an S3 storage like the Object Storage Service OBS for Open Telekom Cloud or any other
underlying cloud storage used.


## Documentation


This service provisions Storage organization (like Storage Accounts and Containers in Azure or bucket objects on S3 storage).


### Storage Providers


Different storage providers are supported and can be selected using Spring profiles.

- `local`: for local development and testing (uses local file system hierarchy in the system's temporary folder)
- `azure`: Azure Blob Storage (organization = Storage Account, space = Storage Container)

  ![Mapping Azure](/images/adr-0001-mapping-azure.svg)

- `s3`: S3 storage provider like Open Telekom Cloud (OTC). Uses one main bucket as SDK storage (organization = object key prefix `<orga_name>/`, space = object
  key prefix `<orga_name>/<space_name>/`). A virtual folder hierarchy is created in the bucket using object key prefixing with `/` character.

  ![Mapping S3](/images/adr-0001-mapping-s3-option-4.svg)

### Storage- and Accessmanagement


The following graphs illustrate the relationship between accessmanager and storagemanager:

TODO: where graph?

- creating an organization in organizationmanager will create an Azure Blob Storage Account
- creating a space in organizationmanager will create a Blob Container within the Azure Blob Storage Account.

### REST Api


The API is documented using [Swagger](https://swagger.io/) (OpenAPI Specification). Developers may
use [Swagger UI](https://swagger.io/tools/swagger-ui/) to visualize and interact with the API's resources
at `http(s)://(host.domain:port)/storagemanager/swagger-ui/index.html`.


### Configuration


For generating tokens in order to execute certain actions, the following configuration needs to be provided:

```yaml
storagemanager:
  auth:
    client-id:
    client-secret:
```

Where:

- ```storagemanager.auth.client-id``` ID of the confidential(!) OIDC-Client
- ```storagemanager.auth.client-secret``` Secret of the confidential(!) OIDC-Client

Please note that the OIDC-Client needs to be confidential and a service account needs to be activated with according
rights to create and update roles.

For managing Azure Storage the following configuration needs to be provided:

```yaml
storagemanager:
  storage:
    azure:
      cors:
        origins: sdk.efs.ai
        maxAge: 86400
      user:
        client-id:
        client-secret:
        tenant:
        subscription-id:
      region:
      resourcegroup: SDK
```

Where:

- ```storagemanager.storage.azure.user.client-id``` Client-ID of the Service Principal with permissions to manage Azure
  Storage
- ```storagemanager.storage.azure.user.client-secret``` Client-Secret of the Service Principal with permissions to manage
  Azure Storage
- ```storagemanager.storage.azure.user.tenant``` The tenant-ID
- ```storagemanager.storage.azure.user.subscription-id``` The subscription-ID (only required if tenant has multiple
  subscriptions)
- ```storagemanager.storage.azure.region``` The Region where Azure Storage Accounts should be located
- ```storagemanager.storage.azure.resourcegroup``` The Resourcegroup where Azure Storage Accounts should be located
  (defaults to 'SDK')
- ```storagemanager.storage.azure.cors.origins``` allowed origins in storage account's cors (comma-separated list)
- ```storagemanager.storage.azure.cors.maxAge``` maximum age in storage account's cors in seconds

In a production environment, it can happen that the creation of a Storage Account takes a little longer and is therefore
not yet available when the first storage container (loadingzone) is created. It must therefore be ensured that the
creation of containers is retried until the storage account is available. Tests have shown that the creation of storage
containers can take up to 10 minutes. It must therefore be ensured that the product of maxRetries and delay (specified
in seconds) corresponds approximately to these 10 minutes.

```yaml
storagemanager:
  storage:
    azure:
      retry:
        maxRetries: 5
        delay: 10000
```

- ```storagemanager.storage.azure.retry.maxRetries``` Maximum number of retries
- ```storagemanager.storage.azure.retry.delay``` Delay between two retries

For managing an S3 storage the following configuration needs to be provided:

```yaml
storagemanager:
  storage:
    s3:
      endpoint: storage.sdk.example.com
      accessKey: <S3_ACCESS_KEY>
      secretKey: <S3_SECRET_KEY>
      bucketName: sdk-storage
```

Where:

- ```storagemanager.storage.s3.endpoint``` Endpoint to be used by the S3 client
- ```storagemanager.storage.s3.accessKey``` S3 access key used to identifiy the user
- ```storagemanager.storage.s3.secretKey``` S3 secret key used to authenticate the user
- ```storagemanager.storage.s3.bucketName``` Name of the SDK storage bucket

### Local Usage


Copy `src/java/resources/application-local-template.yml` to `src/java/resources/application-local.yml`, configure for environment and start service with spring
boot profile 'local'

The values for the configuration can be found in Kubernetes secrets (see table) and in the config-map for the service (`storagemanager`) .

| property                          | k8s secret                                               |
|-----------------------------------|----------------------------------------------------------|
| storagemanager.auth.client-secret | storagemanager-secret: storagemanager.auth.client-secret |

## Deployment


For deployment push the service to Azure DevOps, the pipeline will start automatically.


### Prerequisites

* Up and running organizationmanager (>= v2.0)
* jdk >= 17
* Setup Maven for Azure Artifacts (https://dev.azure.com/EFSCIS/EFS-SDK/_packaging?_a=connect&feed=sdk-snapshots)
* When using Azure storage:
    * A Service Principal with the following rights:
        * Microsoft.Storage/storageAccounts/Delete
        * Microsoft.Storage/storageAccount/Write
        * Microsoft.Storage/storageAccounts/listKeys/action
        * Microsoft.Storage/storageAccounts/read
* When using S3 storage (i.e. MinIO):
    * A service principal (user with AK/SK) with operator rights on the SDK storage bucket (read/write/delete objects)

## TODO


Currently, the documentation is located in usual files like `README.md`, `CHANGELOG.md`, `CONTRIBUTING.md` and `LICENSE.md` inside the root folder of the
repository. That folder is not processed by MkDocs. To build the technical documentation for MkDocs we could follow these steps:

- Cleanup this document
- Move the documentation to Markdown files inside the `docs` folder.
- Build a proper folder/file structure in `docs` and update the navigation in `mkdocs.yaml`.
- Keep the usual files like `README.md`, `CHANGELOG.md`, `CONTRIBUTING.md` and `LICENSE.md` inside the root folder of the repository (developers expect them to
  be there, especially in open source projects), but keep them short/generic and just refer to the documentation in the `docs` folder.

