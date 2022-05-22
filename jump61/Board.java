package jump61;

import java.util.ArrayDeque;
import java.util.Formatter;

import java.util.function.Consumer;

import static jump61.Side.*;
import static jump61.Square.square;

/** Represents the state of a Jump61 game.  Squares are indexed either by
 *  row and column (between 1 and size()), or by square number, numbering
 *  squares by rows, with squares in row 1 numbered from 0 to size()-1, in
 *  row 2 numbered from size() to 2*size() - 1, etc. (i.e., row-major order).
 *
 *  A Board may be given a notifier---a Consumer<Board> whose
 *  .accept method is called whenever the Board's contents are changed.
 *
 *  @author Duc Nguyen
 */
class Board {

    /** An uninitialized Board.  Only for use by subtypes. */
    protected Board() {
        _notifier = NOP;
    }

    /** An N x N board in initial configuration. */
    Board(int N) {
        if (N <= 1) {
            throw new Error("Board error! N <= 1");
        } else if (N > Defaults.MAX_BOARD_SIZE) {
            throw new Error("Board error! N > MAX");
        }
        _boardArray = new Square[N][N];
        for (int r = 0; r < _boardArray.length; r += 1) {
            for (int c = 0; c < _boardArray.length; c += 1) {
                _boardArray[r][c] = Square.INITIAL;
            }
        }
        _notifier = (board) -> {
            System.out.println("ANNOUNCE CHANGES");
        };
        Board tmp = new Board(this);
        _history.add(tmp);
        _numMoves = 0;
        _readonlyBoard = new ConstantBoard(this);
    }

    /** A board whose initial contents are copied from BOARD0, but whose
     *  undo history is clear, and whose notifier does nothing. */
    Board(Board board0) {
        this._boardArray = new Square[board0.size()][board0.size()];
        for (int r = 0; r < board0.size(); r += 1) {
            for (int c = 0; c < board0.size(); c += 1) {
                this._boardArray[r][c] = board0.get(r + 1, c + 1);
            }
        }
        this._notifier = NOP;
        this._history = new ArrayDeque<>();
        this._numMoves = 0;
        _readonlyBoard = new ConstantBoard(this);
    }

    /** Returns a readonly version of this board. */
    Board readonlyBoard() {
        return _readonlyBoard;
    }

    /** (Re)initialize me to a cleared board with N squares on a side. Clears
     *  the undo history and sets the number of moves to 0. */
    void clear(int N) {
        if (N <= 1) {
            throw new Error("Board error! N <= 1");
        } else if (N > Defaults.MAX_BOARD_SIZE) {
            throw new Error("Board error! N > MAX");
        }
        _boardArray = new Square[N][N];
        for (int r = 0; r < _boardArray.length; r += 1) {
            for (int c = 0; c < _boardArray.length; c += 1) {
                _boardArray[r][c] = Square.INITIAL;
            }
        }
        _history.clear();
        _numMoves = 0;
        _readonlyBoard = new ConstantBoard(this);
    }

    /** Copy the contents of BOARD into me. */
    void copy(Board board) {
        this._boardArray = new Square[board.size()][board.size()];
        for (int r = 0; r < board.size(); r += 1) {
            for (int c = 0; c < board.size(); c += 1) {
                this._boardArray[r][c] = board.get(r + 1, c + 1);
            }
        }
        this._history = new ArrayDeque<>(board._history);
        this._numMoves = board._numMoves;
        this._readonlyBoard = new ConstantBoard(this);
    }

    /** Copy the contents of BOARD into me, without modifying my undo
     *  history. Assumes BOARD and I have the same size. */
    private void internalCopy(Board board) {
        assert size() == board.size();
        for (int r = 0; r < board.size(); r += 1) {
            for (int c = 0; c < board.size(); c += 1) {
                this._boardArray[r][c] = board.get(r + 1, c + 1);
            }
        }
    }

    /** Return the number of rows and of columns of THIS. */
    int size() {
        return _boardArray.length;
    }

    /** Returns the contents of the square at row R, column C
     *  1 <= R, C <= size (). */
    Square get(int r, int c) {
        return get(sqNum(r, c));
    }

    /** Returns the contents of square #N, numbering squares by rows, with
     *  squares in row 1 number 0 - size()-1, in row 2 numbered
     *  size() - 2*size() - 1, etc. */
    Square get(int n) {
        if (!exists(n)) {
            throw new Error("get error! n");
        }
        int r = n / size();
        int c = n % size();
        return _boardArray[r][c];
    }

    /** Returns the total number of spots on the board. */
    int numPieces() {
        int count = 0;
        for (int r = 0; r < _boardArray.length; r += 1) {
            for (int c = 0; c < _boardArray.length; c += 1) {
                count += _boardArray[r][c].getSpots();
            }
        }
        return count;
    }

    /** Returns the Side of the player who would be next to move.  If the
     *  game is won, this will return the loser (assuming legal position). */
    Side whoseMove() {
        return ((numPieces() + size()) & 1) == 0 ? RED : BLUE;
    }

    /** Return true iff row R and column C denotes a valid square. */
    final boolean exists(int r, int c) {
        return 1 <= r && r <= size() && 1 <= c && c <= size();
    }

    /** Return true iff S is a valid square number. */
    final boolean exists(int s) {
        int N = size();
        return 0 <= s && s < N * N;
    }

    /** Return the row number for square #N. */
    final int row(int n) {
        return n / size() + 1;
    }

    /** Return the column number for square #N. */
    final int col(int n) {
        return n % size() + 1;
    }

    /** Return the square number of row R, column C. */
    final int sqNum(int r, int c) {
        return (c - 1) + (r - 1) * size();
    }

    /** Return a string denoting move (ROW, COL)N. */
    String moveString(int row, int col) {
        return String.format("%d %d", row, col);
    }

    /** Return a string denoting move N. */
    String moveString(int n) {
        return String.format("%d %d", row(n), col(n));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
        to square at row R, column C. */
    boolean isLegal(Side player, int r, int c) {
        return isLegal(player, sqNum(r, c));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
     *  to square #N. */
    boolean isLegal(Side player, int n) {
        return (isLegal(player) && (get(n).getSide() != player.opposite()));
    }

    /** Returns true iff PLAYER is allowed to move at this point. */
    boolean isLegal(Side player) {
        return whoseMove() == player;
    }

    /** Returns the winner of the current position, if the game is over,
     *  and otherwise null. */
    final Side getWinner() {
        int numReds = numOfSide(RED);
        int numBlues = numOfSide(BLUE);
        if (numReds >= size() * size() && numBlues == 0) {
            return RED;
        } else if (numBlues >= size() * size() && numReds == 0) {
            return BLUE;
        }
        return null;
    }

    /** Return the number of squares of given SIDE. */
    int numOfSide(Side side) {
        int count = 0;
        for (int r = 0; r < _boardArray.length; r += 1) {
            for (int c = 0; c < _boardArray.length; c += 1) {
                Side tmp = _boardArray[r][c].getSide();
                if (tmp == side) {
                    count += 1;
                }
            }
        }
        return count;
    }

    /** Add a spot from PLAYER at row R, column C.  Assumes
     *  isLegal(PLAYER, R, C). */
    void addSpot(Side player, int r, int c) {
        if (get(r, c).getSpots() + 1 <= neighbors(r, c)) {
            _boardArray[r - 1][c - 1] = square(player,
                    get(r, c).getSpots() + 1);
            if (_workQueue.isEmpty()) {
                markUndo();
            }
        } else {
            jump(sqNum(r, c));
        }
    }

    /** Add a spot from PLAYER at square #N.  Assumes isLegal(PLAYER, N). */
    void addSpot(Side player, int n) {
        int r = row(n);
        int c = col(n);
        addSpot(player, r, c);
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white). */
    void set(int r, int c, int num, Side player) {
        internalSet(r, c, num, player);
        announce();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white).  Does not announce
     *  changes. */
    private void internalSet(int r, int c, int num, Side player) {
        internalSet(sqNum(r, c), num, player);
    }

    /** Set the square #N to NUM spots (0 <= NUM), and give it color PLAYER
     *  if NUM > 0 (otherwise, white). Does not announce changes. */
    private void internalSet(int n, int num, Side player) {
        if (!exists(n)) {
            throw new Error("internalSet error! invalid n");
        }
        int r = row(n);
        int c = col(n);
        if (num > 0) {
            _boardArray[r - 1][c - 1] = square(player, num);
        } else if (num == 0) {
            _boardArray[r - 1][c - 1] = square(WHITE, num);
        }
    }

    /** Undo the effects of one move (that is, one addSpot command).  One
     *  can only undo back to the last point at which the undo history
     *  was cleared, or the construction of this Board. */
    void undo() {
        if (!_history.isEmpty() && _history.size() > 1) {
            _history.removeLast();
            Board tmp = _history.getLast();
            tmp._history = this._history;
            this.copy(tmp);
        }
    }

    /** Record the beginning of a move in the undo history. */
    private void markUndo() {
        Board tmp = new Board();
        tmp.copy(this);
        _history.add(tmp);
    }

    /** Add DELTASPOTS spots of side PLAYER to row R, column C,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int r, int c, int deltaSpots) {
        internalSet(r, c, deltaSpots + get(r, c).getSpots(), player);
    }

    /** Add DELTASPOTS spots of color PLAYER to square #N,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int n, int deltaSpots) {
        internalSet(n, deltaSpots + get(n).getSpots(), player);
    }

    /** Used in jump to keep track of squares needing processing.  Allocated
     *  here to cut down on allocations. */
    private final ArrayDeque<Integer> _workQueue = new ArrayDeque<>();

    /** Do all jumping on this board, assuming that initially, S is the only
     *  square that might be over-full. */
    private void jump(int S) {
        if (!exists(S)) {
            throw new Error("jump error! invalid n");
        }
        int r = row(S);
        int c = col(S);
        Side current = _boardArray[r - 1][c - 1].getSide();
        if (get(S).getSpots() + 1 > neighbors(S)) {
            if (exists(r, c - 1)) {
                set(r, c - 1, get(r, c - 1).getSpots(), current);
                _workQueue.add(S - 1);
            }
            if (exists(r, c + 1)) {
                set(r, c + 1, get(r, c + 1).getSpots(), current);
                _workQueue.add(S + 1);
            }
            if (exists(r - 1, c)) {
                set(r - 1, c, get(r - 1, c).getSpots(), current);
                _workQueue.add(S - size());
            }
            if (exists(r + 1, c)) {
                set(r + 1, c, get(r + 1, c).getSpots(), current);
                _workQueue.add(S + size());
            }
        }
        if (getWinner() != current) {
            _boardArray[r - 1][c - 1] = square(current, 1);
            while (!_workQueue.isEmpty()) {
                addSpot(current, _workQueue.removeLast());
            }
        } else {
            _workQueue.clear();
        }
    }

    /** Returns my dumped representation. */
    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("%1$-3s%n", "===");
        for (int r = 0; r < size(); r += 1) {
            String tmp = "";
            for (int c = 0; c < size(); c += 1) {
                Square curr = get(r + 1, c + 1);
                if (curr.getSide() == RED) {
                    tmp += curr.getSpots() + "r ";
                } else if (curr.getSide() == BLUE) {
                    tmp += curr.getSpots() + "b ";
                } else {
                    tmp += curr.getSpots() + "- ";
                }
            }
            tmp = tmp.trim();
            out.format("    %1$-4s%n", tmp);
        }
        out.format("%1$-3s", "===");
        return out.toString();
    }

    /** Returns an external rendition of me, suitable for human-readable
     *  textual display, with row and column numbers.  This is distinct
     *  from the dumped representation (returned by toString). */
    public String toDisplayString() {
        String[] lines = toString().trim().split("\\R");
        Formatter out = new Formatter();
        for (int i = 1; i + 1 < lines.length; i += 1) {
            out.format("%2d %s%n", i, lines[i].trim());
        }
        out.format("  ");
        for (int i = 1; i <= size(); i += 1) {
            out.format("%3d", i);
        }
        return out.toString();
    }

    /** Returns the number of neighbors of the square at row R, column C. */
    int neighbors(int r, int c) {
        int size = size();
        int n;
        n = 0;
        if (r > 1) {
            n += 1;
        }
        if (c > 1) {
            n += 1;
        }
        if (r < size) {
            n += 1;
        }
        if (c < size) {
            n += 1;
        }
        return n;
    }

    /** Returns the number of neighbors of square #N. */
    int neighbors(int n) {
        return neighbors(row(n), col(n));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        } else {
            if (this.size() != ((Board) obj).size()) {
                return false;
            }
            Board B = (Board) obj;
            for (int r = 0; r < _boardArray.length; r += 1) {
                for (int c = 0; c < _boardArray.length; c += 1) {
                    if (_boardArray[r][c] != B._boardArray[r][c]) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    @Override
    public int hashCode() {
        return numPieces();
    }

    /** Set my notifier to NOTIFY. */
    public void setNotifier(Consumer<Board> notify) {
        _notifier = notify;
        announce();
    }

    /** Take any action that has been set for a change in my state. */
    private void announce() {
        _notifier.accept(this);
    }

    /** A notifier that does nothing. */
    private static final Consumer<Board> NOP = (s) -> { };

    /** A read-only version of this Board. */
    private ConstantBoard _readonlyBoard;

    /** Use _notifier.accept(B) to announce changes to this board. */
    private Consumer<Board> _notifier;

    /** The board itself. */
    private Square[][] _boardArray;

    /** Undo history. */
    private ArrayDeque<Board> _history = new ArrayDeque<>();

    /** Number of moves. */
    private int _numMoves;

}
