# Studio API Endpoint List

## Data Controller (`/api/studio/data`)

| Method | Path                | Description            |
| ------ | ------------------- | ---------------------- |
| GET    | `/{modelId}`        | List all records       |
| GET    | `/{modelId}/{id}`   | Get record by ID       |
| POST   | `/{modelId}`        | Create new record      |
| PUT    | `/{modelId}/{id}`   | Update existing record |
| DELETE | `/{modelId}/{id}`   | Delete record          |
| GET    | `/{modelId}/export` | Export data as CSV     |
| POST   | `/{modelId}/import` | Import data from CSV   |

## Flow Controller (`/api/studio/flows`)

| Method | Path                | Description            |
| ------ | ------------------- | ---------------------- |
| GET    | `/{modelId}`        | List flows for a model |
| POST   | `/`                 | Create new flow        |
| PUT    | `/{flowId}`         | Update flow definition |
| DELETE | `/{flowId}`         | Delete flow            |
| POST   | `/{flowId}/execute` | Execute flow manually  |

## Release Controller (`/api/studio/releases`)

| Method | Path         | Description               |
| ------ | ------------ | ------------------------- |
| GET    | `/{modelId}` | List releases for a model |
| POST   | `/`          | Create new release        |

## Modeler Controller (`/api/studio/modeler` - derived from StuApiController)

| Method | Path      | Description                        |
| ------ | --------- | ---------------------------------- |
| GET    | `/models` | List all models (`StuModelHeader`) |
| GET    | `/count`  | Get model count                    |
