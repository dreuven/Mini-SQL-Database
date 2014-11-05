package db61b;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import static db61b.Utils.*;

/** A single table in a database.
 *  @author P. N. Hilfinger
 */
class Table implements Iterable<Row> {
    /** A new Table whose columns are given by COLUMNTITLES, which may
     *  not contain dupliace names. */
    Table(String[] columnTitles) {
        if (columnTitles.length == 0) {
            System.out.println("You Need Have AT LEAST 1 Column Name Amigo");
        }
        for (int i = columnTitles.length - 1; i >= 1; i -= 1) {
            for (int j = i - 1; j >= 0; j -= 1) {
                if (columnTitles[i].equals(columnTitles[j])) {
                    throw error("duplicate column name: %s",
                                columnTitles[i]);
                }
            }
        }

        _columnTitles = columnTitles;
    }

    /** A new Table whose columns are give by COLUMNTITLES. */
    Table(List<String> columnTitles) {
        this(columnTitles.toArray(new String[columnTitles.size()]));
    }
    /** Returns the column titles of this table. */
    public String[] thisTablesColumnTitles() {
        return _columnTitles;
    }
    /** Return the number of columns in this table. */
    public int columns() {
        return _columnTitles.length;
    }

    /** Return the title of the Kth column.  Requires 0 <= K < columns(). */
    public String getTitle(int k) {
        return _columnTitles[k];
    }

    /** Return the number of the column whose title is TITLE, or -1 if
     *  there isn't one. */
    public int findColumn(String title) {
        int i = 0;
        while (i < columns()) {
            if (getTitle(i).equals(title)) {
                return i;
            }
            i += 1;
        }
        return -1;
    }

    /** Return the number of Rows in this table. */
    public int size() {
        return _rows.size();
    }

    /** Returns an iterator that returns my rows in an unspecfied order. */
    @Override
    public Iterator<Row> iterator() {
        return _rows.iterator();
    }

    /** Add ROW to THIS if no equal row already exists.  Return true if anything
     *  was added, false otherwise. */
    public boolean add(Row row) {
        if (row.size() != columns()) {
            throw error("Error: Incorrect row size.");
        } else if (_rows.contains(row)) {
            return false;
        } else {
            _rows.add(row);
            return true;
        }
    }

    /** Read the contents of the file NAME.db, and return as a Table.
     *  Format errors in the .db file cause a DBException. */
    static Table readTable(String name) {
        BufferedReader input;
        Table table;
        input = null;
        table = null;
        try {
            input = new BufferedReader(new FileReader(name + ".db"));
            String header = input.readLine();
            if (header == null) {
                throw error("missing header in DB file");
            }
            String[] columnNames = header.split(",");
            table = new Table(columnNames);
            String rowinfo = input.readLine();
            while (rowinfo != null) {
                table.add(new Row(rowinfo.split(",")));
                rowinfo = input.readLine();
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

    /** Write the contents of TABLE into the file NAME.db. Any I/O errors
     *  cause a DBException. */
    void writeTable(String name) {
        PrintStream output;
        output = null;
        try {
            String sep;
            sep = "";
            output = new PrintStream(name + ".db");
            String header = "";
            String[] head = _columnTitles;
            for (int i = 0; i < head.length; i++) {
                if (i == head.length - 1) {
                    header += head[i];
                } else {
                    header += head[i] + ",";
                }
            }
            output.append(header);
            Iterator<Row> myiter = iterator();
            while (myiter.hasNext()) {
                output.println();
                Row row = myiter.next();
                sep = "";
                for (int i = 0; i < row.size(); i++) {
                    if (i == row.size() - 1) {
                        sep += row.get(i);
                    } else {
                        sep += row.get(i) + ",";
                    }
                }
                output.append(sep);
            }
        } catch (IOException e) {
            throw error("trouble writing to %s.db", name);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /** Print my contents on the standard output. */
    void print() {
        Iterator<Row> it = iterator();
        while (it.hasNext()) {
            System.out.println(it.next().toString());
        }
    }

    /** Return a new Table whose columns are COLUMNNAMES, selected from
     *  rows of this table that satisfy CONDITIONS. */
    Table select(List<String> columnNames, List<Condition> conditions) {
        Table result = new Table(columnNames);
        int[] indicesOfImportance = new int[result.columns()];
        for (int i = 0; i < columnNames.size(); i++) {
            indicesOfImportance[i] = findColumn(result.getTitle(i));
        }
        Iterator<Row> iter = iterator();
        while (iter.hasNext()) {
            boolean bool = true;
            String[] newrow = new String[columnNames.size()];
            Row rowOfInterest = iter.next();
            int i = 0;
            while (i < result.columns()) {
                newrow[i] = rowOfInterest.get(indicesOfImportance[i]);
                i += 1;
            }
            if (conditions != null) {
                for (Condition cond : conditions) {
                    if (!cond.test(rowOfInterest)) {
                        bool = false;
                        break;
                    }
                }
            }
            if (bool) {
                result.add(new Row(newrow));
            }
        }
        return result;
    }

    /** Return a new Table whose columns are COLUMNNAMES, selected
     *  from pairs of rows from this table and from TABLE2 that match
     *  on all columns with identical names and satisfy CONDITIONS. */
    Table select(Table table2, List<String> columnNames,
                 List<Condition> conditions) {
        Table result = new Table(columnNames);
        ArrayList<Column> selectedColumns = new ArrayList<Column>();
        for (int i = 0; i < columnNames.size(); i++) {
            Column col = new Column(columnNames.get(i), this, table2);
            selectedColumns.add(col);
        }
        ArrayList<String> inCommonTitles = new ArrayList<String>();
        for (String columnNombre: table2.thisTablesColumnTitles()) {
            for (String columnShem : _columnTitles) {
                if (columnShem.equals(columnNombre)) {
                    inCommonTitles.add(columnNombre);
                }
            }
        }
        ArrayList<Column> inCommCol1 = new ArrayList<Column>();
        ArrayList<Column> inCommCol2 = new ArrayList<Column>();
        Iterator<String> iteratorOfColTitles = inCommonTitles.iterator();
        while (iteratorOfColTitles.hasNext()) {
            String nameOfCol = iteratorOfColTitles.next();
            Column col1 = new Column(nameOfCol, this);
            Column col2 = new Column(nameOfCol, table2);
            inCommCol1.add(col1);
            inCommCol2.add(col2);
        }
        Iterator<Row> myTableIter = iterator();
        while (myTableIter.hasNext()) {
            Row curRowTable = myTableIter.next();
            Iterator<Row> tableTwoIter = table2.iterator();
            while (tableTwoIter.hasNext()) {
                Row tab2Row = tableTwoIter.next();
                if (equijoin(inCommCol1, inCommCol2, curRowTable, tab2Row)) {
                    Row newrow = new Row(selectedColumns, curRowTable, tab2Row);
                    if (conditions != null) {
                        if (Condition.test(conditions, curRowTable, tab2Row)) {
                            result.add(newrow);
                        }
                    } else {
                        result.add(newrow);
                    }
                }

            }
        }
        return result;
    }

    /** Return true if the columns COMMON1 from ROW1 and COMMON2 from
     *  ROW2 all have identical values.  Assumes that COMMON1 and
     *  COMMON2 have the same number of elements and the same names,
     *  that the columns in COMMON1 apply to this table, those in
     *  COMMON2 to another, and that ROW1 and ROW2 come, respectively,
     *  from those tables. */
    private static boolean equijoin(List<Column> common1, List<Column> common2,
                                    Row row1, Row row2) {
        Boolean bool = true;
        for (int i = 0; i < common2.size(); i++) {
            String string1 = common1.get(i).getFrom(row1);
            String string2 = common2.get(i).getFrom(row2);
            if (string1.compareTo(string2) != 0) {
                bool = false;
                break;
            }
        }
        return bool;
    }

    /** My rows. */
    private HashSet<Row> _rows = new HashSet<>();
    /** My columTitles. */
    private String[] _columnTitles;
}
