package cd.test;

import cd.Main;
import cd.TreeLoader;
import cd.tree.Node;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class ExPTest
{
    @Test
    public void test() throws IOException
    {
        Path dataPath = Paths.get("test_data");
        for(int i=0; i<=3; i++)
        {
            System.err.println("Testing case: "+i);
            test_one(dataPath.resolve("test" + i + ".txt"), dataPath.resolve("expected" + i + ".txt"));
        }
    }


    private void test_one(Path input, Path expected) throws IOException
    {
        Node root = TreeLoader.load(input);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Main.run(root, ps);
        ps.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Scanner scanner = new Scanner(bais);
        Scanner ex = new Scanner(Files.newBufferedReader(expected));

        diff(scanner, ex);
    }

    private void diff(Scanner scanner, Scanner ex)
    {
        while(scanner.hasNext() && ex.hasNext())
            Assert.assertEquals(ex.next(), scanner.next());

        Assert.assertFalse(scanner.hasNext());
        if(ex.hasNext())
            System.out.println("Next token: '"+ ex.next() +"'");

        Assert.assertFalse(ex.hasNext());
    }
}
