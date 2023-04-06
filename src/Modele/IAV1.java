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

    Sequence<Coup> resultat;

    public IAV1() {

    }

    Path generate_linear_solution(List<Position> caisses, List<Position> buts, Niveau niveau){
        Path path = null;
        Position p, but;
        // look at all direct lines
        for(int caisseI = 0; caisseI<caisses.size(); caisseI++){
            p = caisses.get(caisseI);
            for(int butI = 0; butI< buts.size(); butI++){
                but = buts.get(butI);
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


        return null;
    }

    Path force(List<CaisseMouvement> options, List<Position> caisses, List<Position> buts){
        Path path = null;
        Coup coup;
        Position pousseur = null, case_inverse = null;
        for(int i = 0; i < options.size(); i++){
            // for caisse
            CaisseMouvement current = options.get(i);
            for(int dirI = 0; dirI < 4; dirI++){
                // for mouvement of caisse
                if(current.dir[dirI] != 0){
                    Niveau new_niveau = niveau.clone();
                    // change position caisse
                    new_niveau.supprime(CAISSE, current.ligne, current.colonne);
                    new_niveau.ajouteCaisse(current.ligne + dx[dirI], current.colonne + dy[dirI]);
                    // change position pousseur
                    int newC = current.colonne;
                    int newL = current.ligne;
                    new_niveau.supprime(POUSSEUR, new_niveau.pousseurL, new_niveau.pousseurC);
                    new_niveau.ajoutePousseur(newL, newC);

                    List<Position> new_caisses = new ArrayList<>(caisses);
                    // change the caisse position in caisses
                    for(int j = 0; j < new_caisses.size(); j++){
                        boolean is_egal = current.colonne == new_caisses.get(j).colonne && current.ligne == new_caisses.get(j).ligne;
                        if(is_egal) {
                            Position np = new Position(current.ligne + dx[dirI], current.colonne + dy[dirI], 0);
                            new_caisses.set(j, np);
                        }
                    }


                    path = generate_linear_solution(new_caisses, buts, new_niveau);
                    if(path != null){

                        // actually do the move
                        pousseur = new Position(niveau.pousseurL, niveau.pousseurC, 0);
                        case_inverse = new Position(current.ligne + inv_x[dirI], current.colonne + inv_y[dirI], 0);
                        // add all up until inverse
                        Path pathMouvement1 = niveau.cheminVers(pousseur, case_inverse);
                        // add inverse
                        pathMouvement1.path.add(new Position(case_inverse.ligne + dx[dirI], case_inverse.colonne + dy[dirI], 0));

                        // calculate inverse direction index
                        int inv_dir = get_invdir(dirI);
                        pathMouvement1.direction.add(pathMouvement1.direction.size()-1, inv_dir);


                        suivrePath(pathMouvement1);



                        return path;
                    }


                }
            }
        }
        return null;
    }

    @Override
    public Sequence<Coup> joue() {
        resultat = Configuration.nouvelleSequence();
        System.out.println("--");

        Path path = null;


        List<Position> caisses = niveau.positionCaissesMalPlaces();
        List<Position> buts = niveau.positionButsVides();

        List<CaisseMouvement> options = getOptions(caisses);

        path = generate_linear_solution(caisses, buts, this.niveau);

        if(path == null)
            path = force(options, caisses, buts);


        if(path == null){
            System.out.println("No solution at all for this level!");
            // we have to move one at random
            resultat.insereQueue(new Coup());
            return resultat;
        }

        System.out.println("Solution final: " + path.path);
        generateMovement(path);
        return resultat;
    }

    void suivrePath(Path path){
        System.out.println("Adding path: " + path.path + " to our list of movements");
        Coup coup;
        int dir_pousseur;
        for (int i = 0; i < path.path.size()-1; i++){
            // si on est deja dans la case ou on veut etre, on bouge pas
            if(path.path.size() == 1) continue;
            // si mouvement 1, il faut aller dans direction inverse
            dir_pousseur = path.direction.get(i);
            coup = niveau.deplace(inv_x[dir_pousseur], inv_y[dir_pousseur]);
            resultat.insereQueue(coup);
        }
    }

    void generateMovement(Path path){

        int dir_caisse;
        Position courent, case_inverse, pousseur;
        Path pousseurPosition;

        for(int coup_i = -1; coup_i<path.direction.size()-1; coup_i++) {
            pousseur = new Position(niveau.pousseurL, niveau.pousseurC, 0);
//            current[i] + direction[i+1] = current[i+1]
            dir_caisse = path.direction.get(coup_i + 1);

            // pour sauter la premiere iteration ou current[-1] n'est pas definie
            if (coup_i == -1) continue;

            // generate path pousseurPosition which contains the path from pousseur to inverse, and the actual move
            courent = path.path.get(coup_i);
            case_inverse = new Position(courent.ligne + inv_x[dir_caisse], courent.colonne + inv_y[dir_caisse], 0);
            // add all up until inverse
            pousseurPosition = niveau.cheminVers(pousseur, case_inverse);
            // add inverse to caisse
            pousseurPosition.path.add(new Position(case_inverse.ligne + dx[dir_caisse], case_inverse.colonne + dy[dir_caisse], 0));

            // calculate inverse direction index
            int inv_dir = get_invdir(dir_caisse);
            pousseurPosition.direction.add(pousseurPosition.direction.size()-1, inv_dir);


            suivrePath(pousseurPosition);
        }
    }

    List<CaisseMouvement> getOptions(List<Position> caisses){
        Position pousseur = new Position(niveau.pousseurL, niveau.pousseurC, 0);
        List<CaisseMouvement> options = new ArrayList<CaisseMouvement>();

        for(int i = 0; i<caisses.size(); i++){
            Position caisse = caisses.get(i);
            CaisseMouvement cm = new CaisseMouvement(caisse.ligne, caisse.colonne);
            for(int j = 0; j < 4; j++){
                Position voisin = new Position(caisse.ligne + dx[j], caisse.colonne + dy[j], 0);
                if(niveau.cheminVers(pousseur, voisin) != null){
                    boolean voisin_caisse = niveau.aCaisse(caisse.ligne + inv_x[j], caisse.colonne + inv_y[j]);
                    boolean voisin_mur = niveau.aMur(caisse.ligne + inv_x[j], caisse.colonne + inv_y[j]);
                    if(!voisin_caisse && !voisin_mur)
                        cm.dir[j] = 1;
                }
            }
            options.add(cm);
        }

        return options;
    }

    int get_invdir(int dir){
        switch (dir){
            case 0:
                return 2;
            case 1:
                return 3;
            case 2:
                return 0;
            case 3:
                return 1;
        }
        return -1;
    }
}

