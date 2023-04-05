package Modele;


import Global.Configuration;
import Structures.Sequence;
import Structures.SequenceListe;

import java.util.ArrayList;
import java.util.List;

import java.util.Random;

import static Modele.Niveau.*;

class IAV1 extends IA {
    // Couleurs au format RGB (rouge, vert, bleu, un octet par couleur)
    final static int VERT = 0x00CC00;
    final static int MARRON = 0xBB7755;
    final int[] dx = {-1, 0, 1, 0}; // déplacement en x pour visiter les voisins
    final int[] dy = {0, 1, 0, -1}; // déplacement en y pour visiter les voisins

    final int inv_x[] = {1, 0, -1, 0};
    final int inv_y[] = {0, -1, 0, 1};
    public IAV1() {

    }

    Path generate_linear_solution(List<Position> caisses, List<Position> buts, Niveau niveau){
        Path path = null;
        // look at all direct lines
        for(int caisseI = 0; caisseI<caisses.size(); caisseI++){
            Position p = caisses.get(caisseI);
            for(int butI = 0; butI< buts.size(); butI++){
                Position but = buts.get(butI);
                path = niveau.cheminCaissePosition(p, but);
                if(path != null){
                    // we found one solution
                    List<Position> caisses_removed = new ArrayList<>(caisses);
                    caisses_removed.remove(caisseI);
                    List<Position> buts_removed = new ArrayList<>(buts);
                    buts_removed.remove(butI);


                    Niveau new_niveau = niveau.clone();
                    // caisse is in but
                    new_niveau.ajouteCaisse(but.ligne, but.colonne);
                    new_niveau.supprime(CAISSE, p.ligne, p.colonne);
                    // last direction caisse took was path.direction[-1]
                    // so put player at path.path[-1] + inv[path.direction[-1]]
                    int last = path.direction.size()-1;
                    int newC = but.colonne + inv_y[path.direction.get(last)];
                    int newL = but.ligne + inv_x[path.direction.get(last)];
                    new_niveau.supprime(POUSSEUR, new_niveau.pousseurL, new_niveau.pousseurC);
                    new_niveau.ajoutePousseur(newL, newC);

                    // cas de base
                    if(caisses_removed.size() == 0)
                        return path;


                    // recurence
                    if(generate_linear_solution(caisses_removed, buts_removed, new_niveau) != null)
                        return path;
                }
            }
        }


        // we have found one direct line
        return path;
    }

    @Override
    public Sequence<Coup> joue() {
        Sequence<Coup> resultat = Configuration.nouvelleSequence();

        int dir_caisse, dir_pousseur;
        Position courent, case_inverse;
        Coup coup;
        Path pousseurPosition, path = null;
        Position pousseur;

        List<Position> caisses = niveau.positionCaissesMalPlaces();
        List<Position> buts = niveau.positionButsVides();

        path = generate_linear_solution(caisses, buts, this.niveau);

        if(path == null){
            System.out.println("No solution at all for this level!");
            // we have to move one at random
            resultat.insereQueue(new Coup());
            return resultat;
        }

        System.out.println("Solution final: " + path.path);
        
        for(int coup_i = -1; coup_i<1; coup_i++) {
//        for(int coup_i = -1; coup_i<path.direction.size()-1; coup_i++) {
            pousseur = new Position(niveau.pousseurL, niveau.pousseurC, 0);
//            current[i] + direction[i+1] = current[i+1]
            dir_caisse = path.direction.get(coup_i + 1);

            // pour sauter la premiere iteration ou current[-1] n'est pas definie
            if (coup_i == -1) continue;

            courent = path.path.get(coup_i);
            case_inverse = new Position(courent.ligne + inv_x[dir_caisse], courent.colonne + inv_y[dir_caisse], 0);
            System.out.println("Prochain caisse possition: " + courent + ", il faut que pousseur soit dans" + case_inverse);
            pousseurPosition = niveau.cheminVers(pousseur, case_inverse);
            System.out.println("Pour aller a " + case_inverse + ", il faut faire: " + pousseurPosition.path + " puis on va direction: " + dir_caisse);

            for (int i = 0; i < pousseurPosition.path.size()-1; i++){
                // si on est deja dans la case ou on veut etre, on bouge pas
                if(pousseurPosition.path.size() == 1) continue;
                // si mouvement 1, il faut aller dans direction inverse
                dir_pousseur = pousseurPosition.direction.get(i);
                coup = niveau.deplace(inv_x[dir_pousseur], inv_y[dir_pousseur]);
                System.out.println("MOVING: " + inv_x[dir_pousseur] + " " + inv_y[dir_pousseur]);
                resultat.insereQueue(coup);
            }
            coup = niveau.deplace(dx[dir_caisse], dy[dir_caisse]);
            System.out.println("-MOVING: " + dx[dir_caisse] + " " + dy[dir_caisse]);
            resultat.insereQueue(coup);


            System.out.println();
        }



        return resultat;
    }
}