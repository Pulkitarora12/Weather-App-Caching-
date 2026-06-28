# Weather App Caching

A Spring Boot weather app built to learn and demonstrate caching — both in-memory and distributed (Redis).

## 1. What is Caching

Caching means storing the result of an expensive operation (a slow DB query, or in this case a call to a weather API) so the next time someone asks for the same thing, you return the saved result instead of doing the work again.

In this project, fetching weather data for a city involves calling an external API. That's slow and rate-limited. Caching means: ask once, save the answer, and serve it from memory/Redis on the next request for the same city — until it expires.

## 2. In-Memory vs Distributed Cache

**In-memory cache** lives inside your application's own process (e.g., a `ConcurrentHashMap`, or a library like Caffeine). It's extremely fast since there's no network call involved — but it only exists for that one running instance of your app. If you restart the app, the cache is gone. If you run multiple instances, each one has its own separate cache.

**Distributed cache** (Redis here) runs as a separate service. Every instance of your app talks to the same Redis server over the network, so they all see the same cached data. Slightly slower than in-memory (network hop + serialization), but the data is shared and survives app restarts.

## 3. When to Use Which

- **One app instance, no scaling planned** → in-memory is enough and faster.
- **Multiple instances behind a load balancer** → you need a distributed cache, otherwise each instance caches separately and users get inconsistent results.
- **Caching external API responses that don't change per-user** (like weather data) → distributed cache makes sense even with one instance, because it's still good practice and scales cleanly later.
- **Caching something tied to a single user's session/request** → in-memory is usually fine.

This project uses Redis to practice and demonstrate distributed caching concepts, even though the actual scale doesn't strictly require it yet.

## 4. `@Cacheable`, `@CachePut`, `@CacheEvict`, `@EnableCaching`

- **`@EnableCaching`** — goes on a config class. Turns on Spring's caching support for the whole app. Without it, none of the other annotations do anything.
- **`@Cacheable`** — put on a method that fetches data. Before running the method, Spring checks if the result is already cached. If yes, it returns the cached value and skips the method body entirely. If not, it runs the method, then stores the result.
- **`@CachePut`** — always runs the method, then updates the cache with the new result. Used when you want to refresh the cache (e.g., after updating a record), not just read from it.
- **`@CacheEvict`** — removes an entry from the cache. Used after a delete (or sometimes an update), so old/stale data doesn't get served afterward.

## 5. Why `@Cacheable` Can Run on `value` Alone, but `@CachePut`/`@CacheEvict` Need a Key

`@Cacheable` can technically work with just `value` (the cache name) because Spring will auto-generate a key from the method's arguments by default. That's fine for read operations — if you call it with the same arguments, it generates the same key, and you get a cache hit.

`@CachePut` and `@CacheEvict` need an explicit `key` because they're modifying a *specific* existing entry. If Spring guessed the key differently than how it was originally cached, you'd update/evict the wrong entry (or nothing at all) and end up with stale data sitting in the cache. So for these two, you intentionally point at the exact same key used by `@Cacheable`, usually based on a unique field like `city`.

## 6. Why and When Serialization Matters

Redis stores everything as bytes — it doesn't know about Java objects. So before an object goes into Redis, it has to be **serialized** (converted to bytes/JSON), and when reading it back, **deserialized** into a Java object again.

This is needed any time you cache something with Redis (not with simple in-memory maps, since those just hold the actual object reference). If your cached object doesn't implement `Serializable` (or you don't configure a JSON serializer), Spring/Redis will throw an error trying to store it. This project configures Jackson-based JSON serialization in the cache config so cached weather objects are stored as readable JSON in Redis instead of raw Java serialized bytes.

## 7. Docker Commands to Run Redis

Start a Redis container:
```bash
docker run -d --name redis-cache -p 6379:6379 redis
```

Check it's running:
```bash
docker ps
```

Stop it:
```bash
docker stop redis-cache
```

Start it again later:
```bash
docker start redis-cache
```

Remove it completely:
```bash
docker rm redis-cache
```

## 8. Basic Redis CLI Commands

Enter the CLI (via the running container):
```bash
docker exec -it redis-cache redis-cli
```

Useful commands once inside:
```
PING                 # check connection, should return PONG
KEYS *                # list all cached keys
GET <key>              # view value for a key
TTL <key>              # check remaining time-to-live (seconds)
DEL <key>              # delete a specific key
FLUSHALL               # clear everything in Redis (use carefully)
```

## 9. AppConfig — How the Cache Manager Works

`AppConfig` (or `CacheConfig`) is where Spring is told *how* to talk to Redis. This is the bridge between your app and the third-party cache (Redis):

- A `RedisConnectionFactory` bean defines how to connect (host, port).
- A `RedisCacheManager` bean defines cache behavior — TTL (how long entries live before expiring automatically), and the serializer (so objects are stored as JSON, as covered in section 6).
- Different cache names (like `weather`) can have different TTLs configured here if needed.

Once this config exists and `@EnableCaching` is set, Spring automatically wires `@Cacheable`/`@CachePut`/`@CacheEvict` to use this manager — you don't manually call Redis anywhere in your service code.

A `CacheErrorHandler` can also be added here so that if Redis is ever down or unreachable, the app doesn't crash — it just skips the cache and goes straight to the actual method/API call (graceful degradation).

## 10. CacheInspectionService — What It's For

This is a small helper service for *inspecting* what's currently cached, separate from the actual business logic. Useful methods here generally include:

- View all cached weather entries for a given cache name.
- Check whether a specific city's data is currently cached.
- Manually clear the cache for a specific key or the whole cache (useful for testing, without restarting Redis from the CLI every time).

This service exists purely as a debugging/visibility tool — it doesn't replace the actual caching annotations doing the real work.

## 11. API Endpoints

| Method | Endpoint | Description |
|--------|----------|--------------|
| POST | `/api/weather` | Add weather data |
| GET | `/api/weather` | Get all weather records |
| GET | `/api/weather/{city}` | Get weather by city |
| PUT | `/api/weather/{city}` | Update weather data |
| DELETE | `/api/weather/{city}` | Delete weather data |
| GET | `/api/weather/getCacheData` | View cache data |
| GET | `/api/weather/clearCache` | Clear cache |

(Adjust paths above to match your actual controller mappings.)