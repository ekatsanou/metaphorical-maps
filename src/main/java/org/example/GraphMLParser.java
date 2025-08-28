package org.example;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GraphMLParser {

    public static Graph parseGraphML(File graphMLFile) throws Exception {
        Graph graph = new Graph();
        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = saxBuilder.build(graphMLFile);
        Element root = document.getRootElement();

        Namespace yNamespace = Namespace.getNamespace("y", "http://www.yworks.com/xml/yfiles-common/3.0");
        Namespace markupNamespace = Namespace.getNamespace("markup", "http://www.yworks.com/xml/yfiles-common/markup/3.0");
        Namespace defaultNS = root.getNamespace();

        Element graphElement = root.getChild("graph", defaultNS);
        Map<String, Node> nodeMap = new HashMap<>();

        for (Element nodeElem : graphElement.getChildren("node", defaultNS)) {
            String nodeId = nodeElem.getAttributeValue("id");

            double x = 0, y = 0;
            double weight = 0;

            for (Element dataElem : nodeElem.getChildren("data", defaultNS)) {
                // RectD: node position
                Element rect = dataElem.getChild("RectD", yNamespace);
                if (rect != null) {
                    x = Double.parseDouble(rect.getAttributeValue("X"));
                    y = Double.parseDouble(rect.getAttributeValue("Y"));
                }

                // List -> Label -> Label.Text: node label (weight)
                Element listElem = dataElem.getChild("List", markupNamespace);
                if (listElem != null) {
                    Element labelElem = listElem.getChild("Label", yNamespace); // Label is in yNamespace
                    if (labelElem != null) {
                        Element labelText = labelElem.getChild("Label.Text", yNamespace);
                        if (labelText != null) {
                            try {
                                weight = Double.parseDouble(labelText.getTextTrim());
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid weight: " + labelText.getTextTrim());
                            }
                        }
                    }
                }
            }

            Node node = graph.createNodeAt(x, y);
            node.setWeight(weight);
            nodeMap.put(nodeId, node);
        }

        // Parse edges
        for (Element edgeElem : graphElement.getChildren("edge", defaultNS)) {
            String sourceId = edgeElem.getAttributeValue("source");
            String targetId = edgeElem.getAttributeValue("target");

            Node source = nodeMap.get(sourceId);
            Node target = nodeMap.get(targetId);
            if (source != null && target != null) {
                graph.addEdge(source, target);
            }
        }

        return graph;
    }



}
