package de.htwg.se.puzzlerun.controller.Impl1

import java.io._
import java.util.Properties

import de.htwg.se.puzzlerun.controller.IController
import de.htwg.se.puzzlerun.model.Impl1._

import scala.collection.mutable.Map
import scala.swing.Publisher
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

import scala.io.Source
case class jsonLevel(grid: Grid, player: Player, target: Target, obstacles: List[Obstacle], moves: Map[String, Int])
class Controller(path: String) extends IController with Publisher {
  implicit val formats = net.liftweb.json.DefaultFormats
  var state = ""
  var level = 0
  var grid: Grid = _
  var obstacles = scala.collection.mutable.MutableList[Obstacle]()
  var target: Target = _
  var player: Player = _
  var moves: Map[String, Int] = _
  parseJSONLevel(path)
  //generate_level(path)
  wrap()

  def wrap(): Unit = {
    val grid = this.grid.createGrid()
    for (i <- grid.indices; j <- grid(0).indices) {

      for (obstacle <- this.obstacles) {

        if ((i, j) == obstacle.coordinate) {
          this.grid.setCell(i, j, obstacle)
        }
      }

      if ((i, j) == target.coordinate) {
        this.grid.setCell(i, j, this.target)
      }

      if ((i, j) == this.player.coordinate) {
        this.grid.setCell(i, j, this.player)
      }

    }
  }

  def move(x: Int, y: Int): Unit = {

    checkCell(x, y)
    this.grid.setCell(player.x, player.y, new Cell)
    player.coordinate = (x, y)
    if(state.equals("Target reached"))
      generate_level("level0"+level+".config")
    wrap()
    notifyObservers
  }

  def up(): Unit = {
    /*
    Moves the player one field up.
     */
    val y = this.player.coordinate._2
    if(this.player.coordinate._1 - 1 < 0)
      return
    val x = this.player.coordinate._1 - 1
    move(x, y)
    val newAmount = checkMoves(moves.get("Up").get)
    moves.put("Up", newAmount)

  }

  def down(): Unit = {
    /*
    Moves the player one field down.
     */
    val y = this.player.coordinate._2
    if(this.player.coordinate._1 + 1 > grid.height-1)
      return
    val x = this.player.coordinate._1 + 1

    move(x, y)
    val newAmount = checkMoves(moves.get("Down").get)
    moves.put("Down", newAmount)
  }

  def right(): Unit = {
    /*
    Moves the player one field to the right.
     */
    if(this.player.coordinate._2 + 1 > grid.length-1)
      return
    val y = this.player.coordinate._2 + 1
    val x = this.player.coordinate._1
    move(x, y)
    val newAmount = checkMoves(moves.get("Right").get)
    moves.put("Right", newAmount)
  }

  def left(): Unit = {
    /*
    Moves the player one field to the left
     */
    if(this.player.coordinate._2 - 1 < 0)
      return
    val y = this.player.coordinate._2 - 1
    val x = this.player.coordinate._1
    move(x, y)
    val newAmount = checkMoves(moves.get("Left").get)
    moves.put("Left", newAmount)
  }

  def checkCell(x: Int, y: Int): Unit = {
    /*
    Checks whether the cell to be accessed is of type Obstacle or Target.
    Sets state accordingly for the game loop.
     */
    try {
      if (grid.getCell(x, y).isInstanceOf[Obstacle]) {
        state = "Obstacle reached"
      } else if (grid.getCell(x, y).isInstanceOf[Target]) {
        state = "Target reached"
        level += 1

      }
      else{
        state = ""
      }

    } catch {
      case bound: java.lang.ArrayIndexOutOfBoundsException => state = "Obstacle reached"
    }
  }

  def checkMoves(amount: Int): Int = {
    /*
    Checks whether the player has exceeded the moves limit.
     */
    if (amount == 0) state = "Moves depleted"
    var newAmount = amount - 1
    if (newAmount < 0) newAmount = 0 // Keep moves at 0 and don't go negative
    newAmount
  }

  def checkEingabeLength(eingabeLength: Int): Boolean = {

    eingabeLength == 0
  }

  def generate_level(path: String): Unit = {

    val sg: ParseStrategy = new TextParser
    val context = new Context(sg, this)
    context.execute(path)
  }
  def generateJSONLevel(path: String): Unit = {
    def getCurrentDirectory = new java.io.File(".").getCanonicalPath
    val filename = path.substring(0, 7)
    val tempObstacle = this.obstacles.map(o => List(o.coordinate._1, o.coordinate._2))
    val json =
          ("grid" -> List(this.grid.height.toString, this.grid.length.toString)) ~
          ("player" -> List(this.player.coordinate._1, this.player.coordinate._2)) ~
          ("target" -> List(this.target.coordinate._1, this.target.coordinate._2)) ~
          ("obstacles" -> tempObstacle) ~
          ("moves" -> this.moves)

    val file = new File(getCurrentDirectory + "/src/levels/" + filename + ".json")
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(pretty(render(json)))
    bw.close()
    parseJSONLevel(filename + ".json")
  }
  def parseJSONLevel(path: String): Unit = {
    def getCurrentDirectory = new java.io.File(".").getCanonicalPath
    val filename: String = getCurrentDirectory + "/src/levels/" + path
    val json = parse(Source.fromFile(filename).mkString)
    this.grid = Grid(json.\("grid").children.head.extract[String].toInt, json.\("grid").children(1).extract[String].toInt)
    this.player = Player(json.\("player").children.head.extract[Int], json.\("player").children(1).extract[Int])
    this.target = Target(json.\("target").children.head.extract[Int], json.\("target").children(1).extract[Int])
    for(i <- 0 until json.\("obstacles").children.length)
      this.obstacles += Obstacle(json.\("obstacles").children(i)(0).extract[Int], json.\("obstacles").children(i)(1).extract[Int])
    this.moves = Map("Up" -> json.\("moves").children(2).children(0).extract[Int],
      "Down" -> json.\("moves").children(1).children(0).extract[Int],
      "Left" -> json.\("moves").children(3).children(0).extract[Int],
      "Right" -> json.\("moves").children(0).children(0).extract[Int])
    }
}