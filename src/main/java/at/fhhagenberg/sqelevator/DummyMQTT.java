package at.fhhagenberg.sqelevator;

// To be deleted when we have a real mqtt class
public class DummyMQTT {
    public void publish(String topic) {
      System.out.println("MQTT Publish called: " + topic);
      // do nothing
    }
  }