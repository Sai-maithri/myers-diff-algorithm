import java.util.*;
import java.io.*;
import java.nio.file.*;

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
                // Part B will be implemented later.
                printLines(edits);
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
        List<int[]> history = new ArrayList<>();
        for (int d = 0; d <= max; d++) {
            history.add(v.clone());
            for (int k = -d; k <= d; k += 2) {
                int index = offset + k;
                int x;
                if (k == -d || (k != d && v[index - 1] < v[index + 1])) {
                    x = v[index + 1];
                } else {
                    x = v[index - 1] + 1;
                }
                int y = x - k;
                while (x < n && y < m && Arrays.equals(a.get(x), b.get(y))) {
                    x++;
                    y++;
                }
                v[index] = x;
                if (x >= n && y >= m) {
                    return buildEdits(a, b, history, d, offset);
                }
            }
        }
        return new ArrayList<>();
    }
    public static List<Edit> buildEdits(List<byte[]> a, List<byte[]> b, List<int[]> history, int d, int offset) {
        List<Edit> reversed = new ArrayList<>();
        int x = a.size();
        int y = b.size();
        for (int currentD = d; currentD > 0; currentD--) {
            int[] previousV = history.get(currentD);
            int k = x - y;
            int previousK;
            if (k == -currentD || (k != currentD && previousV[offset + k - 1] < previousV[offset + k + 1])) {
                previousK = k + 1;
            } else {
                previousK = k - 1;
            }
            int previousX = previousV[offset + previousK];
            int previousY = previousX - previousK;
            // Move backwards through the matching snake.
            while (x > previousX && y > previousY) {
                x--;
                y--;
                reversed.add(new Edit(' ', a.get(x)));
            }
            // Work out whether the previous edit was
            // an insertion or a deletion.
            if (x == previousX) {
                y--;
                reversed.add(new Edit('+', b.get(y)));
            } else {
                x--;
                reversed.add(new Edit('-', a.get(x)));
            }
        }
        // Any remaining matching lines at the beginning.
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
}
