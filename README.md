# sqelevator-proj
Group assignment SQElevator

## Run with Simulation

Start Simulation, then ElevatorsMQTTAdapter first:


```
mvn clean install -DskipTests exec:java -DmainClass=at.fhhagenberg.sqelevator.ElevatorsMQTTAdapter
mvn clean install -DskipTests exec:java -DmainClass="at.fhhagenberg.sqelevator.ElevatorsMQTTAdapter"
```

then run ElevatorAlgorithm:

```
mvn clean install -DskipTests exec:java -DmainClass=at.fhhagenberg.sqelevator.ElevatorAlgorithm
mvn clean install -DskipTests exec:java -DmainClass="at.fhhagenberg.sqelevator.ElevatorAlgorithm"
```
