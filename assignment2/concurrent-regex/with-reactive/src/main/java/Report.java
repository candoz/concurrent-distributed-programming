import java.nio.file.Path;

public class Report {

    final private Path filePath;
    final private int matches;

    public Report(Path filePath, int matches) {
        this.filePath = filePath;
        this.matches = matches;
    }

    public Path getFilePath() {
        return filePath;
    }

    public int getMatches() {
        return matches;
    }

    @Override
    public boolean equals(Object that) {
        if(this == that) return true;
        if(!(that instanceof Report)) return false;
        Report thatReport = (Report) that;
        return this.getFilePath().equals(thatReport.getFilePath()) && this.getMatches() == thatReport.getMatches();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.filePath != null ? this.filePath.hashCode() : 0);
        hash = 53 * hash + this.matches;
        return hash;
    }

    @Override
    public String toString() {
        return "["+matches+"] "+ filePath;
    }

}
