# Hytale Performance Saver Plugin
This plugin ensures stability of a Hytale server by lowering the resource consumption of the server when it is under
resource pressure.

## Purpose of this Plugin
The resource usage of a Hytale server, even without mods, can fluctuate heavily based on player behavior. For example,
a large group of players in a small area has a relatively small resource footprint, whereas a small amount of players
each independently exploring the world can cause a significant amount of CPU load and RAM consumption.

It is not always reasonable or cost-effective to run a Hytale game server on the hardware specifications it _might_
need when players do unexpected things, since that means that those hardware resources will remain unused for the vast
majority of time. However, simply running a game server on limited resources may lead to bad performance or server
crashes in these rare high load scenarios.

This plugin's primary goal is to handle resource pressure in an intelligent manner that keeps the game enjoyable for
players.

## Main Features
The plugin takes the following measures to optimize resource usage:

### TPS Limiting
Based on how networking and client prediction work, lower, but stable TPS is generally better for the player
experience than high, but fluctuating TPS. The plugin allows to limit the server TPS to a configurable amount
(default: 20 TPS).

The plugin also limits the TPS of a server that is empty (default: 5).

### Dynamic View Radius Adjustment
The plugin detects CPU pressure through low TPS, and RAM pressure by observing the JVM's garbage collection attempts.
If either resource is under pressure, the view radius is dynamically adjusted to free up those resources again. When
the resources recover, the view radius is gradually increased again. This measure is able to prevent resource-related
server crashes even under stress test scenarios.

### Additional Garbage Collection
Java generally does not free up unused memory on its own. This plugin therefore observes the number of loaded chunks
and explicitly triggers an additional garbage collection if it is highly likely that memory can be freed up.

## Usage

### Installation
Place the plugin JAR into your server's `plugins/` folder.

### Configuration
The plugin is configured via a JSON configuration file. Below are all available configuration sections and their options.

#### TPS Adjuster (`Tps`)
Controls the server's TPS limiting behavior.

| Option                   | Type     | Default | Description                                                     |
|--------------------------|----------|---------|-----------------------------------------------------------------|
| `Enabled`                | boolean  | `true`  | Enable/disable TPS limiting                                     |
| `TpsLimit`               | integer  | `20`    | Maximum TPS when players are online                             |
| `TpsLimitEmpty`          | integer  | `5`     | TPS limit when no players are online                            |
| `OnlyWorlds`             | string[] | `[]`    | Restrict TPS adjustment to specific worlds (empty = all worlds) |
| `InitialDelaySeconds`    | integer  | `30`    | Delay before TPS adjustment starts                              |
| `CheckIntervalSeconds`   | integer  | `5`     | How often to check/adjust TPS                                   |
| `EmptyLimitDelaySeconds` | integer  | `300`   | Delay before applying empty server TPS limit                    |

#### View Radius (`ViewRadius`)
Controls dynamic view radius adjustment based on resource pressure.

| Option                    | Type    | Default | Description                                            |
|---------------------------|---------|---------|--------------------------------------------------------|
| `Enabled`                 | boolean | `true`  | Enable/disable dynamic view radius adjustment          |
| `MinViewRadius`           | integer | `2`     | Minimum allowed view radius                            |
| `DecreaseFactor`          | double  | `0.75`  | Factor to multiply current view radius when decreasing |
| `IncreaseValue`           | integer | `1`     | Amount to increase view radius when recovering         |
| `InitialDelaySeconds`     | integer | `30`    | Delay before view radius adjustment starts             |
| `CheckIntervalSeconds`    | integer | `5`     | How often to check resource pressure                   |
| `RecoveryWaitTimeSeconds` | integer | `60`    | Time to wait before attempting to increase view radius |

##### GC Monitor (`ViewRadius.GcMonitor`)
Monitors JVM garbage collection to detect memory pressure.

| Option                  | Type    | Default | Description                                                     |
|-------------------------|---------|---------|-----------------------------------------------------------------|
| `Enabled`               | boolean | `true`  | Enable/disable GC-based pressure detection                      |
| `HeapThresholdRatio`    | double  | `0.85`  | Heap usage ratio threshold to trigger pressure detection        |
| `TriggerSequenceLength` | integer | `3`     | Number of consecutive high-heap GC events to trigger adjustment |
| `WindowSeconds`         | integer | `60`    | Time window for analyzing GC events                             |

##### TPS Monitor (`ViewRadius.TpsMonitor`)
Monitors TPS to detect CPU pressure.

| Option                   | Type     | Default | Description                                                     |
|--------------------------|----------|---------|-----------------------------------------------------------------|
| `Enabled`                | boolean  | `true`  | Enable/disable TPS-based pressure detection                     |
| `TpsWaterMarkHigh`       | double   | `0.75`  | TPS ratio above which view radius can recover                   |
| `TpsWaterMarkLow`        | double   | `0.6`   | TPS ratio below which view radius should decrease               |
| `OnlyWorlds`             | string[] | `[]`    | Restrict TPS monitoring to specific worlds (empty = all worlds) |
| `AdjustmentDelaySeconds` | integer  | `20`    | Delay between TPS-triggered adjustments                         |

#### Chunk Garbage Collection (`ChunkGarbageCollection`)
Triggers JVM garbage collection when chunk unloading indicates memory can be freed.

| Option                          | Type    | Default | Description                                            |
|---------------------------------|---------|---------|--------------------------------------------------------|
| `Enabled`                       | boolean | `true`  | Enable/disable chunk-based GC triggering               |
| `MinChunkCount`                 | integer | `128`   | Minimum chunk count before GC triggering is considered |
| `ChunkDropRatioThreshold`       | double  | `0.8`   | Chunk reduction ratio threshold to trigger GC          |
| `GarbageCollectionDelaySeconds` | integer | `300`   | Minimum time between triggered GC runs                 |
| `InitialDelaySeconds`           | integer | `5`     | Delay before chunk GC monitoring starts                |
| `CheckIntervalSeconds`          | integer | `5`     | How often to check chunk counts                        |

## Contributing
Community contributions are welcome and encouraged.

### Security
If you believe to have found a security vulnerability, please report your findings via security@nitrado.net.