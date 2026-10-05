import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        boolean known = args.length == 3 && (args[0].equals("lines") || args[0].equals("highlight"));
        if (!known) {
            System.err.println("usage: Main lines|highlight A_PATH B_PATH");
            System.exit(2);
        }
        String command = args[0];
        String aPath = args[1];
        String bPath = args[2];
        // TODO: read both files as raw bytes (brief, Section 2), then print the listing.
        try {
            byte[] fileA = Files.readAllBytes(Path.of(aPath));
            byte[] fileB = Files.readAllBytes(Path.of(bPath));
            List<byte[]> linesA = splitLines(fileA);
            List<byte[]> linesB = splitLines(fileB);
            List<Edit> edits = myersDiff(linesA, linesB);
            if (command.equals("lines")) {
                printLines(edits);
            } else {
                // printLines(edits);
                printHighlight(edits);
            }
        } catch (IOException e) {
            System.err.println("error: cannot read file");
            System.exit(2);
        }
    }

    public static class Edit {
        char type; 
        byte[] line;
        Edit(char type, byte[] line) {
            this.type = type;
            this.line = line;
        }
    }
    public static List<byte[]> splitLines(byte[] data) {
        List<byte[]> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == '\n') {
                lines.add(Arrays.copyOfRange(data, start, i));
                start = i + 1;
            }
        }
        if (start < data.length) {
            lines.add(Arrays.copyOfRange(data, start, data.length));
        }
        return lines;
    }
    public static List<Edit> myersDiff(List<byte[]> a, List<byte[]> b) {
        int n = a.size();
        int m = b.size();
        int max = n + m;
        int offset = max + 1;
        int[] v = new int[2 * max + 3];
        v[offset + 1] = 0;
        // Store only the active diagonals for each d.
        List<int[]> history = new ArrayList<>();
        for (int d = 0; d <= max; d++) {
            for (int k = -d; k <= d; k += 2) {
                int index = offset + k;
                int x;
                if (k == -d || (k != d && v[index - 1] < v[index + 1])) {
                    x = v[index + 1];
                } else {
                    x = v[index - 1] + 1;
                }
                int y = x - k;
                // Snake: move through matching lines.
                while (x < n && y < m && Arrays.equals(a.get(x), b.get(y))) {
                    x++;
                    y++;
                }
                v[index] = x;
                if (x >= n && y >= m) {
                    // Save only the active diagonals.
                    int[] snapshot = new int[2 * d + 1];
                    for (int snapshotK = -d; snapshotK <= d; snapshotK++) {
                        snapshot[snapshotK + d] = v[offset + snapshotK];
                    }
                    history.add(snapshot);
                    return buildEdits(a, b, history, d);
                }
            }
            // Save only the active diagonals for this d.
            int[] snapshot = new int[2 * d + 1];
            for (int snapshotK = -d; snapshotK <= d; snapshotK++) {
                snapshot[snapshotK + d] = v[offset + snapshotK];
            }
            history.add(snapshot);
        }
        return new ArrayList<>();
    }
    public static List<Edit> buildEdits(List<byte[]> a, List<byte[]> b, List<int[]> history, int d) {
        List<Edit> reversed = new ArrayList<>();
        int x = a.size();
        int y = b.size();
        for (int currentD = d; currentD > 0; currentD--) {
            // history contains the snapshot AFTER each d.
            // We need the snapshot from d - 1.
            int[] previousV = history.get(currentD - 1);
            int k = x - y;
            int previousK;
            if (k == -currentD || (k != currentD && previousV[(k - 1) + (currentD - 1)] < previousV[(k + 1) + (currentD - 1)])) {
                previousK = k + 1;
            } else {
                previousK = k - 1;
            }
            int previousX = previousV[previousK + (currentD - 1)];
            int previousY = previousX - previousK;
            // Move backwards through the matching snake.
            while (x > previousX && y > previousY) {
                x--;
                y--;
                reversed.add(new Edit(' ', a.get(x)));
            }
            // Previous edit was an insertion.
            if (x == previousX) {
                y--;
                reversed.add(new Edit('+', b.get(y)));
            } else {
                // Previous edit was a deletion.
                x--;
                reversed.add(new Edit('-', a.get(x)));
            }
        }
        // Remaining matching lines at the beginning.
        while (x > 0 && y > 0) {
            x--;
            y--;
            reversed.add(new Edit(' ', a.get(x)));
        }
        Collections.reverse(reversed);
        return reorderChanges(reversed);
    }
    public static List<Edit> reorderChanges(List<Edit> edits) {
        List<Edit> result = new ArrayList<>();
        int i = 0;
        while (i < edits.size()) {
            if (edits.get(i).type == ' ') {
                result.add(edits.get(i));
                i++;
                continue;
            }
            List<Edit> deletes = new ArrayList<>();
            List<Edit> inserts = new ArrayList<>();
            while (i < edits.size() && edits.get(i).type != ' ') {
                if (edits.get(i).type == '-') {
                    deletes.add(edits.get(i));
                } else {
                    inserts.add(edits.get(i));
                }
                i++;
            }
            result.addAll(deletes);
            result.addAll(inserts);
        }
        return result;
    }
    public static void printLines(List<Edit> edits) {
        for (Edit edit : edits) {
            System.out.write(edit.type);
            System.out.write(edit.line,0,edit.line.length);
            System.out.write('\n');
        }
    }

    // Part B
    public static List<int[]> characterOperations(int[] a, int[] b) {
        int n = a.length;
        int m = b.length;
        int max = n + m;
        int offset = max + 1;
        int[] v = new int[2 * max + 3];
        v[offset + 1] = 0;
        // Store only the active diagonals for each d.
        List<int[]> history = new ArrayList<>();
        for (int d = 0; d <= max; d++) {
            for (int k = -d; k <= d; k += 2) {
                int index = offset + k;
                int x;
                if (k == -d || (k != d && v[index - 1] < v[index + 1])) {
                    x = v[index + 1];
                } else {
                    x = v[index - 1] + 1;
                }
                int y = x - k;
                // Snake: move through matching code points.
                while (x < n && y < m && a[x] == b[y]) {
                    x++;
                    y++;
                }
                v[index] = x;
                if (x >= n && y >= m) {
                    int[] snapshot = new int[2 * d + 1];
                    for (int snapshotK = -d; snapshotK <= d; snapshotK++) {
                        snapshot[snapshotK + d] = v[offset + snapshotK];
                    }
                    history.add(snapshot);
                    return buildCharacterOperations(a, b, history,d);
                }
            }
            int[] snapshot = new int[2 * d + 1];
            for (int snapshotK = -d; snapshotK <= d; snapshotK++) {
                snapshot[snapshotK + d] = v[offset + snapshotK];
            }
            history.add(snapshot);
        }
        return new ArrayList<>();
    }

    public static List<int[]> buildCharacterOperations(int[] a, int[] b, List<int[]> history, int d) {
        List<int[]> reversed = new ArrayList<>();
        int x = a.length;
        int y = b.length;
        for (int currentD = d; currentD > 0; currentD--) {
            int[] previousV = history.get(currentD - 1);
            int k = x - y;
            int previousK;
            if (k == -currentD || (k != currentD && previousV[(k - 1) + (currentD - 1)] < previousV[(k + 1) + (currentD - 1)])) {
                previousK = k + 1;
            } else {
                previousK = k - 1;
            }
            int previousX = previousV[previousK + (currentD - 1)];
            int previousY = previousX - previousK;
            // Move backwards through matching code points.
            while (x > previousX && y > previousY) {
                x--;
                y--;
                reversed.add(new int[]{0});
            }
            // Insertion.
            if (x == previousX) {
                y--;
                reversed.add(new int[]{2});
            } else {
                // Deletion.
                x--;
                reversed.add(new int[]{1});
            }
        }
        // Remaining matching code points at the beginning.
        while (x > 0 && y > 0) {
            x--;
            y--;
            reversed.add(new int[]{0});
        }
        Collections.reverse(reversed);
        return reversed;
    }
    
    public static String characterDiff(String oldText, String newText) {
        int[] oldChars = oldText.codePoints().toArray();
        int[] newChars = newText.codePoints().toArray();
        List<int[]> operations = characterOperations(oldChars, newChars);
        List<Integer> oldChanged = new ArrayList<>();
        List<Integer> newChanged = new ArrayList<>();
        int oldPosition = 0;
        int newPosition = 0;
        for (int[] operation : operations) {
            int type = operation[0];
            if (type == 0) {
                // Character is unchanged.
                oldPosition++;
                newPosition++;
            } else if (type == 1) {
                // Character was deleted.
                oldChanged.add(oldPosition);
                oldPosition++;
            } else {
                // Character was inserted.
                newChanged.add(newPosition);
                newPosition++;
            }
        }
        return makeRanges(oldChanged)+ " | " + makeRanges(newChanged);
    }

    public static String makeRanges(List<Integer> positions) {
        if (positions.isEmpty()) {
            return ".";
        }
        StringBuilder result = new StringBuilder();
        int start = positions.get(0);
        int previous = start;
        for (int i = 1; i < positions.size(); i++) {
            int current = positions.get(i);
            if (current == previous + 1) {
                previous = current;
            } else {
                appendRange(result, start, previous + 1);
                start = current;
                previous = current;
            }
        }
        appendRange(result, start, previous + 1);
        return result.toString();
    }
    public static void appendRange(StringBuilder result, int start, int end) {
        if (result.length() > 0) {
            result.append(',');
        }
        result.append(start);
        result.append('-');
        result.append(end);
    }
    public static void printHighlight(List<Edit> edits) {
        int i = 0;
        while (i < edits.size()) {
            // Unchanged line.
            if (edits.get(i).type == ' ') {
                System.out.write(' ');
                System.out.write(edits.get(i).line, 0, edits.get(i).line.length);
                System.out.write('\n');
                i++;
                continue;
            }
            // Collect one complete change block.
            List<byte[]> deletes = new ArrayList<>();
            List<byte[]> inserts = new ArrayList<>();
            while (i < edits.size() && edits.get(i).type != ' ') {
                if (edits.get(i).type == '-') {
                    deletes.add(edits.get(i).line);
                } else {
                    inserts.add(edits.get(i).line);
                }
                i++;
            }
            // Print deletions first.
            for (byte[] line : deletes) {
                System.out.write('-');
                System.out.write(line, 0, line.length);
                System.out.write('\n');
            }
            // Print insertions.
            for (int j = 0; j < inserts.size(); j++) {
                byte[] newLine = inserts.get(j);
                System.out.write('+');
                System.out.write(newLine, 0, newLine.length);
                System.out.write('\n');
                // Pair first deletion with first insertion,
                // second deletion with second insertion, etc.
                if (j < deletes.size()) {
                    byte[] oldLine = deletes.get(j);
                    String oldText = new String(oldLine, StandardCharsets.UTF_8);
                    String newText = new String(newLine, StandardCharsets.UTF_8);
                    String ranges = characterDiff(oldText, newText);
                    byte[] question = ("? " + ranges + "\n").getBytes(StandardCharsets.UTF_8);
                    System.out.write(question, 0, question.length);
                }
            }
        }
    }
}
