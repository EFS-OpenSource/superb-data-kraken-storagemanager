# 0002 Enable Azure Blob Storage "soft delete"

Date: 2023-08-18

## Status

**<u>Proposed</u>** | Accepted | Reverted

## Context

We want to protect Blob Storage against accidential deletion of data, however we **don't** want to backup all our data. 

## Decision

We will enable soft deletion for blobs and containers. To avoid inconsistencies, we enable versioning.

## Consequences

Enabling soft deletion in Azure Blob Storage can have several consequences:

* Protection against accidental deletes: Soft deletion provides protection against accidental or malicious deletion of blobs and snapshots. When you delete a blob or snapshot, it is moved to a soft-deleted state instead of being permanently deleted.
* Retention period: When you enable soft deletion, blobs and snapshots are retained for a specified retention period that was in effect when they were deleted. During this period, you can recover the deleted data.
* Storage space: Soft-deleted blobs and snapshots continue to consume storage space until they are permanently deleted. You can use the Compact blob store task to permanently delete these soft-deleted blobs and free up the used storage space.
* Cost: Enabling soft deletion may increase the storage costs associated with your storage account. This is because the soft-deleted blobs and snapshots continue to consume storage space until they are permanently deleted.

Overall, enabling soft deletion provides an additional layer of protection against accidental or malicious deletion of blobs and snapshots in Azure Blob Storage. However, it is important to consider the potential impact on storage space and costs when enabling this feature.

