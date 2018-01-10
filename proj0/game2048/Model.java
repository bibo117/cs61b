package game2048;

import java.util.Arrays;
import java.util.Formatter;
import java.util.Observable;


/** The state of a game of 2048.
 *  @author Bo Bi
 */
class Model extends Observable {

    /* Coordinate System: column C, row R of the board (where row 0,
     * column 0 is the lower-left corner of the board) will correspond
     * to _board[c][r].  Be careful! This is not the usual 2D matrix
     * numbering, where rows are numbered from the top, and the row
     * number is the *first* index. Rather it works like (x, y) coordinates.
     */

    /** Largest piece value. */
    static final int MAX_PIECE = 2048;

    /** A new 2048 game on a board of size SIZE with no pieces
     *  and score 0. */
    Model(int size) {
        _board = new Tile[size][size];
        _score = _maxScore = 0;
        _gameOver = false;
    }

    /** Return the current Tile at (COL, ROW), where 0 <= ROW < size(),
     *  0 <= COL < size(). Returns null if there is no tile there. */
    Tile tile(int col, int row) {
        return _board[col][row];
    }

    /** Return the number of squares on one side of the board. */
    int size() {
        return _board.length;
    }

    /** Return true iff the game is over (there are no moves, or
     *  there is a tile with value 2048 on the board). */
    boolean gameOver() {
        return _gameOver;
    }

    /** Return the current score. */
    int score() {
        return _score;
    }

    /** Return the current maximum game score (updated at end of game). */
    int maxScore() {
        return _maxScore;
    }

    /** Clear the board to empty and reset the score. */
    void clear() {
        _score = 0;
        _gameOver = false;
        for (Tile[] column : _board) {
            Arrays.fill(column, null);
        }
        setChanged();
    }

    /** Add TILE to the board.  There must be no Tile currently at the
     *  same position. */
    void addTile(Tile tile) {
        assert _board[tile.col()][tile.row()] == null;
        _board[tile.col()][tile.row()] = tile;
        checkGameOver();
        setChanged();
    }

    /** Tilt the board toward SIDE. Return true iff this changes the board.
     *  @param col Column
     *  @param  row Row
     *  @return true if board is changed
     */

    boolean inBounds(int col, int row) {
        return (0 <= col && col < Main.BOARD_SIZE
                && 0 <= row && row < Main.BOARD_SIZE);
    }

    /** Removes gaps from column.
     * @param col Column
     * @param row Row
     * @param side Side
     * @return if null blocks are removed
     * */
    boolean tiltRemoveNull(int col, int row, Side side) {
        int nextCol = col + side.originaldCol();
        int nextRow = row + side.originaldRow();
        boolean changed = false;

        while (inBounds(nextCol, nextRow)) {
            Tile currTile = _board[col][row];
            Tile nextTile = _board[nextCol][nextRow];

            if (currTile == null && nextTile != null) {
                _board[col][row] = nextTile.move(col, row);
                _board[nextCol][nextRow] = null;
                changed = true;
            }

            col = nextCol;
            row = nextRow;
            nextCol = col + side.originaldCol();
            nextRow = row + side.originaldRow();
        }


        return changed;
    }
    /** Merges tiles during a tilt move.
     * @param col Column
     * @param row Row
     * @param side Side
     * @return true if merge occurred
     * */
    boolean tiltMerge(int col, int row, Side side) {
        int nextCol = col + side.originaldCol();
        int nextRow = row + side.originaldRow();
        boolean changed = false;

        while (inBounds(nextCol, nextRow)) {
            Tile currTile = _board[col][row];
            Tile nextTile = _board[nextCol][nextRow];

            if (currTile != null
                    && nextTile != null
                    && currTile.value() == nextTile.value()) {
                _board[col][row] = currTile.merge(col, row, nextTile);
                _score += _board[col][row].value();
                _board[nextCol][nextRow] = null;
                changed = true;

                tiltRemoveNull(nextCol, nextRow, side);
            }

            col = nextCol;
            row = nextRow;
            nextCol = col + side.originaldCol();
            nextRow = row + side.originaldRow();
        }
        return changed;
    }


    /** Tilts board towards side.
     * @param side Side tilting towards
     * @return returns if board is changed
     * */
    boolean tilt(Side side) {
        boolean changed;
        changed = false;

        if (side == Side.NORTH) {
            side = Side.SOUTH;
        } else if (side == Side.SOUTH) {
            side = Side.NORTH;
        } else if (side == Side.WEST) {
            side = Side.EAST;
        } else if (side == Side.EAST) {
            side = Side.WEST;
        }

        int originalCol = side.originalCol() * (Main.BOARD_SIZE - 1);
        int originalRow = side.originalRow() * (Main.BOARD_SIZE - 1);

        for (int k = 0; k < Main.BOARD_SIZE; k++) {

            while (tiltRemoveNull(originalCol, originalRow, side)) {
                changed = true;
            }

            if (tiltMerge(originalCol, originalRow, side)) {
                changed = true;
            }


            if (side.originaldCol() == 0) {
                originalCol += side.originaldRow();
            } else {
                originalRow -= side.originaldCol();
            }


        }

        checkGameOver();
        if (changed) {
            setChanged();
        }
        return changed;
    }

    /** Return the current Tile at (COL, ROW), when sitting with the board
     *  oriented so that SIDE is at the top (farthest) from you. */
    private Tile vtile(int col, int row, Side side) {
        return _board[side.col(col, row, size())][side.row(col, row, size())];
    }

    /** Move TILE to (COL, ROW), merging with any tile already there,
     *  where (COL, ROW) is as seen when sitting with the board oriented
     *  so that SIDE is at the top (farthest) from you. */
    private void setVtile(int col, int row, Side side, Tile tile) {
        int pcol = side.col(col, row, size()),
                prow = side.row(col, row, size());
        if (tile.col() == pcol && tile.row() == prow) {
            return;
        }
        Tile tile1 = vtile(col, row, side);
        _board[tile.col()][tile.row()] = null;

        if (tile1 == null) {
            _board[pcol][prow] = tile.move(pcol, prow);
        } else {
            _board[pcol][prow] = tile.merge(pcol, prow, tile1);
        }
    }

    /** Deternmine whether game is over and update _gameOver and _maxScore
     *  accordingly. */
    private void checkGameOver() {

        Boolean empty = false;
        Boolean adjacent = false;
        for (int k = 0; k < Main.BOARD_SIZE; k++) {
            for (int i = 0; i < Main.BOARD_SIZE; i++) {

                if ((tile(k, i) != null)
                        && (tile(k, i).value() == MAX_PIECE)) {
                    _gameOver = true;
                    if (score() > maxScore()) {
                        _maxScore = score();
                    }

                }
                if (tile(k, i) == null) {
                    empty = true;
                } else if (k + 1 < Main.BOARD_SIZE && tile(k + 1, i) != null
                        && tile(k, i).value() == tile(k + 1, i).value()
                        || i + 1 < Main.BOARD_SIZE && tile(k, i + 1) != null
                        && tile(k, i).value() == tile(k, i + 1).value()) {
                    adjacent = true;
                }

            }
        }


        if (!_gameOver && !empty && !adjacent) {
            _gameOver = true;
            if (score() > maxScore()) {
                _maxScore = score();
            }

        }


    }

    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("[%n");
        for (int row = size() - 1; row >= 0; row -= 1) {
            for (int col = 0; col < size(); col += 1) {
                if (tile(col, row) == null) {
                    out.format("|    ");
                } else {
                    out.format("|%4d", tile(col, row).value());
                }
            }
            out.format("|%n");
        }
        out.format("] %d (max: %d)", score(), maxScore());
        return out.toString();
    }

    /** Current contents of the board. */
    private Tile[][] _board;
    /** Current score. */
    private int _score;
    /** Maximum score so far.  Updated when game ends. */
    private int _maxScore;
    /** True iff game is ended. */
    private boolean _gameOver;

}
