package Modele;

public class CaisseMouvement {
    int ligne, colonne;
    int[] dir = {0, 0, 0, 0};

    public CaisseMouvement(int ligne, int colonne){
        this.ligne = ligne;
        this.colonne = colonne;
    }
}
