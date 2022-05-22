package jump61;

import java.util.ArrayList;
import java.util.Random;

import static jump61.Side.*;

/** An automated Player.
 *  @author P. N. Hilfinger
 */
class AI extends Player {

    /** A new player of GAME initially COLOR that chooses moves automatically.
     *  SEED provides a random-number seed used for choosing moves.
     */
    AI(Game game, Side color, long seed) {
        super(game, color);
        _random = new Random(seed);
    }

    @Override
    String getMove() {
        Board board = getGame().getBoard();
        assert getSide() == board.whoseMove();
        int choice = searchForMove();
        getGame().reportMove(board.row(choice), board.col(choice));
        return String.format("%d %d", board.row(choice), board.col(choice));
    }

    /** Return a move after searching the game tree to DEPTH>0 moves
     *  from the current position. Assumes the game is not over. */
    private int searchForMove() {
        Board work = new Board(getBoard());
        assert getSide() == work.whoseMove();
        _foundMove = -1;
        int depth = 4;
        int alpha = -Integer.MAX_VALUE, beta = Integer.MAX_VALUE;
        if (getSide() == RED) {
            minMax(work, depth, true, 1, alpha, beta);
        } else if (getSide() == BLUE) {
            minMax(work, depth, true, -1, alpha, beta);
        }
        return _foundMove;
    }

    /** Return an ArrayList of ValidMoves.
     * @param b which position
     * @param player for which player. */
    private ArrayList<Integer> getValidMoves(Board b, Side player) {
        ArrayList<Integer> listMoves = new ArrayList<>();
        for (int i = 0; i < b.size() * b.size(); i += 1) {
            if (b.isLegal(player, i)) {
                listMoves.add(i);
            }
        }
        return listMoves;
    }

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove.
     *  @return: int */
    private int minMax(Board board, int depth, boolean saveMove,
                       int sense, int alpha, int beta) {
        if (depth == 0) {
            return staticEval(board, board.size() * board.size());
        }
        if (board.getWinner() == board.whoseMove()) {
            return Integer.MAX_VALUE;
        } else if (board.getWinner() == board.whoseMove().opposite()) {
            return -Integer.MAX_VALUE;
        }
        if (sense == 1) {
            int maxEval = -Integer.MAX_VALUE;
            ArrayList<Integer> listMoves = getValidMoves(board,
                    board.whoseMove());
            for (Integer move : listMoves) {
                Board oldBoard = new Board(board);
                board.addSpot(board.whoseMove(), move);
                int eval = minMax(board, depth - 1,
                        false, -1, alpha, beta);
                if (maxEval <= eval) {
                    maxEval = eval;
                    if (saveMove) {
                        _foundMove = move;
                    }
                }
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) {
                    break;
                }
                board = oldBoard;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            ArrayList<Integer> listMoves = getValidMoves(board,
                    board.whoseMove());
            for (Integer move: listMoves) {
                Board oldBoard = new Board(board);
                board.addSpot(board.whoseMove(), move);
                int eval = minMax(board, depth - 1,
                        false, 1, alpha, beta);
                if (minEval >= eval) {
                    minEval = eval;
                    if (saveMove) {
                        _foundMove = move;
                    }
                }
                beta = Math.min(beta, eval);
                if (beta <= alpha) {
                    break;
                }
                board = oldBoard;
            }
            return minEval;
        }
    }

    /** Return a heuristic estimate of the value of board position B.
     *  Use WINNINGVALUE to indicate a win for Red and -WINNINGVALUE to
     *  indicate a win for Blue. */
    private int staticEval(Board b, int winningValue) {
        return winningValue - b.numOfSide(b.whoseMove());
    }

    /** A random-number generator used for move selection. */
    private Random _random;

    /** Used to convey moves discovered by minMax. */
    private int _foundMove;

}
