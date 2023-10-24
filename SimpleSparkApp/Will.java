import java.util.Scanner;
public class Will {
   public static final Scanner reader = new Scanner(System.in);
   public static final int sleepTime = 1000;
   public static void main(String[] args) {
fire("Left");   }
   public static void move(String location) {
       System.out.println("move" + location);
       try { Thread.sleep(sleepTime); } catch (Exception e) { }
   }
   public static void fire(String location) {
       System.out.println("fire" + location);
       try { Thread.sleep(sleepTime); } catch (Exception e) { }
   }
   public static String get(String location) {
       System.out.println("get" + location);
       return reader.nextLine();
   }
}