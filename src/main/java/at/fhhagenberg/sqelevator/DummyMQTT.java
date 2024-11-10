package at.fhhagenberg.sqelevator;

// To be deleted when we have a real mqtt class
public class DummyMQTT {
    public void Publish(String Topic) {
      System.out.println("MQTT Publish called: " + Topic);
      // do nothing
      return;
    }
  }