package org.example;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Edge {
    private Node source, target;

    public Edge(Node source, Node target) {
        this.source = source;
        this.target = target;
    }

    public boolean connects(Node n) {
        return source.equals(n) || target.equals(n);
    }

    public Node getOpposite(Node n) {
        return n.equals(source) ? target : source;
    }


}