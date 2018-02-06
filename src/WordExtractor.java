import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordExtractor {

    private static final String urls[] = {
            "http://www.baahrakhari.com//news-details/47660/2018-02-05",
            "http://thahakhabar.com/news/31391",
            "https://setopati.com/politics/129563"
    };

    public static void main(String[] args) {
        try {
            long startTime = System.currentTimeMillis();
            System.out.println("Please wait while data is loaded...");

            Map<String, Integer> wordOccurrences = getWords(parseHtml(getHtmlStream()));

            System.out.println("\n\n------------Words without repetition------------");
            printWords(wordOccurrences, 1);

            System.out.println("\n\n------------Words with 2 occurrences------------");
            printWords(wordOccurrences, 2);

            long stopTime = System.currentTimeMillis();
            float elapsedTime = ( stopTime - startTime ) / 1000f;
            System.out.println("------------Request completed in " + elapsedTime + " Seconds ------------" );

        } catch ( IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.out.println("Could not load the given URL");
        }
    }

    public static class Request implements Callable<InputStream> {

        private final String url;

        private Request(String url) {
            this.url = url;
        }

        @Override
        public InputStream call() throws Exception {
            return new URL(url).openStream();
        }

    }

    private static InputStream getHtmlStream() throws InterruptedException, ExecutionException {
        List<Future<InputStream>> futures = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for(String url : urls) {
            futures.add((executor.submit(new Request(url))));
        }
        executor.shutdown();

        Vector<InputStream> responses = new Vector<>();
        for(Future<InputStream> future : futures) {
            responses.add(future.get());
        }

        return new SequenceInputStream(responses.elements());
    }

    private static Map<String, Integer> getWords(String content) {
        Map<String, Integer> wordOccurrences = new HashMap<>();

        StringTokenizer words = new StringTokenizer(content, " ");
        while ( words.hasMoreTokens() ) {
            String word = words.nextToken();
            if(word.matches("[" + ",-/:@#!*$%^&.'|_+={}()"+ "]+"))  continue;

            Integer oldCount = wordOccurrences.get(word);
            if ( oldCount == null ) {
                oldCount = 0;
            }
            wordOccurrences.put(word, oldCount + 1);
        }

        return wordOccurrences;
    }

    private static void printWords(Map<String, Integer> wordOccurrences, int noOfOccurrences) {
        wordOccurrences.entrySet().stream()
                .filter(x -> x.getValue() == noOfOccurrences)
                .map(Map.Entry::getKey)
                .forEach(System.out::println);
    }

    private static String parseHtml(InputStream inputStream) throws IOException{
        String sourceLine;
        String content = "";

        InputStreamReader pageInput = new InputStreamReader(inputStream);
        BufferedReader source = new BufferedReader(pageInput);

        while ((sourceLine = source.readLine()) != null)
            content += sourceLine + "\t";

        Pattern style = Pattern.compile("<style.*?>.*?</style>");
        Matcher mstyle = style.matcher(content);
        while (mstyle.find()) content = mstyle.replaceAll("");

        Pattern script = Pattern.compile("<script.*?>.*?</script>");
        Matcher mscript = script.matcher(content);
        while (mscript.find()) content = mscript.replaceAll("");

        Pattern tag = Pattern.compile("<.*?>");
        Matcher mtag = tag.matcher(content);
        while (mtag.find()) content = mtag.replaceAll("");

        Pattern comment = Pattern.compile("<!--.*?-->");
        Matcher mcomment = comment.matcher(content);
        while (mcomment.find()) content = mcomment.replaceAll("");

        Pattern sChar = Pattern.compile("&.*?;");
        Matcher msChar = sChar.matcher(content);
        while (msChar.find()) content = msChar.replaceAll("");

        content = content.trim().replaceAll("\\s{2,}", " ");

        pageInput.close();
        source.close();

        return content;
    }
}
