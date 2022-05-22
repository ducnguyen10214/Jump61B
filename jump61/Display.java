package jump61;

import ucb.gui2.TopLevel;
import ucb.gui2.LayoutSpec;

import java.io.FileReader;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

/** The GUI controller for jump61.  To require minimal change to textual
 *  interface, we adopt the strategy of converting GUI input (mouse clicks)
 *  into textual commands that are sent to the Game object through a
 *  a Writer.  The Game object need never know where its input is coming from.
 *  A Display is an Observer of Games and Boards so that it is notified when
 *  either changes.
 *  @author Duc Nguyen
 */
class Display extends TopLevel implements View, CommandSource, Reporter {

    /** A new window with given TITLE displaying GAME, and using COMMANDWRITER
     *  to send commands to the current game. */
    Display(String title) {
        super(title, true);
        addMenuButton("Game->New Game", this::newGame);
        addMenuButton("Game->Quit", this::quit);
        addMenuCheckBox("Option->Red Manual", true, this::setManualRed);
        addMenuCheckBox("Option->Red AI", false, this::setAIRed);
        addMenuCheckBox("Option->Blue Manual", false, this::setManualBlue);
        addMenuCheckBox("Option->Blue AI", true, this::setAIBlue);
        addMenuButton("Option->Resize", this::setBoardSize);
        _boardWidget = new BoardWidget(_commandQueue);
        add(_boardWidget, new LayoutSpec("height", 100, "width", 100));
        display(true);
    }

    /** Process INPUT. */
    void takeArguments(String[] input) {
        if (input.length != 1 && input.length != 2) {
            throw new Error("Display error! Wrong arguments");
        } else if (input.length == 1) {
            _commandQueue.add("size 6");
        } else {
            try {
                FileReader file = new FileReader(input[1]);
                Scanner sc = new Scanner(file);
                while (sc.hasNextLine()) {
                    String tmp = sc.nextLine();
                    _commandQueue.add(tmp);
                }
            } catch (FileNotFoundException excp) {
                System.err.print("Could not open the file!");
                System.exit(1);
            }
        }
    }

    /** Response to "Quit" button click. */
    void quit(String dummy) {
        System.exit(0);
    }

    /** Response to "New Game" button click. */
    void newGame(String dummy) {
        setManualRed(dummy);
        setAIBlue(dummy);
        _commandQueue.offer("new");
    }

    /** Reset to a new 2 x 2 game. */
    void setBoardSize(String dummy) {
        String size = getTextInput("Enter the desired board size (2-10): ",
                "Resize", "", "");
        setManualRed(dummy);
        setAIBlue(dummy);
        _commandQueue.offer("size " + size);
    }

    /** Change Red to manual mode. */
    void setManualRed(String dummy) {
        select("Option->Red AI", false);
        select("Option->Red Manual", true);
        _commandQueue.offer("manual red");
    }

    /** Change Red to AI mode. */
    void setAIRed(String dummy) {
        select("Option->Red Manual", false);
        select("Option->Red AI", true);
        _commandQueue.offer("auto red");
    }

    /** Change Blue to manual mode. */
    void setManualBlue(String dummy) {
        select("Option->Blue AI", false);
        select("Option->Blue Manual", true);
        _commandQueue.offer("manual blue");
    }

    /** Change Blue to AI mode. */
    void setAIBlue(String dummy) {
        select("Option->Blue Manual", false);
        select("Option->Blue AI", true);
        _commandQueue.offer("auto blue");
    }

    @Override
    public void update(Board board) {
        _boardWidget.update(board);
        _boardWidget.setSize(_boardWidget.getPreferredSize());
        pack();
        _boardWidget.repaint();
    }

    @Override
    public String getCommand(String ignored) {
        try {
            return _commandQueue.take();
        } catch (InterruptedException excp) {
            throw new Error("unexpected interrupt");
        }
    }

    @Override
    public void announceWin(Side side) {
        showMessage(String.format("%s wins!", side.toCapitalizedString()),
                    "Game Over", "information");
    }

    @Override
    public void announceMove(int row, int col) {
    }

    @Override
    public void msg(String format, Object... args) {
        showMessage(String.format(format, args), "", "information");
    }

    @Override
    public void err(String format, Object... args) {
        showMessage(String.format(format, args), "Error", "error");
    }

    /** Time interval in msec to wait after a board update. */
    static final long BOARD_UPDATE_INTERVAL = 50;

    /** The widget that displays the actual playing board. */
    private BoardWidget _boardWidget;

    /** Queue for commands going to the controlling Game. */
    private final ArrayBlockingQueue<String> _commandQueue =
        new ArrayBlockingQueue<>(100);
}
