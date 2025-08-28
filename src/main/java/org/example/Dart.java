package org.example;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Dart {

    private Node from;
    private Node to;
    private Dart twin;
    private Dart next;
    private boolean visited;
    private Edge edge;

    public Dart(Node from, Node to) {
        this.from = from;
        this.to = to;
    }
}
