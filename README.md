# sqelevator-proj

Group assignment SQElevator

## Start MQTT Broker

Navigate to `mqtt` folder and run
```
docker run -it -p 1883:1883 -v "$PWD/mosquitto/config:/mosquitto/config" -v /mosquitto/data -v /mosquitto/log --network host eclipse-mosquitto
```

## Run with Simulation

Start Simulation, then `ElevatorsMQTTAdapter` first:

```
mvn clean install -DskipTests exec:java -DmainClass=at.fhhagenberg.sqelevator.ElevatorsMQTTAdapter
or
mvn clean install -DskipTests exec:java -DmainClass="at.fhhagenberg.sqelevator.ElevatorsMQTTAdapter"
```

then run `ElevatorAlgorithm`:

```
mvn clean install -DskipTests exec:java -DmainClass=at.fhhagenberg.sqelevator.ElevatorAlgorithm
or
mvn clean install -DskipTests exec:java -DmainClass="at.fhhagenberg.sqelevator.ElevatorAlgorithm"
```

## Build Jar

To build a jar run
```
mvn -B clean package -DmainClass="at.fhhagenberg.sqelevator.ElevatorsMQTTAdapter"
or
mvn -B clean package -DmainClass="at.fhhagenberg.sqelevator.ElevatorAlgorithm"
```
This builds a "<name>-with-dependencies.jar" in target/.