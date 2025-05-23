package net.pistonmaster.pistonqueue.velocity.utils;

public class AcceptedData {
  private final String name;
  private final String time;
  public AcceptedData(String name, String time) {
    this.name = name;
    this.time = time;
  }
  public String getName() { return name; }
  public String getTime() { return time; }
}
