package Modele;
/*
 * Sokoban - Encore une nouvelle version (à but pédagogique) du célèbre jeu
 * Copyright (C) 2018 Guillaume Huard
 * 
 * Ce programme est libre, vous pouvez le redistribuer et/ou le
 * modifier selon les termes de la Licence Publique Générale GNU publiée par la
 * Free Software Foundation (version 2 ou bien toute autre version ultérieure
 * choisie par vous).
 * 
 * Ce programme est distribué car potentiellement utile, mais SANS
 * AUCUNE GARANTIE, ni explicite ni implicite, y compris les garanties de
 * commercialisation ou d'adaptation dans un but spécifique. Reportez-vous à la
 * Licence Publique Générale GNU pour plus de détails.
 * 
 * Vous devez avoir reçu une copie de la Licence Publique Générale
 * GNU en même temps que ce programme ; si ce n'est pas le cas, écrivez à la Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307,
 * États-Unis.
 * 
 * Contact:
 *          Guillaume.Huard@imag.fr
 *          Laboratoire LIG
 *          700 avenue centrale
 *          Domaine universitaire
 *          38401 Saint Martin d'Hères
 */

import Global.Configuration;
import Structures.FAPListe;
//import Structures.FAPTableau;
import Structures.Iterateur;
import Structures.SequenceListe;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Niveau implements Cloneable {
	static final int VIDE = 0;
	static final int MUR = 1;
	static final int POUSSEUR = 2;
	static final int CAISSE = 4;
	static final int BUT = 8;
	int l, c;
	int[][] cases;
	String nom;
	int pousseurL, pousseurC;
	int nbButs;
	int nbCaissesSurBut;

	final int[] dx = {-1, 0, 1, 0}; // déplacement en x pour visiter les voisins
	final int[] dy = {0, 1, 0, -1}; // déplacement en y pour visiter les voisins


	Niveau() {
		cases = new int[1][1];
		l = c = 1;
		pousseurL = pousseurC = -1;
	}

	int ajuste(int cap, int objectif) {
		while (cap <= objectif) {
			cap = cap * 2;
		}
		return cap;
	}

	void redimensionne(int nouvL, int nouvC) {
		int capL = ajuste(cases.length, nouvL);
		int capC = ajuste(cases[0].length, nouvC);
		if ((capL > cases.length) || (capC > cases[0].length)) {
			int[][] nouvelles = new int[capL][capC];
			for (int i = 0; i < cases.length; i++)
				for (int j = 0; j < cases[0].length; j++)
					nouvelles[i][j] = cases[i][j];
			cases = nouvelles;
		}
		if (nouvL >= l)
			l = nouvL + 1;
		if (nouvC >= c)
			c = nouvC + 1;
	}

	void fixeNom(String s) {
		nom = s;
	}

	void videCase(int i, int j) {
		redimensionne(i, j);
		cases[i][j] = VIDE;
	}

	void supprime(int contenu, int i, int j) {
		if (aBut(i, j)) {
			if (aCaisse(i, j) && ((contenu & CAISSE | contenu & BUT) != 0))
				nbCaissesSurBut--;
			if ((contenu & BUT) != 0)
				nbButs--;
		}
		if (aPousseur(i, j) && ((contenu & POUSSEUR) != 0))
			pousseurL = pousseurC = -1;
		cases[i][j] &= ~contenu;
	}

	void ajoute(int contenu, int i, int j) {
		redimensionne(i, j);
		int resultat = cases[i][j] | contenu;
		if ((resultat & BUT) != 0) {
			if (((resultat & CAISSE) != 0) && (!aCaisse(i, j) || !aBut(i, j)))
				nbCaissesSurBut++;
			if (!aBut(i, j))
				nbButs++;
		}
		if (((resultat & POUSSEUR) != 0) && !aPousseur(i, j)) {
			if (pousseurL != -1)
				throw new IllegalStateException("Plusieurs pousseurs sur le terrain !");
			pousseurL = i;
			pousseurC = j;
		}
		cases[i][j] = resultat;
	}

	int contenu(int i, int j) {
		return cases[i][j] & (POUSSEUR | CAISSE);
	}

	public Coup elaboreCoup(int dLig, int dCol) {
		int destL = pousseurL + dLig;
		int destC = pousseurC + dCol;
		Coup resultat = new Coup();

		if (aCaisse(destL, destC)) {
			int dCaisL = destL + dLig;
			int dCaisC = destC + dCol;

			if (estOccupable(dCaisL, dCaisC)) {
				resultat.deplacementCaisse(destL, destC, dCaisL, dCaisC);
			} else {
				return null;
			}
		}
		if (!aMur(destL, destC)) {
			resultat.deplacementPousseur(pousseurL, pousseurC, destL, destC);
			return resultat;
		}
		return null;
	}

	void appliqueMouvement(Mouvement m) {
		if (m != null) {
			int contenu = contenu(m.depuisL(), m.depuisC());
			if (contenu != 0) {
				if (estOccupable(m.versL(), m.versC())) {
					supprime(contenu, m.depuisL(), m.depuisC());
					ajoute(contenu, m.versL(), m.versC());
				} else {
					Configuration.alerte("Mouvement impossible, la destination est occupée : " + m);
				}
			} else {
				Configuration.alerte("Mouvement impossible, aucun objet à déplacer : " + m);
			}
		}
	}

	void joue(Coup cp) {
		appliqueMouvement(cp.caisse());
		appliqueMouvement(cp.pousseur());
		Iterateur<Marque> it2 = cp.marques().iterateur();
		while (it2.aProchain()) {
			Marque m = it2.prochain();
			fixerMarque(m.valeur, m.ligne, m.colonne);
		}
	}

	Coup deplace(int i, int j) {
		Coup cp = elaboreCoup(i, j);
		if (cp != null)
			joue(cp);
		return cp;
	}

	void ajouteMur(int i, int j) {
		ajoute(MUR, i, j);
	}

	void ajoutePousseur(int i, int j) {
		ajoute(POUSSEUR, i, j);
	}

	void ajouteCaisse(int i, int j) {
		ajoute(CAISSE, i, j);
	}

	void ajouteBut(int i, int j) {
		ajoute(BUT, i, j);
	}

	public int lignes() {
		return l;
	}

	public int colonnes() {
		return c;
	}

	String nom() {
		return nom;
	}

	boolean estVide(int l, int c) {
		return cases[l][c] == VIDE;
	}

	public boolean aMur(int l, int c) {
		return (cases[l][c] & MUR) != 0;
	}

	public boolean aBut(int l, int c) {
		return (cases[l][c] & BUT) != 0;
	}

	public boolean aPousseur(int l, int c) {
		return (cases[l][c] & POUSSEUR) != 0;
	}

	public boolean aCaisse(int l, int c) {
		return (cases[l][c] & CAISSE) != 0;
	}

	public boolean estOccupable(int l, int c) {
		return (cases[l][c] & (MUR | CAISSE | POUSSEUR)) == 0;
	}

	public boolean estTermine() {
		return nbCaissesSurBut == nbButs;
	}

	public int lignePousseur() {
		return pousseurL;
	}

	public int colonnePousseur() {
		return pousseurC;
	}

	// Par convention, la méthode clone de java requiert :
	// - que la classe clonée implémente Cloneable
	// - que le resultat soit construit avec la méthode clone de la classe parente (pour qu'un clonage
	//   profond fonctionne sur toute l'ascendence de l'objet)
	// Le nouvel objet sera de la même classe que l'objet cible du clonage (creation spéciale dans Object)
	@Override
	public Niveau clone() {
		try {
			Niveau resultat = (Niveau) super.clone();
			// Le clone de base est un clonage à plat pour le reste il faut
			// cloner à la main : cela concerne les cases
			resultat.cases = new int[cases.length][];
			for (int i=0; i< cases.length; i++)
				resultat.cases[i] = cases[i].clone();
			return resultat;
		} catch (CloneNotSupportedException e) {
			Configuration.erreur("Bug interne, niveau non clonable");
		}
		return null;
	}

	public int marque(int i, int j) {
		return (cases[i][j] >> 8) & 0xFFFFFF;
	}

	public void fixerMarque(int m, int i, int j) {
		cases[i][j] = (cases[i][j] & 0xFF) | (m << 8);
	}

//	SequenceListe<Position> voisinsAccessibles(Position pos){
//		int ligne = pos.ligne;
//		int colonne = pos.colonne;
//		SequenceListe<Position> results = new SequenceListe<Position>();
//		if( !aMur(ligne-1,colonne) && !aCaisse(ligne-1, colonne)){
//			// up
//			results.insereQueue(new Position(ligne-1, colonne));
//		}
//		if(!aMur(ligne, colonne-1) && !aCaisse(ligne, colonne-1)){
//			// left
//			results.insereQueue(new Position(ligne, colonne-1));
//		}
//		if(!aMur(ligne, colonne-1) && !aCaisse(ligne+1, colonne)){
//			// down
//			results.insereQueue(new Position(ligne+1, colonne));
//		}
//		if(!aMur(ligne, colonne-1) && !aCaisse(ligne, colonne+1)){
//			// right
//			results.insereQueue(new Position(ligne, colonne+1));
//		}
//
//		return results;
//	}

	SequenceListe<Position> positionCaisses(){
		SequenceListe<Position> listeCaisse = new SequenceListe<Position>();

		for(int ligne = 0; ligne< l; ligne++){
			for(int colonne = 0; colonne< c; colonne++){
				if(aCaisse(ligne, colonne)){
					listeCaisse.insereQueue(new Position(ligne, colonne, 0));
				}
			}
		}

		return listeCaisse;
	}

	boolean cheminVers(Position p1, Position p2) {

		// dijkstra
		int[][] distances = new int[l][c]; // Tableau des distances à chaque point
		boolean[][] visited = new boolean[l][c]; // Tableau pour savoir si un point a déjà été visité
		FAPListe<Position> FAP = new FAPListe<Position>();

		for (int i = 0; i < l; i++) {
			Arrays.fill(distances[i], 2000);
		}
		distances[p1.ligne][p1.colonne] = 0;
		p1.distance = 0;
		FAP.insere(p1);

		while (!FAP.estVide()) {
			Position courant = FAP.extrait();

			// Si le noeud a déjà été visité, on passe au suivant
			if (visited[courant.ligne][courant.colonne]) {
				continue;
			}
			visited[courant.ligne][courant.colonne] = true;

			// Si on est arrivé à la fin, on peut arrêter la recherche
			if (courant.ligne == p2.ligne && courant.colonne == p2.colonne) {
				break;
			}

			// Parcours des voisins
			for (int i = 0; i < 4; i++) {
				Position voisin = new Position(courant.ligne + dx[i], courant.colonne + dy[i], 0);

				// Vérification des limites de la matrice
				if (voisin.ligne < 0 || voisin.colonne < 0 || voisin.ligne >= l || voisin.colonne >= c) {
					continue;
				}

				// Vérification si le voisin est un obstacle
				if (aMur(voisin.ligne, voisin.colonne) || aCaisse(voisin.ligne, voisin.colonne)) {
					continue;
				}
				int newDistance = courant.distance + 1;

				//
				if (newDistance < distances[voisin.ligne][voisin.colonne]) {
					distances[voisin.ligne][voisin.colonne] = newDistance;
					voisin.distance = newDistance;
					FAP.insere(voisin);
				}
			}
		}


		if(distances[p2.ligne][p2.colonne] != 2000){
			affiche_dijkstra(p1, p2, distances);
			return true;
		}
		return false;

	}

	void init_distance_debut(Position pousseur, Position caisse, int[][][] distances){
//		fait dijkstra pour les 4 voisin,
//		les voisin accessibles, distance 0
		for (int i = 0; i < 4; i++) {
			Position voisin = new Position(caisse.ligne+dx[i], caisse.colonne+dy[i], 0);
			// Vérification des limites de la matrice
			if (voisin.ligne < 0 || voisin.colonne < 0 || voisin.ligne >= l || voisin.colonne >= c) {
				continue;
			}
			// Vérification si le voisin est un obstacle
			if (aMur(voisin.ligne, voisin.colonne) || aCaisse(voisin.ligne, voisin.colonne)) {
				continue;
			}
//			if accessible par dijkstra
			if(cheminVers(pousseur, voisin)){
				distances[caisse.ligne][caisse.colonne][i] = 0;
			}
		}
	}

	int find_direction(Position pousseur, Position caisse){
		if(pousseur.ligne == caisse.ligne){
			if(pousseur.colonne < caisse.colonne){
//				left
				return 0;
			}
			// right
			return 2;
		} else if (pousseur.colonne == caisse.colonne) {
			// meme colonne
			if(pousseur.ligne > caisse.ligne){
				// down
				return 1;
			}
			// up
			return 3;
		}
//		pousseur pas a cote
		return -1;
	}

	boolean cheminCaissePosition(Position pCaisse, Position p2) {

		// dijkstra
		int[][][] distances = new int[l][c][4]; // Tableau des distances à chaque point
		boolean[][][] visited = new boolean[l][c][4]; // Tableau pour savoir si un point a déjà été visité

		FAPListe<CaissePousseur> FAP = new FAPListe<CaissePousseur>();

		for (int i = 0; i < l; i++) {
			for (int j = 0; j < c; j++) {
				Arrays.fill(distances[i][j], 2000);
				Arrays.fill(visited[i][j], false);
			}
		}

		Position pPousseur = new Position(this.pousseurL, this.pousseurC, 0);
		init_distance_debut(pPousseur, pCaisse,distances);
		CaissePousseur cpCourant = new CaissePousseur(pPousseur, pCaisse);

		FAP.insere(cpCourant);
		CaissePousseur courant;
		while (!FAP.estVide()) {
			courant = FAP.extrait();

			// Si le CaissePousseur a déjà été visité, on passe au suivant
			int movement_direction = find_direction(courant.pousseur, courant.caisse);

			// on saute le premier config ou le jouer n'est pas a cote
			if(movement_direction != -1){
				// ici le jouer est a cote de la caisse
				if (visited[courant.caisse.ligne][courant.caisse.colonne][movement_direction]) {
					continue;
				}
				visited[courant.caisse.ligne][courant.caisse.colonne][movement_direction] = true;
			};

			// Si on est arrivé à la fin, on peut arrêter la recherche
			if (courant.caisse.ligne == p2.ligne && courant.caisse.colonne == p2.colonne) {
				break;
			}

			// Parcours des voisins
			for (int i = 0; i < 4; i++) {
				Position voisin = new Position(courant.caisse.ligne + dx[i], courant.caisse.colonne + dy[i], 0);
				Position pInverse = new Position(courant.caisse.ligne - dx[i], courant.caisse.colonne - dy[i], 0);

				boolean egal_oldCaisse = voisin.ligne != pCaisse.ligne || voisin.colonne != pCaisse.colonne;
				boolean egal_actuel = courant.caisse.ligne != pCaisse.ligne || courant.caisse.colonne != pCaisse.colonne;

				// si oldCaisse, ne teste pas
				// si actuell, test
				if(egal_oldCaisse || !egal_actuel){
					// si le voisin est la position de la caisse originel, il faut pas le planter
					// ici on est dans un caisse different

					// Vérification des limites de la matrice
					if (voisin.ligne < 0 || voisin.colonne < 0 || voisin.ligne >= l || voisin.colonne >= c) {
						continue;
					}

					// Vérification si le voisin est un obstacle
					if (aMur(voisin.ligne, voisin.colonne) || aCaisse(voisin.ligne, voisin.colonne)) {
						continue;
					}



					//inverse obstacle
					if(aMur(pInverse.ligne, pInverse.colonne) || aCaisse(pInverse.ligne, pInverse.colonne))
						continue;
				}

				Niveau niveauCloneCourant = this.clone();
				niveauCloneCourant.misAJour(courant.pousseur, pCaisse, courant.caisse);


				//inverse pas accessible
				if(!niveauCloneCourant.cheminVers(courant.pousseur, pInverse))
					continue;


				int newDistance = courant.distance + 1;


				if (newDistance < distances[voisin.ligne][voisin.colonne][i]) {
					distances[voisin.ligne][voisin.colonne][i] = newDistance;
//																  pousseur,    caisse
					CaissePousseur prochainCP = new CaissePousseur(courant.caisse, voisin);
					prochainCP.distance = newDistance;
					FAP.insere(prochainCP);
				}
			}
		}
		for(int i = 0; i<4; i++){
			if(distances[p2.ligne][p2.colonne][i] != 2000){
	//			affiche_dijkstra(p1, p2, distances);
				return true;
			}
		}
		return false;
	}

	void misAJour(Position pousseur, Position oldCaisse, Position newCaisse){
		// met la caisse de old a new
		this.cases[oldCaisse.ligne][oldCaisse.colonne] = VIDE;
		ajouteCaisse(newCaisse.ligne, newCaisse.colonne);
		// met a jour la position de le pousseur
		this.pousseurC = pousseur.colonne;
		this.pousseurL = pousseur.ligne;
	}

	void affiche_dijkstra(Position p1, Position p2, int[][] distances){

		// Affichage du chemin le plus court
		List<Position> path = new ArrayList<>();
		int endColonne = p2.colonne;
		int endLigne = p2.ligne;
		int startColonne = p1.colonne;
		int startLigne = p1.ligne;
		while (!(endLigne == startLigne && endColonne == startColonne)) {
			path.add(new Position(endLigne, endColonne, distances[endLigne][endColonne]));
			int min = 2000;
//			int voisinMinLigne;
//			int voisinMinColonne;
			for (int i = 0; i < 4; i++) {
				int nx = endLigne + dx[i];
				int ny = endColonne + dy[i];
				if (nx < 0 || ny < 0 || nx >= l || ny >= c) {
					continue;
				}
				if(distances[nx][ny] < min){
					min = distances[nx][ny];
					endLigne = nx;
					endColonne = ny;

				}
			}
		}
		path.add(new Position(startLigne, startColonne, distances[startLigne][startColonne]));
		Collections.reverse(path);
		System.out.println("Chemin le plus court : " + path);
	}




}
