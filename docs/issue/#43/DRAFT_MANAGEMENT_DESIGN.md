# Studio Draft Management & Lifecycle Design

## 1. Concept

Based on the absence of a separate `StuDraft` entity and the presence of `StuRelease`, the lifecycle is defined as:

- **Draft (Working Copy)**: Represented by the current mutable state of `stu_dic_model_header`, `stu_dic_model` (fields), and `stu_flow` tables. "Saving" in the UI updates these tables directly.
- **Release (Published)**: Represented by immutable snapshots stored in `stu_release` table as JSON.
- **Runtime**: Currently "Live Edit" (reads from Draft tables). _Future: Runtime should read from Release snapshots for production stability._

## 2. API Design (RESTful)

### 2.1 Model Management (`/api/studio/models`)

| Method   | Endpoint | Action              | Logic                                                               |
| -------- | -------- | ------------------- | ------------------------------------------------------------------- |
| `GET`    | `/`      | List/Search         | Query `stu_dic_model_header`. Query params: `q` (search), `status`. |
| `POST`   | `/`      | Create              | Insert into `stu_model_header`.                                     |
| `GET`    | `/{id}`  | Get Draft           | Fetch Header + Fields + Flows from mutable tables.                  |
| `PUT`    | `/{id}`  | Update (Save Draft) | Update Header.                                                      |
| `DELETE` | `/{id}`  | Delete              | Delete Header + Fields + Flows (Cascade).                           |

### 2.2 Draft Operations (`/api/studio/models/{id}/...`)

| Method   | Endpoint | Action             | Logic                                                                                            |
| -------- | -------- | ------------------ | ------------------------------------------------------------------------------------------------ |
| `PUT`    | `/draft` | Batch Update Draft | (Optional) Bulk update fields/flows. Same as "Save" but explicit.                                |
| `DELETE` | `/draft` | Discard Draft      | **Restore** tables from the latest `StuRelease` snapshot. If no release, clear to initial state? |

### 2.3 Release Operations (`/api/studio/models/{id}/...`)

| Method | Endpoint    | Action        | Logic                                                                                                                     |
| ------ | ----------- | ------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `POST` | `/publish`  | Publish       | 1. Fetch current tables. 2. Create `StuRelease` JSON snapshot. 3. Save `StuRelease`. 4. Update Header `publishedVersion`. |
| `GET`  | `/releases` | List Releases | Query `stu_release`.                                                                                                      |
| `GET`  | `/validate` | Validate      | Run consistency checks on Draft data (Schema types, Field refs, Flow links).                                              |

## 3. Implementation Plan

1.  **Refactor `StuApiController`**: Replace the single RPC endpoint with `ModelController` implementing the above REST endpoints.
2.  **Implement `ModelService`**: Encapsulate logic for:
    - `publish(modelId)`: Snapshot creation.
    - `discard(modelId)`: Snapshot restoration (JSON -> Entity mapping).
    - `validate(modelId)`: Business logic checks.
3.  **Search**: Add JPAL specification or simple `ByNameContaining` query in repository.

## 4. Migration

- Existing `StuApiController` RPC calls from Frontend (`saveModel`, `listModel`) must be updated to call the new REST endpoints.
- `DynamicEntityService` (Data Browser) can remain as is but terminology should align.
