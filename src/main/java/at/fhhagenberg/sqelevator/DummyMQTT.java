package at.fhhagenberg.sqelevator;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// To be deleted when we have a real mqtt class
public class DummyMQTT {
  private static Logger logger = LogManager.getLogger(DummyMQTT.class);
    public void publish(String topic) {
      logger.info("MQTT Publish called: {}", topic);
      // do nothing
    }
  }