package org.dataone.cn.indexer.parser;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.convert.SolrLatitudeConverter;
import org.dataone.cn.indexer.convert.SolrLongitudeConverter;
import org.dataone.cn.indexer.solrhttp.SolrElementField;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import ch.hsr.geohash.GeoHash;

/**
 * Extract one representative bounding box and per-geometry center geohashes.
 * 
 * For JSON-LD datasets, `Place.geo` values of type `GeoCoordinates` or `GeoShape`
 * (`box`, `line`, and `polygon`) are converted to individual bounding boxes.
 * As with EML, multi-valued geohashes represent each box's center; the four
 * single-valued coordinate fields can hold only one box. When multiple geometries
 * are present, the representative box is chosen deterministically by ascending
 * south, west, north, and east coordinates. Other boxes are not retained as bounds
 * in the index. Antimeridian-crossing boxes retain their west-greater-than-east
 * representation. Invalid geometries are logged and excluded.
 */
public class SchemaOrgSpatialField implements ISolrDataField {
    private static final Log log = LogFactory.getLog(SchemaOrgSpatialField.class);
    private static final Query QUERY = QueryFactory.create(
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "PREFIX SO: <http://schema.org/> "
            + "SELECT ?kind ?value ?lat ?lon WHERE { "
            + "  ?dataset rdf:type SO:Dataset; SO:spatialCoverage ?place . "
            + "  ?place rdf:type SO:Place; SO:geo ?geo . "
            + "  { ?geo rdf:type SO:GeoCoordinates . "
            + "    OPTIONAL { ?geo SO:latitude ?lat } OPTIONAL { ?geo SO:longitude ?lon } "
            + "    BIND('point' AS ?kind) } "
            + "  UNION "
            + "  { ?geo rdf:type SO:GeoShape; ?property ?value . "
            + "    VALUES (?property ?kind) { (SO:box 'box') (SO:line 'line') "
            + "                              (SO:polygon 'polygon') } } "
            + "}");

    static {
        // Jena lazily initializes result variables; initialize before sharing the query across workers.
        QUERY.setResultVars();
    }

    private final SolrLatitudeConverter latitudeConverter = new SolrLatitudeConverter();
    private final SolrLongitudeConverter longitudeConverter = new SolrLongitudeConverter();

    @Override
    public String getName() {
        return "schemaOrgSpatial";
    }

    @Override
    public List<SolrElementField> getFields(byte[] data, String identifier) {
        throw new UnsupportedOperationException("Schema.org spatial extraction requires an RDF dataset");
    }

    public List<SolrElementField> getFields(Dataset dataset) {
        Bounds representative = null;
        List<SolrElementField> fields = new ArrayList<SolrElementField>();
        QueryExecution execution = QueryExecutionFactory.create(QUERY, dataset);
        try {
            ResultSet results = execution.execSelect();
            while (results.hasNext()) {
                QuerySolution row = results.next();
                String kind = row.getLiteral("kind").getString();
                try {
                    Bounds geometry;
                    if ("point".equals(kind)) {
                        geometry = point(lexical(row, "lat"), lexical(row, "lon"));
                    } else {
                        geometry = shape(kind, lexical(row, "value"));
                    }
                    if (representative == null || geometry.compareTo(representative) < 0) {
                        representative = geometry;
                    }
                    addGeohashes(fields, geometry);
                } catch (IllegalArgumentException e) {
                    log.warn("Ignoring invalid schema.org " + kind + " geometry: " + e.getMessage());
                }
            }
        } finally {
            execution.close();
        }

        if (representative == null) {
            return fields;
        }
        fields.add(new SolrElementField("southBoundCoord", Double.toString(representative.south)));
        fields.add(new SolrElementField("westBoundCoord", Double.toString(representative.west)));
        fields.add(new SolrElementField("northBoundCoord", Double.toString(representative.north)));
        fields.add(new SolrElementField("eastBoundCoord", Double.toString(representative.east)));
        return fields;
    }

    private void addGeohashes(List<SolrElementField> fields, Bounds bounds) {
        double longitude = bounds.east;
        if (longitude < bounds.west) {
            longitude += 360;
        }
        longitude = (longitude + bounds.west) / 2;
        if (longitude > 180) {
            longitude -= 360;
        }
        String hash = GeoHash.withCharacterPrecision((bounds.south + bounds.north) / 2,
                longitude, 9).toBase32();
        for (int length = 1; length <= 9; length++) {
            fields.add(new SolrElementField("geohash_" + length, hash.substring(0, length)));
        }
    }

    private String lexical(QuerySolution row, String name) {
        if (row.get(name) == null || !row.get(name).isLiteral()) {
            throw new IllegalArgumentException(name + " is missing or is not a literal");
        }
        return row.getLiteral(name).getLexicalForm();
    }

    private Bounds point(String lat, String lon) {
        double latitude = coordinate(lat, true);
        double longitude = coordinate(lon, false);
        return new Bounds(latitude, longitude, latitude, longitude);
    }

    private Bounds shape(String kind, String value) {
        String trimmed = value.trim();
        String[] parts = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s*,\\s*|\\s+");
        int minimum = "box".equals(kind) ? 4 : ("line".equals(kind) ? 4 : 6);
        if (parts.length < minimum || parts.length % 2 != 0
                || ("box".equals(kind) && parts.length != 4)) {
            throw new IllegalArgumentException("unexpected number of coordinates in " + value);
        }
        Bounds first = point(parts[0], parts[1]);
        if ("box".equals(kind)) {
            Bounds last = point(parts[2], parts[3]);
            if (first.south > last.north) {
                throw new IllegalArgumentException("south latitude exceeds north latitude in " + value);
            }
            return new Bounds(first.south, first.west, last.north, last.east);
        }
        for (int i = 2; i < parts.length; i += 2) {
            first.include(point(parts[i], parts[i + 1]));
        }
        return first;
    }

    private double coordinate(String value, boolean latitude) {
        String converted = latitude ? latitudeConverter.convert(value) : longitudeConverter.convert(value);
        if (converted == null) {
            throw new IllegalArgumentException("invalid coordinate " + value);
        }
        double result = Double.parseDouble(converted);
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new IllegalArgumentException("non-finite coordinate " + value);
        }
        return result;
    }

    private static class Bounds implements Comparable<Bounds> {
        private double south;
        private double west;
        private double north;
        private double east;

        Bounds(double south, double west, double north, double east) {
            this.south = south;
            this.west = west;
            this.north = north;
            this.east = east;
        }

        void include(Bounds other) {
            south = Math.min(south, other.south);
            north = Math.max(north, other.north);
            west = Math.min(west, other.west);
            east = Math.max(east, other.east);
        }

        @Override
        public int compareTo(Bounds other) {
            int result = Double.compare(south, other.south);
            if (result == 0) result = Double.compare(west, other.west);
            if (result == 0) result = Double.compare(north, other.north);
            if (result == 0) result = Double.compare(east, other.east);
            return result;
        }
    }
}
