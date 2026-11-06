package org.dataone.cn.indexer.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.junit.Test;

import ch.hsr.geohash.GeoHash;

public class SchemaOrgSpatialFieldTest {
    private SolrDoc process(String coverage) throws Exception {
        String json = "{\"@context\":\"https://schema.org/\","
                + "\"@type\":\"Dataset\",\"spatialCoverage\":" + coverage + "}";
        JsonLdSubprocessor processor = new JsonLdSubprocessor();
        List<ISolrDataField> fields = new ArrayList<ISolrDataField>();
        fields.add(new SchemaOrgSpatialField());
        processor.setFieldList(fields);
        Map<String, SolrDoc> docs = new HashMap<String, SolrDoc>();
        processor.processDocument("test", docs,
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        return docs.get("test");
    }

    private String place(String geo) {
        return "{\"@type\":\"Place\",\"geo\":" + geo + "}";
    }

    private String point(String lat, String lon) {
        return "{\"@type\":\"GeoCoordinates\",\"latitude\":" + lat + ",\"longitude\":" + lon + "}";
    }

    private String shape(String kind, String value) {
        return "{\"@type\":\"GeoShape\",\"" + kind + "\":\"" + value + "\"}";
    }

    private void assertBounds(SolrDoc doc, double south, double west, double north, double east,
            double centerLat, double centerLon, double... otherCenters) {
        assertEquals(Collections.singletonList(Double.toString(south)), doc.getAllFieldValues("southBoundCoord"));
        assertEquals(Collections.singletonList(Double.toString(west)), doc.getAllFieldValues("westBoundCoord"));
        assertEquals(Collections.singletonList(Double.toString(north)), doc.getAllFieldValues("northBoundCoord"));
        assertEquals(Collections.singletonList(Double.toString(east)), doc.getAllFieldValues("eastBoundCoord"));
        for (int length = 1; length <= 9; length++) {
            List<String> expected = new ArrayList<String>();
            expected.add(GeoHash.withCharacterPrecision(centerLat, centerLon, length).toBase32());
            for (int i = 0; i < otherCenters.length; i += 2) {
                expected.add(GeoHash.withCharacterPrecision(otherCenters[i], otherCenters[i + 1], length).toBase32());
            }
            List<String> actual = doc.getAllFieldValues("geohash_" + length);
            Collections.sort(expected);
            Collections.sort(actual);
            assertEquals(expected, actual);
        }
    }

    @Test
    public void numericAndStringPointsProducePointBoundsAndGeohashes() throws Exception {
        assertBounds(process(place(point("39.3280", "120.1633"))),
                39.328, 120.1633, 39.328, 120.1633, 39.328, 120.1633);
        assertBounds(process(place(point("\"36.7016\"", "\"-121.90504\""))),
                36.7016, -121.90504, 36.7016, -121.90504, 36.7016, -121.90504);
    }

    @Test
    public void multiplePointsHaveSeparateGeohashes() throws Exception {
        String geos = "[" + point("-17.65", "50") + "," + point("-19", "51") + "]";
        assertBounds(process(place(geos)), -19, 51, -19, 51, -19, 51, -17.65, 50);
        String reversed = "[" + point("-19", "51") + "," + point("-17.65", "50") + "]";
        assertBounds(process(place(reversed)), -19, 51, -19, 51, -19, 51, -17.65, 50);
    }

    @Test
    public void lineAndPolygonUseAllVertices() throws Exception {
        assertBounds(process(place(shape("line", "39.3280 120.1633 40.445 123.7878"))),
                39.328, 120.1633, 40.445, 123.7878, (39.328 + 40.445) / 2,
                (120.1633 + 123.7878) / 2);
        assertBounds(process(place(shape("polygon",
                "39.3280,120.1633 40.445,123.7878 41,121 39.77,122.42 39.3280,120.1633"))),
                39.328, 120.1633, 41, 123.7878, (39.328 + 41) / 2, (120.1633 + 123.7878) / 2);
    }

    @Test
    public void boxAndDegenerateLineUseTheirCenters() throws Exception {
        assertBounds(process(place(shape("box", "39.3280 120.1633 40.445 123.7878"))),
                39.328, 120.1633, 40.445, 123.7878, (39.328 + 40.445) / 2,
                (120.1633 + 123.7878) / 2);
        assertBounds(process(place(shape("line", "10 20 10 40"))),
                10, 20, 10, 40, 10, 30);
    }

    @Test
    public void mixedGeometriesAndPlacesKeepSeparateCenters() throws Exception {
        String geos = "[" + point("-19", "51") + "," + shape("line", "-18 49 -17 52") + "]";
        String places = "[" + place(geos) + "," + place(shape("box", "-20 48 -16 53")) + "]";
        assertBounds(process(places), -20, 48, -16, 53, -18, 50.5, -19, 51, -17.5, 50.5);
    }

    @Test
    public void crossingBoxIsPreservedAlongsideOtherGeometry() throws Exception {
        String box = shape("box", "50 175 75 -125");
        assertBounds(process(place(box)), 50, 175, 75, -125, 62.5, -155);
        assertBounds(process(place("[" + box + "," + point("60", "170") + "]")),
                50, 175, 75, -125, 62.5, -155, 60, 170);
    }

    @Test
    public void invalidGeometriesAreExcludedWithoutPartialBounds() throws Exception {
        String geos = "[" + shape("line", "1 2 3") + "," + point("91", "3") + ","
                + "{\"@type\":\"GeoCoordinates\",\"latitude\":4}," + point("1", "2") + "]";
        assertBounds(process(place(geos)), 1, 2, 1, 2, 1, 2);
        SolrDoc doc = process(place(shape("box", "5 2 1 4")));
        assertTrue(doc.getAllFieldValues("southBoundCoord").isEmpty());
        assertTrue(doc.getAllFieldValues("geohash_1").isEmpty());
    }
}
