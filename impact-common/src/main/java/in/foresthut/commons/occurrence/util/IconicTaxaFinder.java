package in.foresthut.commons.occurrence.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IconicTaxaFinder {
    public static String iconicTaxaOf(String kingdom, String phylum, String _class, String order) {
        List<String> classes = new ArrayList<String>(
                Arrays.asList("Aves", "Mammalia", "Amphibia", "Squamata", "Insecta", "Arachnida"));
        List<String> actinopterygiiOrders = new ArrayList<String>(
                Arrays.asList(
                        "Acanthuriformes", "Acipenseriformes", "Alepocephaliformes", "Argentiniformes",
                        "Ateleopodiformes", "Aulopiformes", "Centrarchiformes", "Clupeiformes", "Galaxiiformes",
                        "Labriformes", "Lepidogalaxiiformes", "Myctophiformes", "Osmeriformes", "Perciformes",
                        "Polypteriformes", "Salmoniformes", "Stomiiformes", "Amiiformes", "Lepisosteiformes",
                        "Albuliformes", "Anguilliformes", "Elopiformes", "Notacanthiformes", "Characiformes",
                        "Cypriniformes", "Gonorynchiformes", "Gymnotiformes", "Siluriformes", "Hiodontiformes",
                        "Osteoglossiformes"));

        if (kingdom != null && (kingdom.equals("Plantae") || kingdom.equals("Fungi"))) {
            return kingdom;
        } else if (kingdom != null && kingdom.equals("Animalia")) {
            if (phylum != null && phylum.equals("Mollusca")) {
                return phylum;
            } else if (_class != null && classes.contains(_class)) {
                return _class;
            } else if (order != null && actinopterygiiOrders.contains(order)) {
                return "Actinopterygii";
            }
        }
        return "Other";
    }
}
