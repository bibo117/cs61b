package db61b;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static db61b.Utils.error;

/**
 * A single table in a database.
 *
 * @author Bo Bi
 */
class Table {

    /**
     * A new Table whose columns are given by COLUMNTITLES, which may
     * not contain duplicate names.
     */
    Table(String[] columnTitles) {
        if (columnTitles.length == 0) {
            throw error("table must have at least one column");
        }
        _size = 0;
        _rowSize = columnTitles.length;

        for (int i = columnTitles.length - 1; i >= 1; i -= 1) {
            for (int j = i - 1; j >= 0; j -= 1) {
                if (columnTitles[i].equals(columnTitles[j])) {
                    throw error("duplicate column name: %s",
                            columnTitles[i]);
                }
            }
        }

        _titles = columnTitles;
        _columns = new ValueList[_titles.length];
        for (int i = 0; i < columns(); i++) {
            _columns[i] = new ValueList();
        }
    }

    /**
     * A new Table whose columns are give by COLUMNTITLES.
     */
    Table(List<String> columnTitles) {
        this(columnTitles.toArray(new String[columnTitles.size()]));
    }

    /**
     * Return the number of columns in this table.
     */
    public int columns() {
        return _columns.length;
    }

    /**
     * Return the title of the Kth column.  Requires 0 <= K < columns().
     */
    public String getTitle(int k) {
        return _titles[k];
    }

    /**
     * Return the number of the column whose title is TITLE, or -1 if
     * there isn't one.
     */
    public int findColumn(String title) {
        int count = 0;
        int number = 0;
        for (int i = 0; i < columns(); i++) {
            if (this.getTitle(i).equals(title)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Return the number of rows in this table.
     */
    public int size() {
        return _size;
    }

    /**
     * Return the value of column number COL (0 <= COL < columns())
     * of record number ROW (0 <= ROW < size()).
     */
    public String get(int row, int col) {
        try {
            return _columns[col].get(row);
        } catch (IndexOutOfBoundsException excp) {
            throw error("invalid row or column");
        }
    }

    /**
     * Returns the index based on lexographic position when comparing
     * VALUES.
     */
    public int lexographic(String[] values) {
        int result = 0;
        if (this.size() < 1) {
            result = 0;
        }
        loop:
        for (int i = 0; i < this.size(); i++) {
            if (values[0].compareTo(this.get(i, 0)) < 0) {
                result = i;
                break loop;
            } else if (values[0].compareTo(this.get(i, 0)) == 0) {
                int j = 1;
                while (j < this.columns()) {
                    int l = i;
                    while (l < this.size()) {
                        if (values[j].compareTo(this.get(l, j)) < 0) {
                            result = l;
                            break loop;
                        } else if (values[j].compareTo(this.get(i, j)) > 0) {
                            if (l == this.size() - 1
                                    || values[0].compareTo(this.get(l + 1, 0))
                                    != 0) {
                                result = l + 1;
                                break loop;
                            } else {
                                l++;
                            }
                        } else {
                            j++;
                        }
                    }
                }
            } else {
                result = this.size();
            }
        }
        return result;
    }

    /**
     * Add a new row whose column values are VALUES to me if no equal
     * row already exists.  Return true if anything w  as added,
     * false otherwise.
     */
    public boolean add(String[] values) {
        if (values.length != this.columns()) {
            throw error("Not Same Length");
        }
        int count = 0;
        String[] one = new String[this.columns()];
        if (this.size() == 0) {
            for (int i = 0; i < this.columns(); i++) {
                _columns[i].add(values[i]);
            }
            _size += 1;
            return true;
        } else {
            for (int i = 0; i < this.size(); i++) {
                for (int j = 0; j < this.columns(); j++) {
                    one[j] = this.get(i, j);
                    if (j == (this.columns() - 1)) {
                        if (!Arrays.equals(one, values)) {
                            count += 1;
                        }
                    }
                }
            }
        }
        if (count == this.size()) {
            int index = lexographic(values);
            for (int i = 0; i < this.columns(); i++) {
                _columns[i].add(index, values[i]);
            }
            _size += 1;
            return true;
        } else {
            return false;
        }
    }


    /**
     * Add a new row whose column values are extracted by COLUMNS from
     * the rows indexed by ROWS, if no equal row already exists.
     * Return true if anything was added, false otherwise. See
     * Column.getFrom(Integer...) for a description of how Columns
     * extract values.
     */
    public boolean add(List<Column> columns, Integer... rows) {
        /* Create ArrayList to store each column of the table as an array */
        String[] one = new String[columns()];
        for (int i = 0; i < columns.size(); i++) {
            one[i] = columns.get(i).getFrom(rows);
        }
        return add(one);
    }


    /**
     * Read the contents of the file NAME.db, and return as a Table.
     * Format errors in the .db file cause a DBException.
     */
    static Table readTable(String name) {
        BufferedReader input;
        Table table;
        input = null;
        try {
            input = new BufferedReader(new FileReader(name + ".db"));
            String header = input.readLine();
            if (header == null) {
                throw error("missing header in DB file");
            }
            String[] columnNames = header.split(",");
            table = new Table(columnNames);
            String container = input.readLine();
            while (container != null) {
                String[] array = container.split(",");
                table.add(array);
                container = input.readLine();
            }
        } catch (FileNotFoundException e) {
            throw error("could not find %s.db", name);
        } catch (IOException e) {
            throw error("problem reading from %s.db", name);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    /* Ignore IOException */
                }
            }
        }
        return table;
    }

    /**
     * Write the contents of TABLE into the file NAME.db. Any I/O errors
     * cause a DBException.
     */
    void writeTable(String name) {
        PrintStream output;
        output = null;
        try {
            String sep;
            sep = "";
            output = new PrintStream(name + ".db");
            for (int i = 0; i < this.columns(); i++) {
                if (i == this.columns() - 1) {
                    output.print(this.getTitle(i));
                } else {
                    output.print(this.getTitle(i) + ",");
                }
            }
            output.println();
            for (int i = 0; i < this.size(); i++) {
                for (int j = 0; j < this.columns(); j++) {
                    if (j == this.columns() - 1) {
                        output.print(this.get(i, j));
                    } else {
                        output.print(this.get(i, j) + ",");
                    }
                }
                output.println();
            }
        } catch (IOException e) {
            throw error("trouble writing to %s.db", name);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /**
     * Print my contents on the standard output, separated by spaces
     * and indented by two spaces.
     */
    void print() {
        String[] container = new String[columns()];
        for (int i = 0; i < this.size(); i++) {
            for (int j = 0; j < this.columns(); j++) {
                container[j] = this.get(i, j).trim();
            }
            StringBuilder create = new StringBuilder();
            for (String value : container) {
                create.append(" " + value);
            }
            String text = create.toString();
            System.out.println(" " + text);
        }
    }

    /**Filters and Returns specific columnNames that are similar to
     * COLUMNNAMES. **/
    public List<String> columnnamefilter(List<String> columnNames) {
        List<String> newstring = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            for (int j = 0; j < this.columns(); j++) {
                if (this.getTitle(j).equals(columnNames.get(i))) {
                    newstring.add(columnNames.get(i));
                }
            }
        }
        return newstring;
    }
    /** Returns specific rows based on COLUMNNAMES, CONDITIONS.**/
    public List<Integer> conditions(List<String> columnNames,
                                    List<Condition> conditions) {
        List<Integer> rows = new ArrayList<Integer>();
        for (int l = 0; l < conditions.size(); l++) {
            if (conditions.get(l).check(columnNames)) {
                for (int j = 0; j < conditions.size(); j++) {
                    for (int q = 0; q < size(); q++) {
                        if (conditions.get(j).test(q)) {
                            rows.add(q);
                        }
                    }
                }
            }
        }
        return rows;
    }

    /**
     * Return a new Table whose columns are COLUMNNAMES, selected from
     * rows of this table that satisfy CONDITIONS.
     */
    Table select(List<String> columnNames, List<Condition> conditions) {
        Table result = new Table(columnNames);
        List<Integer> index = new ArrayList<Integer>();
        List<String> names = listcreator(this);
        List<String> final1 = new ArrayList<>();
        if (names.containsAll(columnNames)) {
            String[] values = new String[columnNames.size()];
            for (int i = 0; i < columnNames.size(); i++) {
                index.add(findColumn(columnNames.get(i)));
            }
            for (int l = 0; l < this.size(); l++) {
                if (Condition.test(conditions, l)) {
                    for (int k = 0; k < index.size(); k++) {
                        for (int m = 0; m < this.columns(); m++) {
                            if (m == index.get(k)) {
                                values[k] = this.get(l, m);
                            }

                        }
                    }
                    result.add(values);
                }
            }
        } else {
            int count = 0;
            for (int i = 0; i < columnNames.size(); i++) {
                for (int j = 0; j < names.size(); j++) {
                    if (columnNames.get(i).equals(names.get(i))) {
                        count++;
                    }
                }
                if (count == 0) {
                    final1.add(columnNames.get(i));
                }
            }
            String res = String.join("", final1);
            throw error("Error: unknown column:" + " " + res);
        }
        return result;
    }

    /**Returns columns based on TABLE and NAMES based on if equijoin is true.**/
    public ArrayList<Column> columngenerator(Table table, List<String> names) {
        ArrayList<Column> columns = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            columns.add(new Column(names.get(i), table));
        }
        return columns;
    }

    /**Returns a table RESULT built by multiple paramaters such as
     * STRING, ONE, TABLE, TABLEROW, THISCOL, and COLUMNNAMES. **/
    public void builder(String[] string, Table one, Table table, Table result,
                        int tablerow, int thiscol, List<String> columnNames) {
        int i = 0;
        while (i < columnNames.size()) {
            if (one.findColumn(columnNames.get(i)) != -1) {
                for (int j = 0; j < one.columns(); j++) {
                    if (one.getTitle(j).equals(columnNames.get(i))) {
                        string[i] = one.get(tablerow, j);
                    }
                }
                i++;
            } else if (table.findColumn(columnNames.get(i)) != -1) {
                for (int j = 0; j < table.columns(); j++) {
                    if (table.getTitle(j).equals(columnNames.get(i))) {
                        string[i] = table.get(thiscol, j);
                    }
                }
                i++;
            }
        }
        result.add(string);
    }
    /**
     * Return a new Table whose columns are COLUMNNAMES, selected
     * from pairs of rows from this table and from TABLE2 that match
     * on all columns with identical names and satisfy CONDITIONS.
     */
    Table select2(Table table2,
                  List<String> columnNames, List<Condition> conditions) {
        Table result = new Table(columnNames);
        List<String> titlesfromthis =
                listcreator(this);
        List<String> titlesfromtable =
                listcreator(table2);
        List<String> combined = new ArrayList<>(titlesfromthis);
        combined.retainAll(titlesfromtable);
        String[] container = new String[columnNames.size()];
        ArrayList<Column> columnsfromthis = columngenerator(this, combined);
        ArrayList<Column> columnsfromtable = columngenerator(table2, combined);
        for (int i = 0; i < this.size(); i++) {
            for (int j = 0; j < table2.size(); j++) {
                if ((equijoin(columnsfromthis, columnsfromtable, i, j))
                        && (Condition.test(conditions, i, j))) {
                    builder(container, this,
                            table2, result, i, j, columnNames);
                }
            }
        }
        return result;
    }


        /*if (!compareTitles(table2)){
            resulting = outerjoin(table2,columnNames, conditions);
        } else {
            List<String> names = new ArrayList<String>(columnNames);
            if (!containsname(columnNames)) {
                names.add(0, getTitle(0));
            }
            Table filtered1 = select(names, conditions);
            Table filtered2 = table2.select(names, conditions);
            List<String> checker_table1 = new ArrayList<String>();
            List<String> checker_table2 = new ArrayList<String>();
            if (filtered1.size() > 0) {
                for (int i = 0; i < filtered1.size(); i++) {
                    checker_table1.add(filtered1.get(i, 0));
                }
            }
            if (filtered2.size() > 0) {
                for (int i = 0; i < filtered2.size(); i++) {
                    checker_table2.add(filtered2.get(i, 0));
                }
            }
            List<String> common = new ArrayList<String>(checker_table1);
            common.retainAll(checker_table1);
            if (filtered1.size() == 0) {
                Table new_filtered = filter(names, checker_table2);
                resulting = addcolumns(filtered2, new_filtered, columnNames);
                resulting.print();
            } else if (filtered2.size() == 0) {
                Table newtable = table2.filter(names, checker_table1);
                resulting = addcolumns(newtable, filtered1, columnNames);
            } else {
                Table filteredone = filter(names, common);
                Table filteredtwo = table2.filter(names, common);
                resulting = addcolumns(filteredone, filteredtwo, columnNames);
                */

    /** Returns a list of columnnames based on the table ONE given.**/
    public List<String> listcreator(Table one) {
        List<String> checker = new ArrayList<String>();
        for (int i = 0; i < one.columns(); i++) {
            checker.add(one.getTitle(i));
        }
        return checker;
    }


    /**
     * Return <0, 0, or >0 depending on whether the row formed from
     * the elements _columns[0].get(K0), _columns[1].get(K0), ...
     * is less than, equal to, or greater than that formed from elememts
     * _columns[0].get(K1), _columns[1].get(K1), ....  This method ignores
     * the _index.
     */
    private int compareRows(int k0, int k1) {
        for (int i = 0; i < _columns.length; i += 1) {
            int c = _columns[i].get(k0).compareTo(_columns[i].get(k1));
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    /**
     * Return true if the columns COMMON1 from ROW1 and COMMON2 from
     * ROW2 all have identical values.  Assumes that COMMON1 and
     * COMMON2 have the same number of elements and the same names,
     * that the columns in COMMON1 apply to this table, those in
     * COMMON2 to another, and that ROW1 and ROW2 are indices, respectively,
     * into those tables.
     */
    public static boolean equijoin(List<Column> common1, List<Column> common2,
                                   int row1, int row2) {
        for (int i = 0; i < common1.size(); i++) {
            if (!common1.get(i).getFrom(row1).
                    equals(common2.get(i).getFrom(row2))) {
                return false;
            }
        }
        return true;
    }

    /**
     * A class that is essentially ArrayList<String>.  For technical reasons,
     * we need to encapsulate ArrayList<String> like this because the
     * underlying design of Java does not properly distinguish between
     * different kinds of ArrayList at runtime (e.g., if you have a
     * variable of type Object that was created from an ArrayList, there is
     * no way to determine in general whether it is an ArrayList<String>,
     * ArrayList<Integer>, or ArrayList<Object>).  This leads to annoying
     * compiler warnings.  The trick of defining a new type avoids this
     * issue.
     */
    private static class ValueList extends ArrayList<String> {
    }

    /**
     * My column titles.
     */
    private final String[] _titles;
    /**
     * My columns. Row i consists of _columns[k].get(i) for all k.
     */
    private final ValueList[] _columns;

    /**
     * Rows in the database are supposed to be sorted. To do so, we
     * have a list whose kth element is the index in each column
     * of the value of that column for the kth row in lexicographic order.
     * That is, the first row (smallest in lexicographic order)
     * is at position _index.get(0) in _columns[0], _columns[1], ...
     * and the kth row in lexicographic order in at position _index.get(k).
     * When a new row is inserted, insert its index at the appropriate
     * place in this list.
     * (Alternatively, we could simply keep each column in the proper order
     * so that we would not need _index.  But that would mean that inserting
     * a new row would require rearranging _rowSize lists (each list in
     * _columns) rather than just one.
     */
    private final ArrayList<Integer> _index = new ArrayList<>();

    /**
     * My number of rows (redundant, but convenient).
     */
    private int _size;
    /**
     * My number of columns (redundant, but convenient).
     */
    private final int _rowSize;
}

