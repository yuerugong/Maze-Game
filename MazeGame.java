import java.awt.Color;
import java.util.*;
import tester.*;
import javalib.impworld.*;
import javalib.worldimages.*;

// to represent a cell in a maze
class Node {
  // node's coordinates
  int x;
  int y;
  // node's neighbors
  Node up;
  Node down;
  Node left;
  Node right;
  // has this node been visited in the search?
  boolean visited = false;
  // has this node been visited by player?
  boolean visitByPlayer;
  // has this node been visited in initially calculating the path?
  boolean hiddenVisited = false;
  // is this node in the automatically found path to the target?
  boolean path = false;

  Node(int x, int y) {
    this.x = x;
    this.y = y;
  }

  // Return this node represented as a string
  public String toString() {
    return Integer.toString(this.x) + "x" + Integer.toString(this.y) + "y";
  }

  // get the Posn of the center of this node
  Posn getPosn() {
    return new Posn((this.x * MazeWorld.EDGEWIDTH) + (MazeWorld.EDGEWIDTH / 2),
        (this.y * MazeWorld.EDGEHEIGHT) + (MazeWorld.EDGEHEIGHT / 2));
  }

  // return a WorldImage representation of this node
  void draw(WorldScene bg) {
    Posn p = this.getPosn();
    Color c;
    if (this.path) {
      c = new Color(61, 118, 204);
    }
    else if (this.visitByPlayer && this.visited && MazeWorld.PATHVIS) {
      c = new Color(0, 150, 150);
    }
    else if (this.visited && MazeWorld.PATHVIS) {
      c = new Color(145, 184, 242);
    }
    else if (this.visitByPlayer && !MazeWorld.PATHVI) {
      c = new Color(0, 150, 0);
    }
    else if (this.isTarget()) {
      c = new Color(108, 32, 128);
    }
    else if (this.isStart()) {
      c = new Color(32, 128, 70);
    }
    else {
      c = new Color(192, 192, 192);
    }
    bg.placeImageXY(new RectangleImage(MazeWorld.EDGEWIDTH, MazeWorld.EDGEHEIGHT, "solid", c), p.x,
        p.y);
  }

  // is this node the start node for the maze?
  boolean isStart() {
    return this.x == 0 & this.y == 0;
  }

  // is this node the end node for the maze?
  boolean isTarget() {
    return this.x == MazeWorld.WORLDWIDTH - 1 && this.y == MazeWorld.WORLDHEIGHT - 1;
  }

  // Get the neighbors of this node
  ArrayList<Node> getNeighbors() {
    ArrayList<Node> result = new ArrayList<Node>();
    if (this.left instanceof Node) {
      result.add(this.left);
    }
    if (this.up instanceof Node) {
      result.add(this.up);
    }
    if (this.right instanceof Node) {
      result.add(this.right);
    }
    if (this.down instanceof Node) {
      result.add(this.down);
    }
    return result;
  }
}

// to represent a weighted edge between two nodes
class Edge {
  // nodes connected
  Node a;
  Node b;
  // weight of this edge
  int weight;

  Edge(Node a, Node b, int weight) {
    this.a = a;
    this.b = b;
    this.weight = weight;
  }

  // draw this edge
  void draw(WorldScene bg) {
    Posn a = this.a.getPosn();
    Posn b = this.b.getPosn();
    Posn p = new Posn((a.x + b.x) / 2, (a.y + b.y) / 2);
    int thickness = 2;
    Color c = new Color(102, 102, 102);
    if (a.x == b.x) {
      bg.placeImageXY(new RectangleImage(MazeWorld.EDGEWIDTH, thickness, "solid", c), p.x, p.y);
    }
    else {
      bg.placeImageXY(new RectangleImage(thickness, MazeWorld.EDGEHEIGHT, "solid", c), p.x, p.y);
    }
  }
}

// represents a maze world
class MazeWorld extends World {

  int state;

  // nodes in the board
  ArrayList<ArrayList<Node>> nodes = initializeNodes();

  // world size in nodes
  static int WORLDHEIGHT = 15;
  static int WORLDWIDTH = 20;

  // sizes of edges in pixels
  static int EDGEHEIGHT = 600 / WORLDHEIGHT;
  static int EDGEWIDTH = 800 / WORLDWIDTH;

  // random object used for edge weights
  Random random = new Random();

  // all edges in the world
  ArrayList<Edge> allEdges = initializeEdges();

  // which searching algorithm to be run on-tick
  // "bfs" - breadth-first. "dfs" - depth-first
  String tickSearch = "";

  // worklist for the bfs
  Queue<Node> breadthFirstWorkList = new LinkedList<Node>();

  // worklist for the dfs
  Stack<Node> depthFirstWorkList = new Stack<Node>();

  // hashmap to reconstruct the path from
  HashMap<String, Edge> cameFromEdge = new HashMap<String, Edge>();

  // The player of this world
  Player player = new Player(MazeUtils.getStart(this.nodes));

  // are the visited paths to be drawn?
  static boolean PATHVIS = false;
  static boolean PATHVI = false;

  // 0 for no direction bias, 1 for vertical, 2 for horizontal
  int bias = 0;

  // if true, prevent a new search from being started
  boolean newSearchBlock = false;

  // edges in this maze
  ArrayList<Edge> edges = initializeMaze();

  // the path from beginning to end of this maze
  ArrayList<Node> path = depthFirstSearch();

  public WorldScene makeScene() {
    WorldScene bg = new WorldScene(600, 800);
    if (this.state == 0) {
      makeImageHelp(0, MazeWorld.WORLDWIDTH / 2, bg);
      makeImageHelp(MazeWorld.WORLDWIDTH / 2, MazeWorld.WORLDWIDTH, bg);
      player.draw(bg);
    }
    else if (this.state == 1) {
      makeImageHelp(0, MazeWorld.WORLDWIDTH / 2, bg);
      makeImageHelp(MazeWorld.WORLDWIDTH / 2, MazeWorld.WORLDWIDTH, bg);
      player.draw(bg);
      String msg = "The maze is solved, press N to start a new maze";
      bg.placeImageXY(new TextImage(msg, new Color(255, 0, 0)), 200, 200);
    }
    WorldImage socre = new TextImage(this.player.getScore(), Color.black);
    bg.placeImageXY(socre, 400, 610);
    return bg;
  }

  // draw cells and edges in rows between given values
  void makeImageHelp(int min, int max, WorldScene bg) {
    for (ArrayList<Node> l : nodes) {
      for (Node n : l) {
        if (min <= n.x && max > n.x) {
          n.draw(bg);
        }
      }
    }
    ArrayList<Edge> nonEdges = allEdges;
    nonEdges.removeAll(edges);
    for (Edge e : nonEdges) {
      if (min <= e.a.x + 1 && max > e.a.x) {
        e.draw(bg);
      }
    }
  }

  // handle key events
  public void onKeyEvent(String s) {
    // reset the maze
    if (s.equals("n")) {
      this.nodes = initializeNodes();
      this.allEdges = initializeEdges();

      this.breadthFirstWorkList.clear();
      this.depthFirstWorkList.clear();
      this.cameFromEdge.clear();
      this.player = new Player(MazeUtils.getStart(this.nodes));
      this.edges = initializeMaze();
      this.newSearchBlock = false;
      this.tickSearch = "";
      this.state = 0;
      MazeWorld.PATHVIS = true;
      MazeWorld.PATHVI = false;
      // this.path = depthFirstSearch();
    }
    // start breadth-first search
    if (s.equals("b") && !this.newSearchBlock) {
      this.newSearchBlock = true;
      breadthFirstWorkList.add(MazeUtils.getStart(this.nodes));
      this.tickSearch = "bfs";
    }
    // start depth-first search
    if (s.equals("d") && !this.newSearchBlock) {
      this.newSearchBlock = true;
      depthFirstWorkList.add(MazeUtils.getStart(this.nodes));
      this.tickSearch = "dfs";
    }
    // move the player up one
    if (s.equals("up")) {
      player.moveUp(this.path);
    }
    // move the player down one
    if (s.equals("down")) {
      player.moveDown(this.path);
    }
    // move the player left one
    if (s.equals("left")) {
      player.moveLeft(this.path);
    }
    // move the player right one
    if (s.equals("right")) {
      player.moveRight(this.path);
    }
    // toggle path visibility
    if (s.equals("t")) {
      MazeWorld.PATHVIS = !MazeWorld.PATHVIS;
      MazeWorld.PATHVI = !MazeWorld.PATHVI;
    }

  }

  // advance the world one tick
  public void onTick() {
    // run one tick of breadth first search
    if (this.tickSearch.equals("bfs") && !breadthFirstWorkList.isEmpty() && this.state == 0) {
      Node next = breadthFirstWorkList.poll();
      if (next.visited) {
        // dont do anything
      }
      else if (next.isTarget()) {
        reconstruct(cameFromEdge, next);
      }
      else {
        for (Node n : next.getNeighbors()) {
          if (!n.visited) {
            breadthFirstWorkList.add(n);
            Edge temp = new Edge(next, n, 0);
            cameFromEdge.put(n.toString(), temp);
          }
        }
      }
      next.visited = true;
    }
    // run one tick of depth first search
    if (this.tickSearch.equals("dfs") && !depthFirstWorkList.isEmpty() && this.state == 0) {
      Node next = depthFirstWorkList.pop();
      if (next.visited) {
        // dont do anything
      }
      else if (next.isTarget()) {
        reconstruct(cameFromEdge, next);
      }
      else {
        for (Node n : next.getNeighbors()) {
          if (!n.visited) {
            depthFirstWorkList.add(n);
            Edge temp = new Edge(next, n, 0);
            cameFromEdge.put(n.toString(), temp);
          }
        }
      }
      next.visited = true;
    }

    if (this.player.loc.isTarget() || MazeUtils.getStart(this.nodes).path) {
      this.state = 1;
    }
  }

  // set all nodes' path field in the path to true
  void reconstruct(HashMap<String, Edge> h, Node n) {
    n.path = true;
    Edge e = h.get(n.toString());
    Node prev = e.a;
    while (!prev.isStart()) {
      prev.path = true;
      e = h.get(prev.toString());
      prev = e.a;
    }
    prev.path = true;
  }

  // create ArrayList of Nodes based on world size
  ArrayList<ArrayList<Node>> initializeNodes() {
    ArrayList<ArrayList<Node>> result = new ArrayList<ArrayList<Node>>();

    for (int x = 0; x < WORLDWIDTH; x++) {
      result.add(new ArrayList<Node>());
      for (int y = 0; y < WORLDHEIGHT; y++) {
        Node n = new Node(x, y);
        result.get(x).add(n);
      }
    }
    return result;
  }

  // create an ArrayList of Edges with random weights between all of this
  // world's nodes
  ArrayList<Edge> initializeEdges() {
    ArrayList<Edge> result = new ArrayList<Edge>();

    int width = nodes.size();
    int height = nodes.get(0).size();

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        Node n = nodes.get(x).get(y);
        // if there is a node to the right of n
        if (x < width - 1) {
          // initial random weight
          int valr = this.random.nextInt(1000000);
          if (this.bias == 2) {
            // this edge will be chosen first with weight 0
            valr = 0;
          }
          Node right = nodes.get(x + 1).get(y);
          Edge e = new Edge(n, right, valr);
          result.add(e);
        }
        // if there is a node below n
        if (y < height - 1) {
          // initial random weight
          int vald = this.random.nextInt(1000000);
          if (this.bias == 1) {
            // this edge will be chosen first with weight 0
            vald = 0;
          }
          Node down = nodes.get(x).get(y + 1);
          Edge e = new Edge(n, down, vald);
          result.add(e);
        }
      }
    }
    return result;
  }

  // return an ArrayList<Edge> of edges in the maze
  ArrayList<Edge> initializeMaze() {
    HashMap<String, String> representatives = new HashMap<String, String>();
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    ArrayList<Edge> worklist = this.allEdges;
    // sort edges by weight
    MazeUtils.sort(worklist);
    // initialize the HashMap
    initRepresentatives(representatives, this.nodes);

    int nextIdx = 0;
    while (MazeUtils.moreThanOneTree(representatives)) {
      Edge next = worklist.get(nextIdx);
      if (MazeUtils.find(representatives, next.a.toString())
          .equals(MazeUtils.find(representatives, next.b.toString()))) {
        // dont do anything
      }
      else {
        edgesInTree.add(next);
        MazeUtils.union(representatives, MazeUtils.find(representatives, next.a.toString()),
            MazeUtils.find(representatives, next.b.toString()));
      }
      nextIdx++;
    }
    // connect the nodes in the selected edges
    for (Edge e : edgesInTree) {
      MazeUtils.connect(e);
    }
    return edgesInTree;
  }

  // return a HashMap where each node in given ArrayList is mapped to itself
  void initRepresentatives(HashMap<String, String> h, ArrayList<ArrayList<Node>> nodes) {
    h.clear();
    for (ArrayList<Node> l : nodes) {
      for (Node n : l) {
        String asString = n.toString();
        h.put(asString, asString);
      }
    }
  }

  // Find the path to the end using depth-first search
  ArrayList<Node> depthFirstSearch() {
    HashMap<String, Edge> cameFromEdge = new HashMap<String, Edge>();
    Stack<Node> worklist = new Stack<Node>();
    ArrayList<Node> result = new ArrayList<Node>();

    worklist.add(MazeUtils.getStart(this.nodes));
    while (!worklist.isEmpty()) {
      Node next = worklist.pop();
      if (next.hiddenVisited) {
        // dont do anything
      }
      else if (next.isTarget()) {
        result = MazeUtils.reconstruct(cameFromEdge, next);
      }
      else {
        for (Node n : next.getNeighbors()) {
          if (!n.hiddenVisited) {
            worklist.add(n);
            Edge temp = new Edge(next, n, 0);
            cameFromEdge.put(n.toString(), temp);
          }
        }
      }
      next.hiddenVisited = true;
    }
    return result;
  }

}

// to hold utility methods
class MazeUtils {
  // Sorts the given ArrayList of edges according to their edge
  // weights
  static void sort(ArrayList<Edge> arr) {
    Collections.sort(arr, new EdgeComparator());
  }

  // does the given HashMap contain more than one tree?
  static boolean moreThanOneTree(HashMap<String, String> h) {
    ArrayList<String> parents = new ArrayList<String>();
    Set<String> keySet = h.keySet();
    // get the parents of all nodes
    for (String s : keySet) {
      String parent = MazeUtils.find(h, s);
      parents.add(parent);
    }

    if (parents.size() > 1) {
      String a = parents.get(0);
      for (String s : parents) {
        if (!a.equals(s)) {
          return true;
        }
      }
    }
    return false;
  }

  // get the root of the tree given string is in
  static String find(HashMap<String, String> h, String s) {
    String val = h.get(s);
    if (val.equals(s)) {
      return s;
    }
    else {
      return find(h, val);
    }
  }

  // set value of key a to b
  static void union(HashMap<String, String> h, String a, String b) {
    h.put(a, b);
  }

  // connect the two nodes in given edge
  static void connect(Edge e) {
    Node a = e.a;
    Node b = e.b;
    if (a.x == b.x && e.a.y == e.b.y + 1) {
      e.a.up = e.b;
      e.b.down = e.a;
    }
    else if (e.a.x == e.b.x && e.a.y == e.b.y - 1) {
      e.a.down = e.b;
      e.b.up = e.a;
    }
    else if (e.a.x == e.b.x + 1) {
      e.a.left = e.b;
      e.b.right = e.a;
    }
    else if (e.a.x == e.b.x - 1) {
      e.a.right = e.b;
      e.b.left = e.a;
    }
  }

  // Returns the starting node from given ArrayList of Nodes
  static Node getStart(ArrayList<ArrayList<Node>> src) {
    return src.get(0).get(0);
  }

  // return the path of nodes to reach given node through given hashmap
  static ArrayList<Node> reconstruct(HashMap<String, Edge> h, Node n) {
    ArrayList<Node> result = new ArrayList<Node>();
    result.add(n);
    Edge e = h.get(n.toString());
    Node prev = e.a;
    while (!prev.isStart()) {
      result.add(prev);
      e = h.get(prev.toString());
      prev = e.a;
    }
    result.add(prev);
    return result;
  }
}

// To represent a player in the game
class Player {
  // this player's location
  Node loc;
  // number of wrong moves made by the player
  int wrongMoves = 0;

  Player(Node loc) {
    this.loc = loc;
  }

  // return a WorldImage representation of this player
  void draw(WorldScene bg) {
    Posn p = this.loc.getPosn();
    WorldImage outline = new CircleImage(6, "solid", new Color(0, 0, 0));
    WorldImage player = new CircleImage(6, "solid", new Color(255, 255, 255));
    bg.placeImageXY(player.overlayImages(outline), p.x, p.y);
  }

  // attempt to move this player up, if they made a wrong move
  // add one to their wrongMoves
  void moveUp(ArrayList<Node> path) {
    if (this.loc.up instanceof Node) {
      this.loc = this.loc.up;
      if (!path.contains(this.loc)) {
        this.wrongMoves++;
      }
    }
    this.loc.visitByPlayer = true;
  }

  // attempt to move this player down, if they made a wrong move
  // add one to their wrongMoves
  void moveDown(ArrayList<Node> path) {
    if (this.loc.down instanceof Node) {
      this.loc = this.loc.down;
      if (!path.contains(this.loc)) {
        this.wrongMoves++;
      }
    }
    this.loc.visitByPlayer = true;
  }

  // attempt to move this player left, if they made a wrong move
  // add one to their wrongMoves
  void moveLeft(ArrayList<Node> path) {
    if (this.loc.left instanceof Node) {
      this.loc = this.loc.left;
      if (!path.contains(this.loc)) {
        this.wrongMoves++;
      }
    }
    this.loc.visitByPlayer = true;
  }

  // attempt to move this player right, if they made a wrong move
  // add one to their wrongMoves
  void moveRight(ArrayList<Node> path) {
    if (this.loc.right instanceof Node) {
      this.loc = this.loc.right;
      if (!path.contains(this.loc)) {
        this.wrongMoves++;
      }
    }
    this.loc.visitByPlayer = true;
  }

  // return a string revealing this player's score
  String getScore() {
    String result = "You made ";
    result = result + Integer.toString(this.wrongMoves) + " wrong moves";

    return result;
  }
}

// to compare two edges by their edge weights
class EdgeComparator implements Comparator<Edge> {
  // returns negative if e1 comes first, 0 if they are tied,
  // positive if e2 comes first
  public int compare(Edge e1, Edge e2) {
    return e1.weight - e2.weight;
  }
}

// to hold tests and examples for MazeWorlds
// some tests assume a 15x20 world
class ExamplesMazeWorld {
  Node n1;
  Node n2;
  Node n3;
  Node n4;
  Node n5;
  Node n6;
  Node n7;
  Node n8;
  Node n9;
  Node n10;

  Edge e12;
  Edge e23;
  Edge e45;
  Edge e56;
  Edge e78;
  Edge e89;
  Edge e14;
  Edge e25;
  Edge e36;
  Edge e47;
  Edge e58;
  Edge e69;

  Player p1;

  MazeWorld maze = new MazeWorld();

  // Example of a MazeWorld
  MazeWorld mw;
  MazeWorld myWorld;

  // Example of EdgeComparator
  Comparator<Edge> edgeComp = new EdgeComparator();

  // Example of ArrayList of Edges
  ArrayList<Edge> edgeList;

  // Examples of ArrayList of Nodes
  ArrayList<Node> l1;
  ArrayList<Node> l2;
  ArrayList<Node> l3;

  // Example of a 2D ArrayList of Nodes
  ArrayList<ArrayList<Node>> nodeList;

  // Example of HashMap<String, String>
  HashMap<String, String> hash = new HashMap<String, String>();
  HashMap<String, String> hash2 = new HashMap<String, String>();

  // Example of HashMap<String, Edge>
  HashMap<String, Edge> hash3 = new HashMap<String, Edge>();

  // Example of ArrayList of Integer
  ArrayList<Integer> arr1;
  ArrayList<Integer> arr2;
  ArrayList<Integer> arr3;
  ArrayList<Integer> arr4;
  ArrayList<ArrayList<Integer>> arrArr1;
  ArrayList<ArrayList<Integer>> arrArr2;
  ArrayList<Integer> resultArr1;
  ArrayList<Integer> resultArr2;

  // examples for posns
  Posn n1Posn = new Posn(MazeWorld.EDGEWIDTH / 2, MazeWorld.EDGEHEIGHT / 2);
  Posn n4Posn = new Posn(MazeWorld.EDGEWIDTH / 2, MazeWorld.EDGEHEIGHT + (MazeWorld.EDGEWIDTH / 2));
  Posn n5Posn = new Posn(MazeWorld.EDGEHEIGHT + (MazeWorld.EDGEWIDTH / 2),
      MazeWorld.EDGEHEIGHT + (MazeWorld.EDGEWIDTH / 2));

  // Initialize values
  void init() {
    n1 = new Node(0, 0);
    n2 = new Node(1, 0);
    n3 = new Node(2, 0);
    n4 = new Node(0, 1);
    n5 = new Node(1, 1);
    n6 = new Node(2, 1);
    n7 = new Node(0, 2);
    n8 = new Node(1, 2);
    n9 = new Node(2, 2);
    n10 = new Node(19, 14);

    e12 = new Edge(n1, n2, 1);
    e23 = new Edge(n2, n3, 5);
    e45 = new Edge(n4, n5, 8);
    e56 = new Edge(n5, n6, 3);
    e78 = new Edge(n7, n8, 7);
    e89 = new Edge(n8, n9, 9);
    e14 = new Edge(n1, n4, 1);
    e25 = new Edge(n2, n5, 4);
    e36 = new Edge(n3, n6, 11);
    e47 = new Edge(n4, n7, 50);
    e58 = new Edge(n5, n8, 3);
    e69 = new Edge(n6, n9, 5);

    edgeList = new ArrayList<Edge>(Arrays.asList(e36, e12));

    l1 = new ArrayList<Node>();
    l1.add(n1);
    l2 = new ArrayList<Node>();
    l2.add(n2);
    l3 = new ArrayList<Node>();
    l3.add(n3);

    nodeList = new ArrayList<ArrayList<Node>>();
    nodeList.add(l1);
    nodeList.add(l2);
    nodeList.add(l3);

    hash.clear();
    hash.put(n1.toString(), n1.toString());
    hash.put(n2.toString(), n2.toString());
    hash.put(n3.toString(), n3.toString());

    hash3.put(n2.toString(), e12);
    hash3.put(n5.toString(), e25);
    hash3.put(n6.toString(), e56);
    hash3.put(n9.toString(), e69);
    // ArrayLists of Integer
    arr1 = new ArrayList<Integer>();
    arr2 = new ArrayList<Integer>();
    arr3 = new ArrayList<Integer>();
    arr4 = new ArrayList<Integer>();
    arr1.add(1);
    arr2.add(2);
    arr3.add(3);
    arr4.add(4);
    // ArrayList of ArrayLists of Integer
    arrArr1 = new ArrayList<ArrayList<Integer>>();
    arrArr1.add(arr1);
    arrArr1.add(arr2);
    arrArr1.add(arr3);
    arrArr1.add(arr4);
    arrArr2 = new ArrayList<ArrayList<Integer>>();
    arrArr2.add(arr2);
    arrArr2.add(arr4);
    // Expected example after flattening the ArrayList of ArrayList of
    // Integer
    resultArr1 = new ArrayList<Integer>();
    resultArr1.add(1);
    resultArr1.add(2);
    resultArr1.add(3);
    resultArr1.add(4);
    resultArr2 = new ArrayList<Integer>();
    resultArr2.add(2);
    resultArr2.add(4);

    p1 = new Player(n1);
    myWorld = new MazeWorld();
  }

  // initialize mw
  void initMW() {
    mw = new MazeWorld();
  }

  // test the getPosn method
  boolean testGetPosn(Tester t) {
    init();
    return t.checkExpect(n1.getPosn(), n1Posn) && t.checkExpect(n5.getPosn(), n5Posn)
        && t.checkExpect(n4.getPosn(), n4Posn);
  }

  // test the Edge comparator and the sort method
  void testEdgeSort(Tester t) {
    init();
    t.checkExpect(edgeList.get(0), e36);
    // sort edgeList
    MazeUtils.sort(edgeList);
    t.checkExpect(edgeList.get(0), e12);

    t.checkExpect(edgeComp.compare(e12, e36) < 0, true);
    t.checkExpect(edgeComp.compare(e12, e12) == 0, true);
    t.checkExpect(edgeComp.compare(e36, e12) > 0, true);
  }

  // test the union and find methods
  void testUnionFind(Tester t) {
    init();
    t.checkExpect(MazeUtils.find(hash, n1.toString()), n1.toString());
    t.checkExpect(MazeUtils.find(hash, n2.toString()), n2.toString());
    t.checkExpect(MazeUtils.find(hash, n3.toString()), n3.toString());
    // union the nodes
    MazeUtils.union(hash, MazeUtils.find(hash, n1.toString()), MazeUtils.find(hash, n2.toString()));
    t.checkExpect(MazeUtils.find(hash, n1.toString()), n2.toString());
    // union the nodes
    MazeUtils.union(hash, MazeUtils.find(hash, n1.toString()), MazeUtils.find(hash, n3.toString()));
    t.checkExpect(MazeUtils.find(hash, n2.toString()), n3.toString());
  }

  // test the moreThanOneTree method
  void testMoreThanOneTree(Tester t) {
    init();
    t.checkExpect(MazeUtils.moreThanOneTree(hash), true);
    MazeUtils.union(hash, MazeUtils.find(hash, n1.toString()), MazeUtils.find(hash, n2.toString()));
    t.checkExpect(MazeUtils.moreThanOneTree(hash), true);
    MazeUtils.union(hash, MazeUtils.find(hash, n1.toString()), MazeUtils.find(hash, n3.toString()));
    t.checkExpect(MazeUtils.moreThanOneTree(hash), false);
  }

  // test the initRepresentatives method
  void testInitRepresentatives(Tester t) {
    init();
    initMW();
    // set hash2 with each node in nodeList pointing to itself
    mw.initRepresentatives(hash2, nodeList);
    t.checkExpect(hash, hash2);
  }

  // test the initializeNodes method
  void testInitializeNodes(Tester t) {
    init();
    initMW();
    ArrayList<ArrayList<Node>> nodes = mw.initializeNodes();
    t.checkExpect(nodes.get(0).get(0), n1);
    t.checkExpect(nodes.get(0).get(1), n4);
    t.checkExpect(nodes.get(1).get(0), n2);
    t.checkExpect(nodes.get(5).get(5).x == 5 && nodes.get(5).get(5).y == 5, true);
  }

  // test the connect method, then test movement methods for players
  void testConnect(Tester t) {
    // test connect
    init();
    MazeUtils.connect(e12);
    MazeUtils.connect(e23);
    MazeUtils.connect(e45);
    MazeUtils.connect(e56);
    MazeUtils.connect(e78);
    MazeUtils.connect(e89);
    MazeUtils.connect(e14);
    MazeUtils.connect(e47);
    MazeUtils.connect(e25);
    MazeUtils.connect(e58);
    MazeUtils.connect(e36);
    MazeUtils.connect(e69);
    t.checkExpect(n5.up, n2);
    t.checkExpect(n5.down, n8);
    t.checkExpect(n5.left, n4);
    t.checkExpect(n5.right, n6);
    t.checkExpect(n8.up, n5);
    t.checkExpect(n2.down, n5);
    t.checkExpect(n4.right, n5);
    t.checkExpect(n6.left, n5);

    // test movement
    t.checkExpect(p1.loc, n1);

    p1.moveRight(l3);
    t.checkExpect(p1.loc, n2);

    p1.moveDown(l2);
    t.checkExpect(p1.loc, n5);

    p1.moveLeft(l1);
    t.checkExpect(p1.loc, n4);

    p1.moveUp(l2);
    t.checkExpect(p1.loc, n1);
  }

  // test the compare method for edges
  boolean testCompare(Tester t) {
    init();
    return t.checkExpect(edgeComp.compare(e12, e36) < 0, true)
        && t.checkExpect(edgeComp.compare(e36, e23) > 0, true)
        && t.checkExpect(edgeComp.compare(e12, e12) == 0, true);
  }

  // test the toString method for nodes
  void testToString(Tester t) {
    init();
    t.checkExpect(n1.toString(), "0x0y");
    t.checkExpect(n2.toString(), "1x0y");
    t.checkExpect(n3.toString(), "2x0y");
  }

  // test the isStart and isTarget method for nodes
  void testIsStartIsTarget(Tester t) {
    init();
    t.checkExpect(n1.isStart(), true);
    t.checkExpect(n2.isStart(), false);
    t.checkExpect(n10.isTarget(), true);
    t.checkExpect(n1.isTarget(), false);
  }

  // test the getNeighbors method for nodes
  void testGetNeighbors(Tester t) {
    init();
    ArrayList<Node> nodeList = new ArrayList<Node>();
    t.checkExpect(n1.getNeighbors(), nodeList);

    MazeUtils.connect(e12);
    nodeList.add(n2);
    t.checkExpect(n1.getNeighbors(), nodeList);
  }

  // test the getScore method for players
  void testGetScore(Tester t) {
    init();
    t.checkExpect(p1.getScore(), "You made 0 wrong moves");
    MazeUtils.connect(e12);
    // empty arraylist means it must be a wrong move
    p1.moveRight(new ArrayList<Node>());
    t.checkExpect(p1.getScore(), "You made 1 wrong moves");
  }

  // test the getStart method
  void testGetStart(Tester t) {
    init();
    t.checkExpect(MazeUtils.getStart(nodeList), n1);
  }

  // test the sort method
  void testSort(Tester t) {
    init();
    ArrayList<Edge> testList = new ArrayList<Edge>(Arrays.asList(e23, e12, e56));
    MazeUtils.sort(testList);
    ArrayList<Edge> sortedList = new ArrayList<Edge>(Arrays.asList(e12, e56, e23));
    t.checkExpect(testList, sortedList);
  }

  // test the onTick method
  void testOnTick(Tester t) {
    // test the breadth-first portion
    init();
    initMW();
    mw.onTick();
    t.checkExpect(mw.breadthFirstWorkList.size(), 0);
    // sets the tickSearch field to "bfs" and
    // adds the start node to the worklist
    mw.onKeyEvent("b");
    t.checkExpect(mw.breadthFirstWorkList.size(), 1);
    Node tempB = mw.breadthFirstWorkList.peek();
    t.checkExpect(tempB.visited, false);
    t.checkExpect(tempB.x == 0 && tempB.y == 0, true);
    // tempB becomes visited, the worklist is updated
    // and no longer has tempB at its head
    mw.onTick();
    t.checkExpect(tempB.visited, true);
    t.checkExpect(mw.breadthFirstWorkList.peek() != tempB, true);
    // test the depth-first portion
    init();
    initMW();
    t.checkExpect(mw.depthFirstWorkList.size(), 0);
    // sets the tickSearch field to "dfs" and
    // adds the start node to the worklist
    mw.onKeyEvent("d");
    t.checkExpect(mw.depthFirstWorkList.size(), 1);
    Node tempD = mw.depthFirstWorkList.peek();
    t.checkExpect(tempD.visited, false);
    t.checkExpect(tempD.x == 0 && tempD.y == 0, true);
    // tempD becomes visited, the worklist is updated
    // and no longer has tempD as its top
    mw.onTick();
    t.checkExpect(tempD.visited, true);
    t.checkExpect(mw.depthFirstWorkList.peek() != tempD, true);
  }

  //
  // test the reconstruct methods
  void testReconstruct(Tester t) {
    init();
    initMW();
    t.checkExpect(n1.path, false);
    t.checkExpect(n4.path, false);
    t.checkExpect(n6.path, false);
    // sets the path field of nodes in the path to the
    // end to true
    mw.reconstruct(hash3, n9);
    t.checkExpect(n1.path, true);
    t.checkExpect(n4.path, false);
    t.checkExpect(n6.path, true);

    init();
    ArrayList<Node> path = MazeUtils.reconstruct(hash3, n9);
    t.checkExpect(path.contains(n1), true);
    t.checkExpect(path.contains(n7), false);
    t.checkExpect(path.contains(n9), true);
  }

  // test the depthFirstSearch method
  void testDepthFirstSearch(Tester t) {
    init();
    initMW();
    // mw.path is the result of the depthFirstSearch() method
    ArrayList<Node> tempPath = mw.path;
    for (Node n : tempPath) {
      t.checkExpect(n.hiddenVisited, true);
    }
  }

  // test the initializeEdges method
  void testInitializeEdges(Tester t) {
    init();
    initMW();
    ArrayList<Edge> edges = mw.initializeEdges();
    for (Edge e : edges) {
      Node a = e.a;
      Node b = e.b;
      // the nodes are adjacent
      t.checkExpect(Math.abs(a.x - b.x) == 1 ^ Math.abs(a.y - b.y) == 1, true);
      // the weight is between 0 and 1m
      t.checkRange(e.weight, 0, 1000000);
    }
  }

  // test the initializeMaze method
  void testInitializeMaze(Tester t) {
    init();
    initMW();
    t.checkExpect(mw.edges.size(), mw.nodes.size() * mw.nodes.get(0).size() - 1);
  }

  // test the onKey event method
  void testOnKey(Tester t) {
    init();
    // input block at initial state
    t.checkExpect(myWorld.newSearchBlock, false);
    t.checkExpect(myWorld.tickSearch, "");
    // start breadth-first search by pressing "b"
    myWorld.onKeyEvent("b");
    t.checkExpect(myWorld.newSearchBlock, true);
    t.checkExpect(myWorld.tickSearch, "bfs");
    // Resets the world to its initial state by pressing "r"
    myWorld.onKeyEvent("r");
    t.checkExpect(myWorld.newSearchBlock, true);
    t.checkExpect(myWorld.tickSearch, "bfs");
    // start depth-first search by pressing "d"
    myWorld.onKeyEvent("d");
    t.checkExpect(myWorld.newSearchBlock, true);
    t.checkExpect(myWorld.tickSearch, "bfs");

    Node prev = myWorld.player.loc;
    t.checkExpect(myWorld.player.loc.equals(prev), true);

    myWorld.onKeyEvent("r");

    myWorld.PATHVIS = true;
    t.checkExpect(myWorld.PATHVIS, true);
    // EFFECT toggles path visibility
    myWorld.onKeyEvent("t");
    t.checkExpect(myWorld.PATHVIS, false);

    // Tests for player movements.
    myWorld.player = p1;
    MazeUtils.connect(e12);
    MazeUtils.connect(e14);
    MazeUtils.connect(e25);
    MazeUtils.connect(e45);
    // moves the player down a node
    myWorld.onKeyEvent("down");
    t.checkExpect(myWorld.player.loc, n4);

    // moves the player right a node
    myWorld.onKeyEvent("right");
    t.checkExpect(myWorld.player.loc, n5);

    // moves the player up a node
    myWorld.onKeyEvent("up");
    t.checkExpect(myWorld.player.loc, n2);

    // moves the player left a node
    myWorld.onKeyEvent("left");
    t.checkExpect(myWorld.player.loc, n1);

    myWorld.bias = 1;
    // cycle through the direction biases
    myWorld.onKeyEvent("w");
    t.checkExpect(myWorld.bias, 2);
  }

  void testBigBang(Tester t) {
    maze.bigBang(800, 620, 0.01);
  }
}