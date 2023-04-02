package Modele;

public class CaissePousseur implements Comparable<CaissePousseur> {
    Position pousseur;
    Position caisse;
    int distance;
    int heuristique;

    public CaissePousseur(Position pousseur, Position caisse){
        this.pousseur = pousseur;
        this.caisse = caisse;
    }

    @Override
    public int compareTo(CaissePousseur other) {
        return Integer.compare(distance + heuristique, other.distance + other.heuristique);
    }
}
