package at.fhhagenberg.sqelevator;

import java.io.FileInputStream;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import at.fhhagenberg.sqelevator.ElevatorsMQTTAdapter.MessageHandler;
import sqelevator.IElevator;

public class ElevatorAlgorithm {

    // all Topics starting with TOPIC_ are finished topics
    // all Topics starting with SUBTOPIC_ are subtopics and need to be appended to the correct finished topic
    public final static String TOPIC_SEP = "/";

    public final static String TOPIC_BUILDING = "buildings";
    public final static String TOPIC_BUILDING_ID = "0";

    public final static String TOPIC_BUILDING_ELEVATORS = TOPIC_BUILDING + TOPIC_SEP + TOPIC_BUILDING_ID + TOPIC_SEP + "elevators";
    public final static String TOPIC_BUILDING_FLOORS = TOPIC_BUILDING + TOPIC_SEP + TOPIC_BUILDING_ID + TOPIC_SEP + "floors";
    public final static String TOPIC_BUILDING_NR_ELEVATORS = TOPIC_BUILDING + TOPIC_SEP + TOPIC_BUILDING_ID + TOPIC_SEP + "NrElevators";
    public final static String TOPIC_BUILDING_NR_FLOORS = TOPIC_BUILDING + TOPIC_SEP + TOPIC_BUILDING_ID + TOPIC_SEP + "NrFloors";

    public final static String SUBTOPIC_ELEVATORS_ELEVATOR_CAPACITY = "ElevatorCapacity";
    public final static String SUBTOPIC_ELEVATORS_ELEVATOR_SETTARGET = "SetTarget";
    public final static String SUBTOPIC_ELEVATORS_ELEVATOR_FLOORREQUESTED = "FloorRequested";
    public final static String SUBTOPIC_ELEVATORS_ELEVATOR_FLOORSERVICED = "FloorServiced";  
    public final static String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDIRECTION = "ElevatorDirection";
    public final static String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDOORSTATUS = "ElevatorDoorStatus";
    public final static String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORTARGETFLOOR = "ElevatorTargetFloor";
    public final static String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTFLOOR = "ElevatorCurrentFloor";
    public final static String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORACCELERATION = "ElevatorAcceleration";
    public final static String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORSPEED = "ElevatorSpeed";
    public final static String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTHEIGHT = "ElevatorCurrentHeight";
    public final static String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTPASSENGERWEIGHT = "ElevatorCurrentPassengersWeight";

    public final static String SUBTOPIC_FLOORS_BUTTONDOWNPRESSED = "ButtonDownPressed";
    public final static String SUBTOPIC_FLOORS_BUTTONUPPRESSED = "ButtonUpPressed";

    private Mqtt5AsyncClient mqttClient;

    // default information about the elevator system
    private int mNrOfFloors = 0;
    private int mNrOfElevators = 0;
    private ArrayList<Integer> mElevatorCapacitys;

    private Building mBuilding;

    /**
     * CTOR
     */
    public ElevatorAlgorithm (Mqtt5AsyncClient _mqttClient) {
        this.mqttClient = _mqttClient;

        // Connect to the broker
        CompletableFuture<Void> connectFuture = mqttClient.connect()
            .thenAccept(connAck -> {
                System.out.println("Connected successfully!");
            })
            .exceptionally(throwable -> {
                System.err.println("Connection failed: " + throwable.getMessage());
                return null;
            });

        connectFuture.join();
    }

    public static void main(String[] args) {

        try {
            String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
            String appConfigPath = rootPath + "Elevators.properties";

            Properties appProps = new Properties();
            appProps.load(new FileInputStream(appConfigPath));

            // Create an MQTT client
            Mqtt5AsyncClient mqttClient = MqttClient.builder()
                .automaticReconnectWithDefaultConfig()
                .useMqttVersion5()
                .identifier(appProps.getProperty("MqttIdentifier"))
                .serverHost(appProps.getProperty("MqttHost")) // Public HiveMQ broker
                .serverPort((Integer) appProps.get("MqttPort")) // Default MQTT port
                .buildAsync();

            ElevatorAlgorithm client = new ElevatorAlgorithm(mqttClient);

            client.run();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     */
    private void run() {
        // get initial information for building
        subcribeToInitials();

        // subscribe to the variables that can change during operation
        subscribeToVariables();

        try {
            while(true) {
                doAlgorithm();
                Thread.sleep(100);
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    private void subcribeToInitials() {
        try {
            // Subscribe to elevator count
            CountDownLatch latchElevaCnt = new CountDownLatch(1);
            this.subscribeMQTT(TOPIC_BUILDING_NR_ELEVATORS, (topic, message) -> {
                try {
                    mNrOfElevators = Integer.parseInt(message);
                    latchElevaCnt.countDown();
                } catch (Exception e) {
                    System.err.println(e.toString());
                }
            });

            latchElevaCnt.await();

            // subscribe to elevator capacity
            mElevatorCapacitys = new ArrayList<>(mNrOfElevators);
            CountDownLatch latchElevCap = new CountDownLatch(mNrOfElevators);
            for (int i = 0; i < mNrOfElevators; i++) {
                this.subscribeMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + i + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_CAPACITY, (topic, message) -> {
                    try {
                        String splittedTopic = topic.split(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP)[0];
                        int elevNr = Integer.parseInt(splittedTopic.split(TOPIC_SEP)[0]);
                        mElevatorCapacitys.set(elevNr, Integer.parseInt(message));
                        latchElevCap.countDown();
                    } catch (Exception e) {
                        System.err.println(e.toString());
                    }
                });
            }
            
            latchElevCap.await();

            // subscribe to floor number
            CountDownLatch latchFloorNr = new CountDownLatch(1);
            this.subscribeMQTT(TOPIC_BUILDING_NR_FLOORS, (topic, message) -> {
                try {
                    mNrOfFloors = Integer.parseInt(message);
                    latchFloorNr.countDown();
                } catch (Exception e) {
                    System.err.println(e.toString());
                }
            });

            latchFloorNr.await();

        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void subscribeToVariables() {
        try {

            // subscribe to Up buttons
            for (int floorNr = 0; floorNr < this.mNrOfFloors; floorNr++) {
                this.subscribeMQTT(TOPIC_BUILDING_FLOORS + TOPIC_SEP + floorNr + TOPIC_SEP + SUBTOPIC_FLOORS_BUTTONUPPRESSED, (topic, message) -> {
                    try {
                        updateTopic(topic, message);
                    } catch (Exception e) {
                        System.err.println(e.toString());
                    }
                });
            }

            // subscribe to Down buttons
            for (int floorNr = 0; floorNr < this.mNrOfFloors; floorNr++) {
                this.subscribeMQTT(TOPIC_BUILDING_FLOORS + TOPIC_SEP + floorNr + TOPIC_SEP + SUBTOPIC_FLOORS_BUTTONDOWNPRESSED, (topic, message) -> {
                    try {
                        updateTopic(topic, message);
                    } catch (Exception e) {
                        System.err.println(e.toString());
                    }
                });
            }

            // subscribe to Floors Requested
            for (int elevNr = 0; elevNr < this.mNrOfElevators; elevNr++) {
                for (int floorNr = 0; floorNr < this.mNrOfFloors; floorNr++) {
                    this.subscribeMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_FLOORREQUESTED + TOPIC_SEP + floorNr, (topic, message) -> {
                        try {
                            updateTopic(topic, message);
                        } catch (Exception e) {
                            System.err.println(e.toString());
                        }
                    });
                }
            }

            // subscribe to Floors Serviced
            for (int elevNr = 0; elevNr < this.mNrOfElevators; elevNr++) {
                for (int floorNr = 0; floorNr < this.mNrOfFloors; floorNr++) {
                    this.subscribeMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_FLOORSERVICED + TOPIC_SEP + floorNr, (topic, message) -> {
                        try {
                            updateTopic(topic, message);
                        } catch (Exception e) {
                            System.err.println(e.toString());
                        }
                    });
                }
            }

            // subscribe to Committed Direction
            subscribeAndSetCallbackForAll(SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDIRECTION, this::updateTopic);

            // subscribe to Door Status
            subscribeAndSetCallbackForAll(SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDOORSTATUS, this::updateTopic);

            // subscribe to Target Floor
            subscribeAndSetCallbackForAll(SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORTARGETFLOOR, this::updateTopic);

            // subscribe to Current Floor
            subscribeAndSetCallback(SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTFLOOR, this::updateTopic);

            // subscribe to Elevator Acceleration
            subscribeAndSetCallback(SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORACCELERATION, this::updateTopic);

            // subscribe to Elevator Speed
            subscribeAndSetCallback(SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORSPEED, this::updateTopic);
            
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void subscribeAndSetCallbackForAll(String subTopic, BiConsumer<String, String> callback) {
        for (int elevNr = 0; elevNr < mNrOfElevators; elevNr++) {
            subscribeAndSetCallback(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + subTopic, callback);
        }
    }

    private void subscribeAndSetCallback(String subTopic, BiConsumer<String, String> callback) {
        // subscribe to Target Floor
        this.subscribeMQTT(subTopic, (topic, message) -> {
            try {
                // call callback
                callback.accept(topic, message);
            } catch (Exception e) {
                System.err.println(e.toString());
            }
        });
    }

    private void updateTopic(String topic, String message) {
        synchronized (this) {
            if (topic.contains(TOPIC_BUILDING_FLOORS)) {
                String[] topicWithoutBaseTopic = topic.split(TOPIC_BUILDING_FLOORS + TOPIC_SEP);
                String[] splittedTopic = topicWithoutBaseTopic[0].split(TOPIC_SEP);

                // get floor number and sub topic
                int floorNr = Integer.parseInt(splittedTopic[0]);
                String subTopic = splittedTopic[1];

                // convert state to bool
                boolean state = Boolean.parseBoolean(message);

                switch (subTopic) {
                    case SUBTOPIC_FLOORS_BUTTONUPPRESSED:
                        mBuilding.updateUpButtonState(floorNr, state);
                        break;
                    case SUBTOPIC_FLOORS_BUTTONDOWNPRESSED:
                        mBuilding.updateDownButtonState(floorNr, state);
                        break;
                    default:
                        break;
                }
            }
            else {

                String[] topicWithoutBaseTopic = topic.split(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP);
                String[] splittedTopic = topicWithoutBaseTopic[0].split(TOPIC_SEP);

                Integer elevNr = Integer.parseInt(splittedTopic[0]);
                String subTopic = splittedTopic[1];

                switch(subTopic) {
                    case SUBTOPIC_ELEVATORS_ELEVATOR_FLOORREQUESTED:
                        mBuilding.updateElevatorFloorRequested(elevNr, Integer.parseInt(splittedTopic[2]), Boolean.parseBoolean(message));
                        break;
                    case SUBTOPIC_ELEVATORS_ELEVATOR_FLOORSERVICED:
                        mBuilding.updateElevatorFloorToService(elevNr, Integer.parseInt(splittedTopic[2]), Boolean.parseBoolean(message));
                        break;
                    case SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDIRECTION:
                        mBuilding.updateElevatorDirection(elevNr, Integer.parseInt(message));
                        break;
                    case SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDOORSTATUS:
                        mBuilding.updateElevatorDoorStatus(elevNr, Integer.parseInt(message));
                        break;
                    case SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORTARGETFLOOR:
                        mBuilding.updateElevatorTargetFloor(elevNr, Integer.parseInt(message));
                        break;
                    case SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTFLOOR:
                        mBuilding.updateElevatorCurrentFloor(elevNr, Integer.parseInt(message));
                        break;
                    case SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORACCELERATION:
                        mBuilding.updateElevatorAcceleration(elevNr, Integer.parseInt(message));
                        break;
                    case SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORSPEED:
                        mBuilding.updateElevatorSpeed(elevNr, Integer.parseInt(message));
                        break;
                    default:
                        System.err.println("Unsupported topic!");
                }
            }
        }
    }

    private void doAlgorithm() {
        Building currentStatus = mBuilding;
    }

    /**
     * 
     */
    private void subscribeMQTT(String topic, MessageHandler messageHandler) {
        // Subscribe to a topic
        mqttClient.subscribeWith()
            .topicFilter(topic)
            .qos(MqttQos.AT_LEAST_ONCE) // QoS level 1
            .callback(publish -> {
                String message = new String(publish.getPayloadAsBytes());
                messageHandler.handleMessage(topic, message);
                }) // Use the provided message handler
            .send()
            .whenComplete((subAck, throwable) -> {
                if (throwable != null) {
                    // Handle subscription failure
                    System.err.println("Failed to subscribe: " + throwable.getMessage());
                } else {
                    // Handle successful subscription
                    System.out.println("Subscribed successfully to topic: " + topic);
                }
            });
    }

    /**
     * DTOR
     */
    protected void finalize() {
        try {
            this.mqttClient.disconnect();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
