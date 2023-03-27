package Modele;
//import Structures.SequenceListe;

public class Position implements Comparable<Position> {
    int ligne, colonne;
    int distance;

    public Position(int ligne, int colonne, int distance){
        this.colonne = colonne;
        this.ligne = ligne;
        this.distance = distance;
    }

    public int compareTo(Position other) {
        return Integer.compare(distance, other.distance);
    }

    public String toString() {
        return "(" + ligne + ", " + colonne + ")";
    }
}
