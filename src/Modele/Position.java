package Modele;
//import Structures.SequenceListe;

public class Position implements Comparable<Position> {
    int ligne, colonne;
    int distance;
    int heuristique;

    public Position(int ligne, int colonne, int distance){
        this.colonne = colonne;
        this.ligne = ligne;
        this.distance = distance;
    }

    public int compareTo(Position other) {

        return Integer.compare(distance + heuristique, other.distance + other.heuristique);
    }

    public String toString() {
        return "(" + ligne + ", " + colonne + ")";
    }
}
