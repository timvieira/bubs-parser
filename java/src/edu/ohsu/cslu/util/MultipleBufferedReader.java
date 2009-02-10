package edu.ohsu.cslu.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

/**
 * Allows multiple consumers to read the same data stream from a {@link Reader}
 * 
 * TODO: Test thread-safety, support read() as well as readLine(), allow purging of read data after
 * all readers have consumed it, etc.
 * 
 * @author Aaron Dunlop
 * @since Oct 23, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class MultipleBufferedReader extends BufferedReader
{
    private final ArrayList<String> lines = new ArrayList<String>(1024);

    public MultipleBufferedReader(Reader in)
    {
        super(in);
    }

    @Override
    public int read() throws IOException
    {
        return super.read();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException
    {
        return super.read(cbuf, off, len);
    }

    @Override
    public String readLine() throws IOException
    {
        String line = super.readLine();
        lines.add(line);
        return line;
    }

    public BufferedReader newReader()
    {
        return new InternalBufferedReader();
    }

    private class InternalBufferedReader extends BufferedReader
    {
        private int currentLine = 0;

        public InternalBufferedReader()
        {
            super(null);
        }

        @Override
        public String readLine()
        {
            if (currentLine < lines.size())
            {
                return lines.get(currentLine++);
            }

            return null;
        }

    }
}
