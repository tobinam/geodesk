package com.geodesk.io;

import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.locationtech.jts.geom.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PolyReader
{
    private final BufferedReader in;
    private final GeometryFactory factory;

    public PolyReader(BufferedReader in, GeometryFactory factory)
    {
        this.in = in;
        this.factory = factory;
    }

    private static void error(String msg, int line)
    {
        throw new ParseException(String.format("Line %d: %s", line, msg));
    }

    private Polygon makePolygon(LinearRing shell, List<LinearRing> holes)
    {
        if(holes.size() == 0) return factory.createPolygon(shell);
        return factory.createPolygon(shell, holes.toArray(new LinearRing[0]));
    }

    public Geometry read() throws IOException
    {
        List<Polygon> polygons = new ArrayList<>();
        LinearRing shell;
        List<LinearRing> holes = new ArrayList<>();
        MutableDoubleList coords = new DoubleArrayList();

        int line = 1;
        String name = in.readLine();
        if(name == null) error("Expected name", line);
        shell = null;
        for(;;)
        {
            line++;
            String ringName = in.readLine();
            if(ringName == null)
            {
                error("Expected ring name", line);
            }
            ringName = ringName.trim();
            if(ringName.equals("END"))
            {
                if(shell == null) error("Must define at least one polygon", line);
                polygons.add(makePolygon(shell, holes));
                break;
            }
            if(ringName.startsWith("!")) // defines a hole
            {
                if (shell == null) error("Must define shell before holes", line);
            }
            else if(shell != null)
            {
                polygons.add(makePolygon(shell, holes));
                shell = null;
                holes.clear();
            }

            for(;;)
            {
                line++;
                String coordPair = in.readLine();
                if(coordPair == null) error("Unexpected end of file", line);
                coordPair = coordPair.trim();
                if(coordPair.equals("END")) break;
                int n = coordPair.indexOf(' ');
                if(n < 0) n = coordPair.indexOf('\t');
                if(n > 0)
                {
                    try
                    {
                        coords.add(Double.parseDouble(coordPair.substring(0,n).trim()));
                        coords.add(Double.parseDouble(coordPair.substring(n+1).trim()));
                        continue;
                    }
                    catch(NumberFormatException ex)
                    {
                        break; // fall through
                    }
                }
                error("Expected <lon> <lat>", line);
            }
            int len = coords.size();
            int coordinateCount = len / 2;
            if(coordinateCount < 6) error("Must specify at least 3 coordinate pairs", line);
            double firstX = coords.get(0);
            double firstY = coords.get(1);
            boolean closed = firstX == coords.get(len-2) && firstY == coords.get(len-1);
            CoordinateSequence seq = factory.getCoordinateSequenceFactory()
                .create(coordinateCount + (closed ? 0 : 1), 2);
            for(int i=0; i<coordinateCount; i++)
            {
                seq.setOrdinate(i, 0, coords.get(i * 2));
                seq.setOrdinate(i, 1, coords.get(i * 2 + 1));
            }
            if(!closed)
            {
                seq.setOrdinate(coordinateCount, 0, firstX);
                seq.setOrdinate(coordinateCount, 1, firstY);
            }
            LinearRing ring = factory.createLinearRing(seq);
            if(shell == null)
            {
                shell = ring;
            }
            else
            {
                holes.add(ring);
            }
        }
        if(polygons.size() == 1) return polygons.get(0);
        return factory.createMultiPolygon(polygons.toArray(new Polygon[0]));
    }
}