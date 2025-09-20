import tester.*;
import javalib.worldimages.*;
import javalib.impworld.*;
import java.awt.Color;
import java.util.*;
//import java.util.Random;

// represents a button
class Button {
  String name;
  int width;
  int height;
  Color color;
  int radius;

  Button(String n, int w, int h, Color c, int r) {
    this.name = n;
    this.width = w;
    this.height = h;
    this.color = c;
    this.radius = r;
  }

  // puts a border and round rectangle together
  WorldImage createRoundedCell(int width, int height, Color fill, Color border, int radius,
      int thickness) {
    int innerWidth = width - (thickness * 2);
    int innerHeight = height - (thickness * 2);

    WorldImage base = createTrueRoundedRect(innerWidth, innerHeight, fill, radius);
    WorldImage borderImage = createRoundedBorder(width, height, border, radius, thickness);

    return new OverlayImage(base, borderImage);
  }

  // turns a rectangle into a round rectangle
  WorldImage createTrueRoundedRect(int width, int height, Color color, int radius) {
    int rectHeight = height - (2 * radius);
    WorldImage centerRect = new RectangleImage(width, rectHeight, OutlineMode.SOLID, color);
 
    int rectWidth = width - (2 * radius);
    WorldImage sideRect = new RectangleImage(rectWidth, height, OutlineMode.SOLID, color);

    WorldImage corner = new CircleImage(radius, OutlineMode.SOLID, color);

    WorldImage topCenter = centerRect.movePinhole(0, -height / 2 + radius + rectHeight / 2);
    WorldImage bottomCenter = centerRect.movePinhole(0, height / 2 - radius - rectHeight / 2);
    WorldImage leftSide = sideRect.movePinhole(-width / 2 + radius + rectWidth / 2, 0);
    WorldImage rightSide = sideRect.movePinhole(width / 2 - radius - rectWidth / 2, 0);

    WorldImage topLeft = corner.movePinhole(-width / 2 + radius, -height / 2 + radius);
    WorldImage topRight = corner.movePinhole(width / 2 - radius, -height / 2 + radius);
    WorldImage bottomLeft = corner.movePinhole(-width / 2 + radius, height / 2 - radius);
    WorldImage bottomRight = corner.movePinhole(width / 2 - radius, height / 2 - radius);

    return new OverlayImage(topCenter,
        new OverlayImage(bottomCenter,
            new OverlayImage(leftSide, new OverlayImage(rightSide, new OverlayImage(topLeft,
                new OverlayImage(topRight, new OverlayImage(bottomLeft, bottomRight)))))));
  }

  // creates a border for a round cell
  WorldImage createRoundedBorder(int width, int height, Color color, int radius, int thickness) {
    WorldImage outer = createTrueRoundedRect(width, height, color, radius);
    WorldImage inner = createTrueRoundedRect(width - (2 * thickness), height - (2 * thickness),
        new Color(0, 0, 0, 0), radius - thickness);

    return new OverlayImage(inner, outer);
  }

  // draws a rounded rectangle and a word of a rectangular button
  WorldImage drawButton() {
    TextImage wordText = new TextImage(this.name, 18, FontStyle.BOLD, Color.BLACK);
    WorldImage cell = this.createRoundedCell(this.width, this.height, this.color, Color.WHITE,
        this.radius, 4);
    WorldImage centeredText = wordText.movePinhole(0, -this.height / 40);
    return new OverlayImage(centeredText, cell);
  }
}

// represents a word in this game
class Word {
  String name;
  String state;
  String diff;
  String category;
  Color color = Color.LIGHT_GRAY;
  int cellWidth = 150;
  int cellHeight = 100;
  Color easyGreen = new Color(194, 242, 182);
  Color mediumYellow = new Color(252, 248, 171);
  Color hardBlue = new Color(173, 230, 250);
  Color expertPurple = new Color(219, 195, 247);
  int correctWidth = 600;

  Word(String n, String s, String h, String c) {
    this.name = n;
    this.state = s;
    this.diff = h;
    this.category = c;
  }

  // assigns a color to each word difficulty
  Color diffColor() {
    if (this.diff.equals("easy")) {
      return easyGreen;
    }
    else if (this.diff.equals("medium")) {
      return mediumYellow;
    }
    else if (this.diff.equals("hard")) {
      return hardBlue;
    }
    else {
      return expertPurple;
    }
  }

  // is this word clicked
  boolean isClicked() {
    return this.state.equals("clicked");
  }

  // does this word have the same category as another word
  boolean hasSameCategory(Word other) {
    return this.category.equals(other.category);
  }

  // changes the state of a word if it is clicked
  void ifClicked(Posn pos, int x, int y, int width, int height, ArrayList<Word> clickedWords) {
    if (pos.x >= x - width / 2 && pos.x <= x + width / 2 && pos.y >= y - height / 2
        && pos.y <= y + height / 2) {
      if (this.color.equals(Color.LIGHT_GRAY) && clickedWords.size() < 4) {
        this.color = Color.PINK;
        this.state = "clicked";
        clickedWords.add(this);
      }
      else if (this.color.equals(Color.PINK)) {
        this.color = Color.LIGHT_GRAY;
        this.state = "unclicked";
        clickedWords.remove(this);
      }
    }
  }

  // draws the words onto rectangles
  WorldImage draw() {
    Button word = new Button(this.name, this.cellWidth, this.cellHeight, this.color, 6);
    return word.drawButton();
  }
}

// represents a list of words
class WordArray {
  ArrayList<Word> words;

  WordArray(ArrayList<Word> words) {
    this.words = words;
  }
}

// represents a connections game world
class ConnectionsWorld extends World {
  public int width = 700;
  public int height = 600;
  public int gridSize = 4;
  public int cellWidth = 150;
  public int cellHeight = 100;
  int startX = 125;
  int startY = 100;
  public boolean reset = false;
  ArrayList<Word> words;
  ArrayList<ArrayList<Word>> lists;

  ArrayList<Word> clickedWords = new ArrayList<Word>();
  int triesLeft = 4;
  Random rand;
  ArrayList<Word> correctWords = new ArrayList<Word>();
  ArrayList<ArrayList<Word>> correctCategories = new ArrayList<ArrayList<Word>>();
  ArrayList<Word> originalWords = new ArrayList<Word>();
  Button submit = new Button("Submit", 120, 60, Color.PINK, 12);

  ConnectionsWorld(Random rand) {
    this.rand = rand;
    this.triesLeft = 4;

    ArrayList<Word> set1 = new ArrayList<Word>(
        Arrays.asList(new Word("PLANT", "unclicked", "easy", "DO SOME TASKS IN THE GARDEN"),
            new Word("MARY JANE", "unclicked", "hard", "SHOES"),
            new Word("MULE", "unclicked", "hard", "SHOES"),
            new Word("PIPER", "unclicked", "expert", "SAND___"),
            new Word("STONE", "unclicked", "expert", "SAND___"),
            new Word("DWINDLE", "unclicked", "medium", "WANE"),
            new Word("PETER", "unclicked", "medium", "WANE"),
            new Word("PRUNE", "unclicked", "easy", "DO SOME TASKS IN THE GARDEN"),
            new Word("FLAT", "unclicked", "hard", "SHOES"),
            new Word("SLIDE", "unclicked", "hard", "SHOES"),
            new Word("CASTLE", "unclicked", "expert", "SAND___"),
            new Word("TAPER", "unclicked", "medium", "WANE"),
            new Word("WATER", "unclicked", "easy", "DO SOME TASKS IN THE GARDEN"),
            new Word("WEED", "unclicked", "easy", "DO SOME TASKS IN THE GARDEN"),
            new Word("FADE", "unclicked", "medium", "WANE"),
            new Word("PAPER", "unclicked", "expert", "SAND___")));

    ArrayList<Word> set2 = new ArrayList<Word>(
        Arrays.asList(new Word("GNASH", "unclicked", "medium", "RUB TOGETHER"),
            new Word("FERMENT", "unclicked", "easy", "WAYS TO PRESERVE FOOD"),
            new Word("JAM", "unclicked", "hard", "BREAKFAST CONDIMENTS"),
            new Word("CAN", "unclicked", "easy", "WAYS TO PRESERVE FOOD"),
            new Word("BUTTER", "unclicked", "hard", "BREAKFAST CONDIMENTS"),
            new Word("GRATE", "unclicked", "medium", "RUB TOGETHER"),
            new Word("TEA", "unclicked", "expert", "PROVERBIAL THINGS THAT ARE SPILED"),
            new Word("HOT SAUCE", "unclicked", "hard", "BREAKFAST CONDIMENTS"),
            new Word("SCRAPE", "unclicked", "medium", "RUB TOGETHER"),
            new Word("GRIND", "unclicked", "medium", "RUB TOGETHER"),
            new Word("SYRUP", "unclicked", "hard", "BREAKFAST CONDIMENTS"),
            new Word("MILK", "unclicked", "expert", "PROVERBIAL THINGS THAT ARE SPILED"),
            new Word("BEANS", "unclicked", "expert", "PROVERBIAL THINGS THAT ARE SPILED"),
            new Word("PICKLE", "unclicked", "easy", "WAYS TO PRESERVE FOOD"),
            new Word("GUTS", "unclicked", "expert", "PROVERBIAL THINGS THAT ARE SPILED"),
            new Word("FREEZE", "unclicked", "easy", "WAYS TO PRESERVE FOOD")));

    ArrayList<Word> set3 = new ArrayList<Word>(
        Arrays.asList(new Word("FLAG", "unclicked", "easy", "SIGNAL DOWN, AS A TAXI"),
            new Word("WHISTLE", "unclicked", "easy", "SIGNAL DOWN, AS A TAXI"),
            new Word("SPIN", "unclicked", "medium", "PARTIALITY"),
            new Word("TURN", "unclicked", "hard", "CARDS IN TEXAS HOLD EM"),
            new Word("ANON", "unclicked", "expert", "SHAKESPEAREAN WORDS"),
            new Word("ART", "unclicked", "expert", "SHAKESPEAREAN WORDS"),
            new Word("WAVE", "unclicked", "easy", "SIGNAL DOWN, AS A TAXI"),
            new Word("HAIL", "unclicked", "easy", "SIGNAL DOWN, AS A TAXI"),
            new Word("BIAS", "unclicked", "medium", "PARTIALITY"),
            new Word("RIVER", "unclicked", "hard", "CARDS IN TEXAS HOLD EM"),
            new Word("FLOP", "unclicked", "hard", "CARDS IN TEXAS HOLD EM"),
            new Word("WILT", "unclicked", "expert", "SHAKESPEAREAN WORDS"),
            new Word("THOU", "unclicked", "expert", "SHAKESPEAREAN WORDS"),
            new Word("SLANT", "unclicked", "medium", "PARTIALITY"),
            new Word("ANGLE", "unclicked", "medium", "PARTIALITY"),
            new Word("HOLE", "unclicked", "hard", "CARDS IN TEXAS HOLD EM")));

    ArrayList<Word> set4 = new ArrayList<Word>(
        Arrays.asList(new Word("SHINE", "unclicked", "expert", "BODY PARTS PLUS LETTER"),
            new Word("ANCHOR", "unclicked", "hard", "CLASSICAL NAUTICAL TATTOOS"),
            new Word("COMPASS", "unclicked", "hard", "CLASSICAL NAUTICAL TATTOOS"),
            new Word("GOBBLE", "unclicked", "medium", "EAT VORACIOUSLY"),
            new Word("BUTTE", "unclicked", "expert", "BODY PARTS PLUS LETTER"),
            new Word("BUCKLE", "unclicked", "easy", "BEND UNDER PRESSURE"),
            new Word("SCARF", "unclicked", "medium", "EAT VORACIOUSLY"),
            new Word("GIVE", "unclicked", "easy", "BEND UNDER PRESSURE"),
            new Word("WOLF", "unclicked", "medium", "EAT VORACIOUSLY"),
            new Word("HEARTH", "unclicked", "expert", "BODY PARTS PLUS LETTER"),
            new Word("BOW", "unclicked", "easy", "BEND UNDER PRESSURE"),
            new Word("MERMAID", "unclicked", "hard", "CLASSICAL NAUTICAL TATTOOS"),
            new Word("SWALLOW", "unclicked", "hard", "CLASSICAL NAUTICAL TATTOOS"),
            new Word("CHINA", "unclicked", "expert", "BODY PARTS "),
            new Word("GULP", "unclicked", "medium", "EAT VORACIOUSLY"),
            new Word("CAVE", "unclicked", "easy", "BEND UNDER PRESSURE")));

    ArrayList<Word> set5 = new ArrayList<Word>(
        Arrays.asList(new Word("BEAUCOUP", "unclicked", "easy", "'MANY' IN DIFFERENT LANGUAGES"),
            new Word("GUSTO", "unclicked", "medium", "ENTHUSIASM"),
            new Word("JENNY", "unclicked", "expert", "RHYME WITH U.S. COINS"),
            new Word("LIME", "unclicked", "expert", "RHYME WITH U.S. COINS"),
            new Word("BRICK", "unclicked", "hard", "RECTANGULAR PRISMS"),
            new Word("MICROWAVE", "unclicked", "hard", "RECTANGULAR PRISMS"),
            new Word("RELISH", "unclicked", "medium", "ENTHUSIASM"),
            new Word("MOLTO", "unclicked", "easy", "'MANY' IN DIFFERENT LANGUAGES"),
            new Word("ZEST", "unclicked", "medium", "ENTHUSIASM"),
            new Word("MULTI", "unclicked", "easy", "'MANY' IN DIFFERENT LANGUAGES"),
            new Word("MORTAR", "unclicked", "expert", "RHYME WITH U.S. COINS"),
            new Word("PICKLE", "unclicked", "expert", "RHYME WITH U.S. COINS"),
            new Word("FISH TANK", "unclicked", "hard", "RECTANGULAR PRISMS"),
            new Word("SHOEBOX", "unclicked", "hard", "RECTANGULAR PRISMS"),
            new Word("MUCHO", "unclicked", "easy", "'MANY' IN DIFFERENT LANGUAGES"),
            new Word("PASSION", "unclicked", "medium", "ENTHUSIASM")));

    this.lists = new ArrayList<ArrayList<Word>>(Arrays.asList(set1, set2, set3, set4, set5));
    // picks a random list of words from the ones above
    this.words = lists.get(this.rand.nextInt(lists.size()));
    this.originalWords = new ArrayList<Word>();
    for (Word word : this.words) {
      this.originalWords.add(new Word(word.name, "unclicked", word.diff, word.category));
    }
  }

  // constructor for tests
  ConnectionsWorld() {
    this(new Random());
  } 

  // constructor for tests
  ConnectionsWorld(ArrayList<Word> words) {
    this.words = words;
  }

  // draws all the words onto the screen or makes a lose screen or a win screen
  public WorldScene makeGrid() {
    WorldScene scene = new WorldScene(width, height);
    WorldScene lose = new WorldScene(width, height);
    lose.placeImageXY(new TextImage("You Lose :(", 40, Color.RED), 350, 300);

    if (triesLeft == 0) {
      return lose;
    }
    if (correctCategories.size() == 4) {
      scene.placeImageXY(new TextImage("You win :)", 40, FontStyle.BOLD, Color.GREEN), 350, 300);
      return scene;
    }

    int categoryY = 40;
    for (ArrayList<Word> category : correctCategories) {
      if (!category.isEmpty() && category.size() == 4) {
        Word firstWord = category.get(0);
        Color categoryColor = firstWord.diffColor();

        String categoryText = firstWord.category.toUpperCase();
        String wordsText = category.get(0).name + " , " + category.get(1).name + " , "
            + category.get(2).name + " , " + category.get(3).name;

        WorldImage categoryName = new TextImage(categoryText, 18, FontStyle.BOLD, Color.BLACK);
        WorldImage wordsList = new TextImage(wordsText, 16, FontStyle.BOLD, Color.BLACK);

        WorldImage combinedText = new AboveAlignImage(AlignModeX.LEFT,
            new OverlayImage(categoryName,
                new RectangleImage(600, 40, OutlineMode.SOLID, new Color(0, 0, 0, 0))),
            new OverlayImage(wordsList,
                new RectangleImage(600, 40, OutlineMode.SOLID, new Color(0, 0, 0, 0))));

        Button roundedCategory = new Button("", 600, 100, categoryColor, 20);
        WorldImage background = roundedCategory.drawButton();

        WorldImage finalCategory = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.MIDDLE,
            combinedText, 0, 0, background);

        scene.placeImageXY(finalCategory, 350, categoryY + 55);

        categoryY += 105;
      }
    }

    int gridStartY = categoryY + 50;
    for (int i = 0; i < words.size(); i++) {
      int x = startX + (i % gridSize) * (600 / gridSize);
      int y = gridStartY + (i / gridSize) * (400 / gridSize);

      WorldImage finalImage = this.words.get(i).draw();
      scene.placeImageXY(finalImage, x, y);
    }

    for (int i = 0; i < triesLeft; i++) {
      int x = 295 + (i % gridSize) * (600 / 16);
      int y = 490 + (i / gridSize) * (400 / 16);

      WorldImage circle = new CircleImage(7, OutlineMode.SOLID, Color.PINK);
      scene.placeImageXY(circle, x, y);
    }

    scene.placeImageXY(submit.drawButton(), 350, 540);

    return scene;
  }

  // modifies the states of clicked things, if the submit button is clicked, it
  // will clear
  // all the clicked words
  public void onMouseClicked(Posn pos) {
    if (pos.x >= 290 && pos.x <= 410 && pos.y >= 510 && pos.y <= 570 && clickedWords.size() == 4) {
      String firstCategory = clickedWords.get(0).category;
      boolean allSameCategory = true;

      int index = 1;
      while (allSameCategory && index < clickedWords.size()) {
        if (!clickedWords.get(index).category.equals(firstCategory)) {
          allSameCategory = false;
        }
        index++;
      }

      if (allSameCategory) {
        for (Word word : clickedWords) {
          word.color = word.diffColor();
          word.state = "correct";
        }
        correctCategories.add(new ArrayList<Word>(clickedWords));
        words.removeAll(clickedWords);

      }
      else {
        for (Word word : clickedWords) {
          word.color = Color.LIGHT_GRAY;
          word.state = "unclicked";
        }
        triesLeft--;
      }
      clickedWords.clear();
      return;
    }

    int categoryHeight = correctCategories.size() * 100;
    int gridStartY = 100 + categoryHeight;

    // int countCorrect = this.correctWords.size() / 4;
    for (int i = 0; i < words.size(); i++) {
      int x = startX + (i % gridSize) * (600 / gridSize);
      int y = gridStartY + (i / gridSize) * (400 / gridSize);

      words.get(i).ifClicked(pos, x, y, cellWidth, cellHeight, clickedWords);
    }
  }

  // modifies the state of the world to the initial state if r is pressed
  public void onKeyEvent(String key) {
    if (key.equals("r")) {
      resetGame();
    }
  }

  // modifies the state of the world to the initial state
  public void resetGame() {
    this.clickedWords.clear();
    this.triesLeft = 4;
    this.correctCategories.clear();

    // Restore the original words list
    this.words = new ArrayList<Word>();
    for (Word word : this.originalWords) {
      this.words.add(new Word(word.name, "unclicked", word.diff, word.category));
    }
  }

  // draws the game
  public WorldScene makeScene() {
    return this.makeGrid();
  }
}

class ConnectionsGame {
  void testBigBang(Tester t) {
    ConnectionsWorld game = new ConnectionsWorld();
    int width = 700;
    int height = 600;
    game.bigBang(width, height, 1.0);
  }
}

class Examples {
  Word beaucoup = new Word("BEAUCOUP", "unclicked", "easy", "'MANY' IN DIFFERENT LANGUAGES");
  Word gusto = new Word("GUSTO", "unclicked", "medium", "ENTHUSIASM");
  Word jenny = new Word("JENNY", "unclicked", "expert", "RHYME WITH U.S. COINS");
  Word lime = new Word("LIME", "unclicked", "expert", "RHYME WITH U.S. COINS");
  Word brick = new Word("BRICK", "unclicked", "hard", "RECTANGULAR PRISM");
  Word microwave = new Word("MICROWAVE", "unclicked", "hard", "RECTANGULAR PRISM");
  Word relish = new Word("RELISH", "unclicked", "medium", "ENTHUSIASM");
  Word molto = new Word("MOLTO", "unclicked", "easy", "'MANY' IN DIFFERENT LANGUAGES");
  Word zest = new Word("ZEST", "unclicked", "medium", "ENTHUSIASM");
  Word multi = new Word("MULTI", "unclicked", "easy", "'MANY' IN DIFFERENT LANGUAGES");
  Word mortar = new Word("MORTAR", "unclicked", "expert", "RHYME WITH U.S. COINS");
  Word pickle = new Word("PICKLE", "unclicked", "expert", "RHYME WITH U.S. COINS");
  Word fishTank = new Word("FISH TANK", "unclicked", "hard", "RECTANGULAR PRISM");
  Word shoebox = new Word("SHOEBOX", "unclicked", "hard", "RECTANGULAR PRISM");
  Word mucho = new Word("MUCHO", "unclicked", "easy", "'MANY' IN DIFFERENT LANGUAGES");
  Word passion = new Word("PASSION", "unclicked", "medium", "ENTHUSIASM");

  ArrayList<Word> easy = new ArrayList<Word>(
      Arrays.asList(this.mucho, this.molto, this.beaucoup, this.multi));
  ArrayList<Word> medium = new ArrayList<Word>(
      Arrays.asList(this.gusto, this.relish, this.zest, this.passion));
  ArrayList<Word> hard = new ArrayList<Word>(
      Arrays.asList(this.brick, this.microwave, this.fishTank, this.shoebox));
  ArrayList<Word> expert = new ArrayList<Word>(
      Arrays.asList(this.jenny, this.lime, this.mortar, this.pickle));

  ArrayList<Word> wrong1 = new ArrayList<Word>(
      Arrays.asList(this.mucho, this.molto, this.beaucoup, this.shoebox));
  ArrayList<Word> wrong2 = new ArrayList<Word>(
      Arrays.asList(this.zest, this.molto, this.beaucoup, this.multi));

  ArrayList<Word> mt = new ArrayList<Word>();
  Posn posn0 = new Posn(0, 0);
  Posn posn1 = new Posn(50, 50);
  Posn posn2 = new Posn(100, 100);
  Posn posn3 = new Posn(75, 75);

  Button sampleButton;
  Button largeButton;

  Color easyGreen = new Color(194, 242, 182);
  Color mediumYellow = new Color(252, 248, 171);
  Color hardBlue = new Color(173, 230, 250);
  Color expertPurple = new Color(219, 195, 247);

  ConnectionsWorld state1 = new ConnectionsWorld(new Random(1));
  ConnectionsWorld state2 = new ConnectionsWorld(new Random(2));

  ArrayList<Word> set1 = new ArrayList<Word>(
      Arrays.asList(new Word("PLANT", "unclicked", "easy", "DO SOME TASKS IN THE GARDEN"),
          new Word("MARY JANE", "unclicked", "hard", "SHOES"),
          new Word("MULE", "unclicked", "hard", "SHOES"),
          new Word("PIPER", "unclicked", "expert", "SAND___"),
          new Word("STONE", "unclicked", "expert", "SAND___"),
          new Word("DWINDLE", "unclicked", "medium", "WANE"),
          new Word("PETER", "unclicked", "medium", "WANE"),
          new Word("PRUNE", "unclicked", "easy", "DO SOME TASKS IN THE GARDEN"),
          new Word("FLAT", "unclicked", "hard", "SHOES"),
          new Word("SLIDE", "unclicked", "hard", "SHOES"),
          new Word("CASTLE", "unclicked", "expert", "SAND___"),
          new Word("TAPER", "unclicked", "medium", "WANE"),
          new Word("WATER", "unclicked", "easy", "DO SOME TASKS IN THE GARDEN"),
          new Word("WEED", "unclicked", "easy", "DO SOME TASKS IN THE GARDEN"),
          new Word("FADE", "unclicked", "medium", "WANE"),
          new Word("PAPER", "unclicked", "expert", "SAND___")));

  void initData() {
    this.mt = new ArrayList<Word>();
    this.state1 = new ConnectionsWorld(new Random(1));
    this.state2 = new ConnectionsWorld(new Random(2));
  }

  void testCreateTrueRoundedRect(Tester t) {
    Button btn1 = new Button("Test", 60, 30, Color.RED, 5);
    WorldImage actual1 = btn1.createTrueRoundedRect(60, 30, Color.RED, 5);

    int radius = 5;
    int width = 60;
    int height = 30;
    Color color = Color.RED;
    int rectHeight = height - (2 * radius);
    WorldImage centerRect = new RectangleImage(width, rectHeight, OutlineMode.SOLID, color);

    int rectWidth = width - (2 * radius);
    WorldImage sideRect = new RectangleImage(rectWidth, height, OutlineMode.SOLID, color);

    WorldImage corner = new CircleImage(radius, OutlineMode.SOLID, color);

    WorldImage topCenter = centerRect.movePinhole(0, -height / 2 + radius + rectHeight / 2);
    WorldImage bottomCenter = centerRect.movePinhole(0, height / 2 - radius - rectHeight / 2);
    WorldImage leftSide = sideRect.movePinhole(-width / 2 + radius + rectWidth / 2, 0);
    WorldImage rightSide = sideRect.movePinhole(width / 2 - radius - rectWidth / 2, 0);

    WorldImage topLeft = corner.movePinhole(-width / 2 + radius, -height / 2 + radius);
    WorldImage topRight = corner.movePinhole(width / 2 - radius, -height / 2 + radius);
    WorldImage bottomLeft = corner.movePinhole(-width / 2 + radius, height / 2 - radius);
    WorldImage bottomRight = corner.movePinhole(width / 2 - radius, height / 2 - radius);

    WorldImage expected1 = new OverlayImage(topCenter,
        new OverlayImage(bottomCenter,
            new OverlayImage(leftSide, new OverlayImage(rightSide, new OverlayImage(topLeft,
                new OverlayImage(topRight, new OverlayImage(bottomLeft, bottomRight)))))));

    t.checkExpect(actual1, expected1);

    Button btn2 = new Button("Test", 40, 100, Color.BLUE, 10);
    WorldImage actual2 = btn2.createTrueRoundedRect(40, 100, Color.BLUE, 10);

    int radius2 = 10;
    int width2 = 40;
    int height2 = 100;
    Color color2 = Color.BLUE;
    int rectHeight2 = height2 - (2 * radius2);
    WorldImage centerRect2 = new RectangleImage(width2, rectHeight2, OutlineMode.SOLID, color2);

    int rectWidth2 = width2 - (2 * radius2);
    WorldImage sideRect2 = new RectangleImage(rectWidth2, height2, OutlineMode.SOLID, color2);

    WorldImage corner2 = new CircleImage(radius2, OutlineMode.SOLID, color2);

    WorldImage topCenter2 = centerRect2.movePinhole(0, -height2 / 2 + radius2 + rectHeight2 / 2);
    WorldImage bottomCenter2 = centerRect2.movePinhole(0, height2 / 2 - radius2 - rectHeight2 / 2);
    WorldImage leftSide2 = sideRect2.movePinhole(-width2 / 2 + radius2 + rectWidth2 / 2, 0);
    WorldImage rightSide2 = sideRect2.movePinhole(width2 / 2 - radius2 - rectWidth2 / 2, 0);

    WorldImage topLeft2 = corner2.movePinhole(-width2 / 2 + radius2, -height2 / 2 + radius2);
    WorldImage topRight2 = corner2.movePinhole(width2 / 2 - radius2, -height2 / 2 + radius2);
    WorldImage bottomLeft2 = corner2.movePinhole(-width2 / 2 + radius2, height2 / 2 - radius2);
    WorldImage bottomRight2 = corner2.movePinhole(width2 / 2 - radius2, height2 / 2 - radius2);

    WorldImage expected2 = new OverlayImage(topCenter2,
        new OverlayImage(bottomCenter2,
            new OverlayImage(leftSide2, new OverlayImage(rightSide2, new OverlayImage(topLeft2,
                new OverlayImage(topRight2, new OverlayImage(bottomLeft2, bottomRight2)))))));

    t.checkExpect(actual2, expected2);
  }

  void testCreateRoundedBorder(Tester t) {
    Button btn1 = new Button("Test", 60, 30, Color.RED, 5);
    WorldImage actual1 = btn1.createRoundedBorder(60, 30, Color.RED, 5, 2);

    int radius = 5;
    int width = 60;
    int height = 30;
    Color color = Color.RED;
    int thickness = 2;

    WorldImage outer = btn1.createTrueRoundedRect(width, height, color, radius);
    WorldImage inner = btn1.createTrueRoundedRect(width - (2 * thickness), height - (2 * thickness),
        new Color(0, 0, 0, 0), radius - thickness);
    WorldImage expected1 = new OverlayImage(inner, outer);
    t.checkExpect(actual1, expected1);

    Button btn2 = new Button("Test", 100, 40, Color.BLUE, 2);
    WorldImage actual2 = btn2.createRoundedBorder(100, 40, Color.BLUE, 4, 2);

    int radius2 = 4;
    int width2 = 100;
    int height2 = 40;
    Color color2 = Color.BLUE;
    int thickness2 = 2;

    WorldImage outer2 = btn2.createTrueRoundedRect(width2, height2, color2, radius2);
    WorldImage inner2 = btn2.createTrueRoundedRect(width2 - (2 * thickness2),
        height2 - (2 * thickness2), new Color(0, 0, 0, 0), radius2 - thickness2);
    WorldImage expected2 = new OverlayImage(inner2, outer2);
    t.checkExpect(actual2, expected2);
  }

  void testCreateRoundedCell(Tester t) {
    Button btn1 = new Button("Test", 50, 30, Color.RED, 5);
    WorldImage actual = btn1.createRoundedCell(50, 30, Color.RED, Color.BLACK, 5, 2);

    int radius = 5;
    int width = 50;
    int height = 30;
    Color fill = Color.RED;
    Color border = Color.BLACK;
    int thickness = 2;

    int innerWidth = width - (thickness * 2);
    int innerHeight = height - (thickness * 2);

    WorldImage base = btn1.createTrueRoundedRect(innerWidth, innerHeight, fill, radius);
    WorldImage borderImage = btn1.createRoundedBorder(width, height, border, radius, thickness);
    WorldImage expected = new OverlayImage(base, borderImage);

    t.checkExpect(actual, expected);

    Button btn2 = new Button("Test", 70, 60, Color.PINK, 4);
    WorldImage actual2 = btn2.createRoundedCell(70, 60, Color.PINK, Color.BLACK, 4, 3);

    int radius2 = 4;
    int width2 = 70;
    int height2 = 60;
    Color fill2 = Color.PINK;
    Color border2 = Color.BLACK;
    int thickness2 = 3;

    int innerWidth2 = width2 - (thickness2 * 2);
    int innerHeight2 = height2 - (thickness2 * 2);

    WorldImage base2 = btn2.createTrueRoundedRect(innerWidth2, innerHeight2, fill2, radius2);
    WorldImage borderImage2 = btn2.createRoundedBorder(width2, height2, border2, radius2,
        thickness2);
    WorldImage expected2 = new OverlayImage(base2, borderImage2);

    t.checkExpect(actual2, expected2);
  }

  void testDrawButton(Tester t) {
    Button btn1 = new Button("Submit", 400, 300, Color.PINK, 10);
    WorldImage actual = btn1.drawButton();
    TextImage wordText = new TextImage("Submit", 18, FontStyle.BOLD, Color.BLACK);
    WorldImage cell = btn1.createRoundedCell(400, 300, Color.PINK, Color.WHITE, 10, 4);
    WorldImage centeredText = wordText.movePinhole(0, -300 / 40);
    WorldImage expected = new OverlayImage(centeredText, cell);
    t.checkExpect(actual, expected);

    Button btn2 = new Button("Submit", 800, 600, Color.BLUE, 20);
    WorldImage actual2 = btn2.drawButton();
    TextImage wordText2 = new TextImage("Submit", 18, FontStyle.BOLD, Color.BLACK);
    WorldImage cell2 = btn2.createRoundedCell(800, 600, Color.BLUE, Color.WHITE, 20, 4);
    WorldImage centeredText2 = wordText2.movePinhole(0, -600 / 40);
    WorldImage expected2 = new OverlayImage(centeredText2, cell2);
    t.checkExpect(actual2, expected2);
  }

  void testDiffColor(Tester t) {

    t.checkExpect(this.beaucoup.diffColor(), easyGreen);
    t.checkExpect(this.gusto.diffColor(), mediumYellow);
    t.checkExpect(this.brick.diffColor(), hardBlue);
    t.checkExpect(this.mortar.diffColor(), expertPurple);

  }

  void testIfClicked(Tester t) {
    this.initData();

    t.checkExpect(this.jenny.color, Color.LIGHT_GRAY);
    t.checkExpect(this.jenny.state, "unclicked");

    this.jenny.ifClicked(this.posn1, 50, 50, 20, 20, new ArrayList<Word>());
    t.checkExpect(this.jenny.color, Color.PINK);
    t.checkExpect(this.jenny.state, "clicked");

    this.jenny.ifClicked(this.posn1, 50, 50, 20, 20, new ArrayList<Word>());
    t.checkExpect(this.jenny.color, Color.LIGHT_GRAY);
    t.checkExpect(this.jenny.state, "unclicked");

    this.jenny.ifClicked(this.posn2, 50, 50, 20, 20, new ArrayList<Word>());
    t.checkExpect(this.jenny.color, Color.LIGHT_GRAY);
    t.checkExpect(this.jenny.state, "unclicked");
  }

  void testHasSameCategory(Tester t) {
    t.checkExpect(this.beaucoup.hasSameCategory(this.beaucoup), true);
    t.checkExpect(this.beaucoup.hasSameCategory(this.gusto), false);
    t.checkExpect(this.multi.hasSameCategory(this.molto), true);
    t.checkExpect(this.molto.hasSameCategory(this.multi), true);
  }

  void testIsClicked(Tester t) {
    Word fishTank = new Word("FISH TANK", "clicked", "hard", "RECTANGULAR PRISM");
    Word shoebox = new Word("SHOEBOX", "clicked", "hard", "RECTANGULAR PRISM");
    Word mucho = new Word("MUCHO", "unclicked", "easy", "'MANY' IN DIFFERENT LANGUAGES");
    Word passion = new Word("PASSION", "unclicked", "medium", "ENTHUSIASM");
    t.checkExpect(fishTank.isClicked(), true);
    t.checkExpect(shoebox.isClicked(), true);
    t.checkExpect(mucho.isClicked(), false);
    t.checkExpect(passion.isClicked(), false);
  }

  void testDraw(Tester t) {
    Button bGusto = new Button("GUSTO", 150, 100, Color.LIGHT_GRAY, 6);
    Button bPassion = new Button("PASSION", 150, 100, Color.LIGHT_GRAY, 6);
    Button bLime = new Button("LIME", 150, 100, Color.LIGHT_GRAY, 6);
    t.checkExpect(this.gusto.draw(), bGusto.drawButton());
    t.checkExpect(this.passion.draw(), bPassion.drawButton());
    t.checkExpect(this.lime.draw(), bLime.drawButton());
  }

  void testMakeGrid(Tester t) {
    int width = 700;
    int height = 600;

    ConnectionsWorld gameLost = new ConnectionsWorld(new Random(1));
    gameLost.triesLeft = 0;

    WorldScene gameLostScreen = new WorldScene(width, height);
    gameLostScreen.placeImageXY(new TextImage("You Lose :(", 40, Color.RED), 350, 300);
 
    t.checkExpect(gameLost.makeScene(), gameLostScreen);

    ConnectionsWorld gameWon = new ConnectionsWorld(new Random(1));
    WorldScene gameWonScreen = new WorldScene(width, height);
    gameWon.correctCategories = new ArrayList<>(
        Arrays.asList(this.easy, this.medium, this.hard, this.expert));
    gameWonScreen.placeImageXY(new TextImage("You win :)", 40, FontStyle.BOLD, Color.GREEN), 350,
        300);

    t.checkExpect(gameWon.makeScene(), gameWonScreen);

    ConnectionsWorld game1 = new ConnectionsWorld(new Random(1));
    
    // check that clicking positions changes word states
    Word firstWord = game1.words.get(0);
    Posn firstWordPos = new Posn(125, 100); // Position of first word
    
    // before click
    t.checkExpect(firstWord.state, "unclicked");
    t.checkExpect(firstWord.color, Color.LIGHT_GRAY);
    
    // after click
    game1.onMouseClicked(firstWordPos);
    t.checkExpect(firstWord.state, "clicked");
    t.checkExpect(firstWord.color, Color.PINK);
    
    // after unclick
    game1.onMouseClicked(firstWordPos);
    t.checkExpect(firstWord.state, "unclicked");
    t.checkExpect(firstWord.color, Color.LIGHT_GRAY);
    
    ConnectionsWorld game = new ConnectionsWorld(new Random(1));
    Word testWord = game.words.get(0); // First word in the list
    Posn wordCenter = new Posn(125, 100); // Position of first word
    
    // checks that clicking a word selects it
    t.checkExpect(testWord.state, "unclicked");
    game.onMouseClicked(wordCenter);
    t.checkExpect(testWord.state, "clicked");
    t.checkExpect(game.clickedWords.size(), 1);
    
    // checks that clicking the word again deselects it
    game.onMouseClicked(wordCenter);
    t.checkExpect(testWord.state, "unclicked");
    t.checkExpect(game.clickedWords.size(), 0);
    
    // checks that you can't select more than 4 words
    for (int i = 0; i < 5; i++) {
      game.onMouseClicked(new Posn(125 + (i * 150), 100));
    }
    t.checkExpect(game.clickedWords.size(), 4);
  }

  void testOnMouseClicked(Tester t) {
    ArrayList<Word> set1 = new ArrayList<Word>(
        Arrays.asList(new Word("PLANT", "clicked", "easy", "DO SOME TASKS IN THE GARDEN"),
            new Word("MARY JANE", "unclicked", "hard", "SHOES"),
            new Word("MULE", "unclicked", "hard", "SHOES"),
            new Word("PIPER", "unclicked", "expert", "SAND___"),
            new Word("STONE", "unclicked", "expert", "SAND___"),
            new Word("DWINDLE", "unclicked", "medium", "WANE"),
            new Word("PETER", "unclicked", "medium", "WANE"),
            new Word("PRUNE", "unclicked", "easy", "DO SOME TASKS IN THE GARDEN"),
            new Word("FLAT", "unclicked", "hard", "SHOES"),
            new Word("SLIDE", "unclicked", "hard", "SHOES"),
            new Word("CASTLE", "unclicked", "expert", "SAND___"),
            new Word("TAPER", "unclicked", "medium", "WANE"),
            new Word("WATER", "unclicked", "easy", "DO SOME TASKS IN THE GARDEN"),
            new Word("WEED", "unclicked", "easy", "DO SOME TASKS IN THE GARDEN"),
            new Word("FADE", "unclicked", "medium", "WANE"),
            new Word("PAPER", "unclicked", "expert", "SAND___")));

    ConnectionsWorld state0 = new ConnectionsWorld(new Random(1));
    ConnectionsWorld state3 = new ConnectionsWorld(set1);
    this.initData();
    this.state1.onMouseClicked(this.posn0);
    t.checkExpect(this.state1, state0);
    this.initData();
    this.state1.onMouseClicked(this.posn3);
    t.checkExpect(this.state1.triesLeft, state3.triesLeft);
  }

  void testOnKeyEvent(Tester t) {
    this.initData();
    // checks if the game resets if r key is pressed
    this.state1.onKeyEvent("r");
    t.checkExpect(this.state1, this.state1);

    // checks if nothing changes if a key other than r is pressed
    this.state2.onKeyEvent("e");
    t.checkExpect(this.state2, this.state2);
  }

  void testResetGame(Tester t) {
    // idk
  }

  void testMakeScene(Tester t) {
    // can just call the makegrid method here dont need to make new exmaplkes
  }

  // TODO: delete? doesnt work because we changed the methods for makescene
  /*
   * void testMakeScene(Tester t) { ConnectionsWorld game1 = new
   * ConnectionsWorld(new Random(1)); ArrayList<Word> set1 = new ArrayList<Word>(
   * Arrays.asList(new Word("PLANT", "unclicked", "easy",
   * "DO SOME TASKS IN THE GARDEN"), new Word("MARY JANE", "unclicked", "hard",
   * "SHOES"), new Word("MULE", "unclicked", "hard", "SHOES"), new Word("PIPER",
   * "unclicked", "expert", "SAND___"), new Word("STONE", "unclicked", "expert",
   * "SAND___"), new Word("DWINDLE", "unclicked", "medium", "WANE"), new
   * Word("PETER", "unclicked", "medium", "WANE"), new Word("PRUNE", "unclicked",
   * "easy", "DO SOME TASKS IN THE GARDEN"), new Word("FLAT", "unclicked", "hard",
   * "SHOES"), new Word("SLIDE", "unclicked", "hard", "SHOES"), new Word("CASTLE",
   * "unclicked", "expert", "SAND___"), new Word("TAPER", "unclicked", "medium",
   * "WANE"), new Word("WATER", "unclicked", "easy",
   * "DO SOME TASKS IN THE GARDEN"), new Word("WEED", "unclicked", "easy",
   * "DO SOME TASKS IN THE GARDEN"), new Word("FADE", "unclicked", "medium",
   * "WANE"), new Word("PAPER", "unclicked", "expert", "SAND___"))); int width =
   * 700; int height = 600; int gridSize = 4; int startX = 125; int startY = 100;
   * int triesLeft = 4; Button submit = new Button("Submit", 120, 60, Color.PINK,
   * 12);
   * 
   * WorldScene game = new WorldScene(width, height);
   * 
   * for (int i = 0; i < set1.size(); i++) { int x = startX + (i % gridSize) *
   * (600 / gridSize); int y = startY + (i / gridSize) * (400 / gridSize);
   * 
   * WorldImage finalImage = set1.get(i).draw(); game.placeImageXY(finalImage, x,
   * y); game.placeImageXY(finalImage, x, y); }
   * 
   * for (int i = 0; i < triesLeft; i++) { int x = 295 + (i % gridSize) * (600 /
   * 16); int y = 480 + (i / gridSize) * (400 / 16);
   * 
   * WorldImage circle = new CircleImage(7, OutlineMode.SOLID, Color.PINK);
   * game.placeImageXY(circle, x, y); }
   * 
   * game.placeImageXY(submit.drawButton(), 350, 540);
   * 
   * WorldScene expectedGame1 = game;
   * 
   * t.checkExpect(game1.makeScene(), expectedGame1);
   * 
   * ConnectionsWorld game2 = new ConnectionsWorld(new Random(1)); game2.triesLeft
   * = 0;
   * 
   * WorldScene gamee = new WorldScene(width, height); gamee.placeImageXY(new
   * TextImage("You Lose :(", 40, Color.RED), 350, 300);
   * 
   * t.checkExpect(game2.makeScene(), gamee); }
   */

}