import java.util.Random;
import java.util.concurrent.Semaphore;

/***************************************************************************************/

//  Provide code for the methods in the classes Aeroplane and Airport, and in one place
//  in the main() method. Look for "your code here" comments.

/* the main class of assignment 2, launching the simulation */

public class Assignment2 {
  // Configuration
  final static int DESTINATIONS = 4;
  final static int AEROPLANES = 6;
  final static int PLANE_SIZE = 3;
  final static int PASSENGERS = 20;
  final static String[] destName = {"Toronto", "New York", "New Delhi", "Beijing"};

  public static void main(String args[]) {
    int i;
    Aeroplane[] sships = new Aeroplane[AEROPLANES];
    Passenger[] passengers = new Passenger[PASSENGERS];

    // create the airport
    Airport sp = new Airport();

    /* create aeroplanes and passengers*/
    for (i=0; i<AEROPLANES; i++) {
      sships[i] = new Aeroplane(sp, i);
    }
    for (i=0; i<PASSENGERS; i++) {
      passengers[i] = new Passenger(sp, i);
    }

    /* now launch them */
    for (i=0; i<AEROPLANES; i++) {
      sships[i].start();
    }
    for (i=0; i<PASSENGERS; i++) {
      passengers[i].start();
    }

    // let them enjoy for 20 seconds
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) { }

    /* now stop them */
    // note how we are using deferred cancellation
    for (i=0; i<AEROPLANES; i++) {
      try {
        sships[i].interrupt();
      } catch (Exception e) { }
    }
    for (i=0; i<PASSENGERS; i++) {
      try {
        passengers[i].interrupt();
      } catch (Exception e) { }
    }

    // Wait until everybody else is finished

    // This should be the last thing done by this program:
    System.out.println("Simulation finished.");
  }
}

/* The class implementing a passenger. */
// This class is completely provided to you, you don't have to change
// anything, just have a look and understand what the passenger wants from
// the airport and from the aeroplanes
class Passenger extends Thread {
  private boolean enjoy;
  private int id;
  private Airport sp;

  // constructor
  public Passenger(Airport sp, int id) {
    this.sp = sp;
    this.id = id;
    enjoy = true;
  }

  // this is the passenger's thread
  public void run() {
    int stime;
    int dest;
    Aeroplane sh;

    while (enjoy) {
      try {
        // Wait
        stime = (int) (700*Math.random());
        sleep(stime);

        // Choose the destination at random
        dest = (int) (((double) Assignment2.DESTINATIONS)*Math.random());
        System.out.println("Passenger " + id + " wants to go to " + Assignment2.destName[dest]);

        // come to the airport and board an aeroplane to my destination
        // (might have to wait if there is no such aeroplane ready)
        sh = sp.wait4Ship(dest);

        // Should be executed after the aeroplane is on the pad and taking passengers
        System.out.println("Passenger " + id + " has boarded aeroplane " + sh.id + ", destination: "+Assignment2.destName[dest]);
        // wait for launch
        sh.wait4launch();

        // Enjoy the ride
        // Should be executed after the aeroplane has launched.
        System.out.println("Passenger "+id+" enjoying the ride to "+Assignment2.destName[dest]+ ": Woohooooo!");

        // wait for landing
        sh.wait4landing();

        // Should be executed after the aeroplane has landed
        System.out.println("Passenger " + id + " leaving the aeroplane " + sh.id);

        // Leave the aeroplane
        sh.leave();
      } catch (InterruptedException e) {
        enjoy = false; // have been interrupted, probably by the main program, terminate
      }
    }
    System.out.println("Passenger "+id+" has finished its rides.");
  }
}

/* The class simulating an aeroplane */
// Now, here you will have to implement several methods
class Aeroplane extends Thread {
  public int id;
  private Airport sp;
  private boolean enjoy;
  // your code here (other local variables and semaphores)
  private int passengers; // number of passengers aboard
  Semaphore semBoard; // to know if a passenger can board this aeroplane
  Semaphore semLeave; // to know if a passenger can leave the aeroplane (once plane has landed)
  Semaphore semLaunch; // know if aeroplane can launch

  // constructor
  public Aeroplane(Airport sp, int id) {
    this.sp = sp;
    this.id = id;
    enjoy = true;
    // your code here (local variable and semaphore initializations)
    passengers = 0;
    semBoard = new Semaphore(0, true);
    semLeave = new Semaphore(0, true);
    semLaunch = new Semaphore(0, true);
  }

  // the aeroplane thread executes this
  public void run() {
    int stime;
    int dest;

    while (enjoy) {
      try {
        // Wait until there an empty landing pad, then land
        dest = sp.wait4landing(this);
        System.out.println("Aeroplane " + id + " landing on pad " + dest);

        semBoard = sp.semPadBoard[dest];
        // Tell the passengers that we have landed
        wait4landing();

        // Wait until all passengers leave
        for (int j=0; j<passengers; j++) {
          leave();
        }

        System.out.println("Aeroplane " + id + " boarding to "+Assignment2.destName[dest]+" now!");

        // the passengers can start to board now
        sp.boarding(dest);

        // Wait until full of passengers

        // 4, 3, 2, 1, Launch!

        System.out.println("Aeroplane " + id + " launches towards "+Assignment2.destName[dest]+"!");

        // tell the passengers we have launched, so they can enjoy now ;-)

        // Fly in the air
        stime = 500+(int) (1500*Math.random());
        sleep(stime);
      } catch (InterruptedException e) {
        enjoy = false; // have been interrupted, probably by the main program, terminate
      }
    }
    System.out.println("Aeroplane "+id+" has finished its flights.");
  }

  // service functions to passengers

  // called by the passengers leaving the aeroplane
  public void leave() throws InterruptedException  {
    // your code here
    try {
      semLeave.acquire(); // attempt to leave aeroplane
    }
     catch (InterruptedException e) {
       break;
     }
    passengers--; // decrement # of passengers on plane

    if (passengers > 0) {
      semLeave.release(); // allow next passenger to leave aeroplane
    }
    else if (passengers == 0) {
      semBoard.release(); // all passengers have left aeroplane, new ones can board
    }
  }

  // called by the passengers sitting in the aeroplane, to wait
  // until the launch
  public void wait4launch() throws InterruptedException {
    // your code here

  }

  // called by the bored passengers sitting in the aeroplane, to wait
  // until landing
  public void wait4landing() throws InterruptedException {
    // your code here

    semLand.release();
  }
}

/* The class implementing the Airport. */
/* This might be convenient place to put lots of the synchronization code into */
class Airport {
  Aeroplane[] pads; // what is sitting on a given pad
  // your code here (other local variables and semaphores)
  Semaphore[] semPadLand = new Semaphore[Assignment2.DESTINATIONS]; // to know when a plane can land on a given pad
  Semaphore[] semPadBoard = new Semaphore[Assignment2.DESTINATIONS]; // to know when a passenger can board plane on pad

  // constructor
  public Airport() {
    int i;
    pads = new Aeroplane[Assignment2.DESTINATIONS];

    // pads[] is an array containing the aeroplanes sitting on corresponding pads
    // Value null means the pad is empty
    for(i=0; i<Assignment2.DESTINATIONS; i++) {
      pads[i] = null;
      semPadLand[i] = new Semaphore(1, true); // initialize with 1 because pads start empty
      semPadBoard[i] = new Semaphore(0, true);
    }
    // your code here (local variable and semaphore initializations
    // see above

  }

  // called by a passenger wanting to go to the given destination
  // returns the aeroplane he/she boarded
  // Careful here, as the pad might be empty at this moment
  public Aeroplane wait4Ship(int dest) throws InterruptedException {
    // your code here
    while (pads[dest] == null) {
      try {
        semPadBoard[dest].acquire(); // request to board
      } catch (InterruptedException e) {
        break;
      }
    }

    return pads[dest];
  }

  // called by an aeroplane to tell the airport that it is accepting passengers now to destination dest
  public void boarding(int dest) {
    // your code here

  }

  // called by an aeroplane returning from a trip
  // Returns the number of the empty pad where to land (might wait
  // until there is an empty pad).
  // Try to rotate the pads so that no destination is starved
  public int wait4landing(Aeroplane sh)  throws InterruptedException  {
    // your code here
    boolean found = false;
    int pad = -1;

    while (!found) { // loop until available landing pad is free
      for (int i=0; i<Assignment2.DESTINATIONS; i++) {
        if (pads[i] == null) { // check empty pad
          try {
            semPad[i].acquire();
          } catch (InterruptedException e) {
            break;
          }
          found = true;
          pad = i;
          pads[i] = sh;
        }
      }
    }

    return pad;
  }

  // called by an aeroplane when it launches, to inform the
  // airport that the pad has been emptied
  public void launch(int dest) {
    // your code here
    semPad[dest].release(); // free up given launch pad
    pads[dest] = null; // pad is now empty
  }
}
