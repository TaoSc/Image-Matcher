package projetprog.models;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class TableResult {

    private SimpleIntegerProperty rank;
    private SimpleStringProperty fileName;
    private SimpleDoubleProperty distance;

    public TableResult(int rank, String fileName, double distance) {
        this.rank = new SimpleIntegerProperty(rank);
        this.fileName = new SimpleStringProperty(fileName);
        this.distance = new SimpleDoubleProperty(distance);
    }

    //<editor-fold desc="Fonctions utilisées par JavaFX">
    public int getRank() {
        return rank.get();
    }

    public void setRank(int rank) {
        this.rank.set(rank);
    }

    public String getFileName() {
        return fileName.get();
    }

    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }

    public double getDistance() {
        return distance.get();
    }

    public void setDistance(double distance) {
        this.distance.set(distance);
    }
    //</editor-fold>

}
