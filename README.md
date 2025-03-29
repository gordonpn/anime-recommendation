# Anime Recommendation

## Development

### Prerequisites

- JDK23

### Run

To run the app

```sh
./gradlew clean bootJar && java -jar build/libs/anime-0.0.1-SNAPSHOT.jar
```

## To-do

- [x] Persist the token, to reuse on next run
- [x] Persist fetched anime details for certain time, to avoid unnecessary refetches
- [ ] Check the sync status table on start and skip is unnecessary
- [ ] Fetch anime details for each anime in the user list and persist