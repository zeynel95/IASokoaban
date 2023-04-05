package Modele;

import Structures.SequenceListe;

import java.util.ArrayList;
import java.util.List;

public class Path {
    List<Position> path;
    List<Integer> direction;

    public Path(){
        this.path = new ArrayList();
        this.direction = new ArrayList();
    }
}
