import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;

import java.util.Properties;

public class Main {
    /**
     * Driver class method.
     * @param args Not used.
     * @throws Exception Throws file exception.
     */

    public static void main(String[] args) throws Exception {

        File folder = new File("dataset");

        if (args.length != 0){
            folder = new File(args[0]);
        }

        File[] listOfFiles = folder.listFiles();
        WordMap<String, FileMap<String, ArrayList<Integer>>> wordMap = new WordMap<>();
        FileMap<String, Integer> fileLengths = new FileMap<>();

        //Iter through files in folder
        for (File file : listOfFiles) {
            if (file.isFile()) {



                //get content of file as string
                Scanner scanner = new Scanner(Paths
                        .get(folder.getName() + "\\" + file.getName()), StandardCharsets.UTF_8.name());
                String content = scanner.useDelimiter("\\A").next();

                //this is the string of the entire content of a file
                String fileName = file.getName();
                scanner.close();


                //We replace punctuation with spaces,
                //Multiple spaces with only one
                String filtered = filterString(content);

                //NLP
                // set up pipeline properties
                Properties props = new Properties();
                // set the list of annotators to run
                props.setProperty("annotators", "tokenize,pos,lemma");
                // set a property for an annotator, in this case the coref annotator is being
                // set to use the neural algorithm
                props.setProperty("coref.algorithm", "neural");
                // build pipeline
                StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
                // create a document object
                CoreDocument document = new CoreDocument(filtered);
                // annnotate the document
                pipeline.annotate(document);
                //System.out.println(document.tokens());

                //index tracks word index in words array
                int index = 0;
                for (CoreLabel tok : document.tokens()){

                    String word = tok.lemma();

                    //case for word not in wordmap
                    if (!wordMap.containsKey(word)){
                        FileMap<String, ArrayList<Integer>> fm = new FileMap<>();
                        ArrayList<Integer> al = new ArrayList<>();
                        al.add(index);
                        fm.put(fileName, al);
                        wordMap.put(word, fm);
                    }

                    //case for word already in wordmap
                    else{
                        FileMap<String, ArrayList<Integer>> fm = wordMap.get(word);
                        //case where file in filemap
                        if (fm.containsKey(fileName)){
                            ArrayList<Integer> al = fm.get(fileName);
                            al.add(index);
                            fm.put(fileName, al);
                            wordMap.put(word, fm);
                        }
                        //case where file not
                        else{
                            ArrayList<Integer> al = new ArrayList<>();
                            al.add(index);
                            fm.put(fileName, al);
                            wordMap.put(word, fm);
                        }
                    }
                    index += 1;
                }

                fileLengths.put(file.getName(), index);

            }
        }

        //query file
        File query = new File("query.txt");

        if (args.length != 0){
            query = new File(args[1]);
        }

        //possible commands
        String searchPrompt = "search ";
        String bigramPrompt = "the most probable bigram of ";

        if (query.isFile()){
            //saves path of query into string
            String str = Files.readString(Path.of(query.getPath()));

            //puts each line of query into ArrayList
            List<String> lines = new ArrayList<>();
            str.lines().forEach(s -> lines.add(s));

            for (String command : lines){
                //command is search query
                if (command.startsWith(searchPrompt)){
                    String searchTerm = command.substring(searchPrompt.length());

                    //search term appears at least once
                    if (wordMap.containsKey(searchTerm)){
                        FileMap<String, ArrayList<Integer>> tempFM = wordMap.get(searchTerm);

                        double highestTFIDF = 0;
                        String relevantDoc = "";

                        //total amount of documents scanned and amount of documents containing word
                        double totalD = listOfFiles.length;
                        double docCount = tempFM.keySet().size();

                        //key is file name
                        for(String key: tempFM.keySet()){
                            ArrayList<Integer> indexList = tempFM.get(key);
                            double fileTFIDF = TFIDF(indexList.size(), fileLengths.get(key), docCount, totalD);

                            //updates highestTFIDF and name of associated document
                            if (fileTFIDF==highestTFIDF){
                                relevantDoc = shorterString(relevantDoc,key);
                            }
                            if (fileTFIDF>highestTFIDF){
                                highestTFIDF = fileTFIDF;
                                relevantDoc = key;
                            }
                        }

                        System.out.println(relevantDoc);
                    }

                    //search term doesn't appear in any document
                    else{
                        System.out.println("word not found");
                    }
                }
                //command is bigram query
                else if (command.startsWith(bigramPrompt)){
                    String bigramTerm = command.substring(bigramPrompt.length());

                    //search term appears at least once
                    if (wordMap.containsKey(bigramTerm)){

                        //create bigrams
                        FileMap<String, ArrayList<Integer>> tempFM = wordMap.get(bigramTerm);
                        WordMap<String, Integer> bigramMap = new WordMap<>();

                        double wordCount = tempFM.size();

                        //goes through each file the queried word appears in
                        for (String currentFile: tempFM.keySet()){

                            //index of each time the word appears in file
                            for (int index: tempFM.get(currentFile)){

                                //goes through each word in wordMap
                                for (String currentWord: wordMap.keySet()){
                                    FileMap<String, ArrayList<Integer>> currentFM = wordMap.get(currentWord);

                                    //checks if word appears in file
                                    if (currentFM.containsKey(currentFile)){
                                        ArrayList<Integer> currentIndexArray = currentFM.get(currentFile);

                                        //checks if word appears after queried word
                                        if (currentIndexArray.contains(index + 1)){

                                            String currentBigram = bigramTerm + " " + currentWord;

                                            //saves bigram to bigramMap if
                                            if (!bigramMap.containsKey(currentBigram)){
                                                bigramMap.put(currentBigram, 1);
                                            }
                                            else if (bigramMap.containsKey(currentBigram)){
                                                bigramMap.put(currentBigram, bigramMap.get(currentBigram)+1);
                                            }

                                        }
                                    }

                                }
                            }
                        }

                        //find most probable bigram
                        String mostProbableBigram="";
                        double highestProbability = 0;

                        for (String bigram : bigramMap.keySet()){

                            int bigramCount = bigramMap.get(bigram);

                            double probability = (double) bigramCount/wordCount;

                            if (probability>highestProbability){
                                highestProbability = probability;
                                mostProbableBigram = bigram;
                            }

                            else if (probability==highestProbability){
                                mostProbableBigram = shorterString(mostProbableBigram,bigram);
                            }

                        }

                        System.out.println(mostProbableBigram);

                    }

                    //search term doesn't appear in any document
                    else{
                        System.out.println("word not found");
                    }
                }
            }
        }

    }

    static String filterString(String s){
        s = s.replaceAll("\\p{Punct}", " ");
        s = s.replaceAll("\n", " ").replaceAll("\r", "");
        s = s.trim().replaceAll(" +", " ");

        return s;
    }

    //calculate term frequency-inverse document frequency
    static double TFIDF(double wordCount, double totalW, double docCount, double totalD){
        double TF = wordCount/totalW;
        double IDF = Math.log(totalD/docCount);
        double result = TF*IDF;

        return result;
    }

    //compare two strings, return shortest string based on lexicographic order
    static String shorterString(String str1, String str2){
        if (str1.length()>str2.length()){
            return str2;
        }

        else if (str2.length()>str1.length()){
            return str1;
        }

        else {
            if (str1.compareTo(str2)<=0){
                return str1;
            }
            else{
                return str2;
            }
        }
    }
}

//Read all files in folder doc:
//Sources: https://stackoverflow.com/questions/1844688/how-to-read-all-files-in-a-folder-from-java