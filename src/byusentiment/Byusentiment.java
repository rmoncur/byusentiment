/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package byusentiment;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import java.util.Properties;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import java.util.List;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.ServerAddress;
import java.io.Console;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author moncur
 */
public class Byusentiment {
    
    public static StanfordCoreNLP pipeline;
    public static MongoClient mongoClient;
    public static DB db;
    public static DBCollection coll;            
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {

        //Byusentiment.loadTweets();
        Byusentiment.initialize();
        List<Tweet> tweets = Byusentiment.getTweets();
        
        //Loading the annotators and training data
        Properties props = new Properties();
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        //props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        props.put("annotators", "tokenize,ssplit,parse,pos,sentiment");
        props.put("parse.model", "lib\\stanford-corenlp-full-2014-01-04\\edu\\stanford\\nlp\\models\\lexparser\\englishPCFG.ser.gz");        
        Byusentiment.pipeline = new StanfordCoreNLP(props);
        
        for(Tweet t : tweets){
            t.calculateSentiment();
            t.saveToDB();
        }
        
        
        
//        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
//        
//        //Annotating the text
//        String text = "Lisa is very angry and hates to be awakened. I am here and love springtime"; // Add your text here!
//        Annotation document = new Annotation(text);
//        pipeline.annotate(document);
//        
//        //Looping through each sentences in this document and extracting the sentiment value
//        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
//        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class); 
//        
//        for(CoreMap sentence: sentences) {
//            
//            Tree t = sentence.get(edu.stanford.nlp.sentiment.SentimentCoreAnnotations.AnnotatedTree.class);
//            int sentimentScore = RNNCoreAnnotations.getPredictedClass(t);
//                        
//            // traversing the words in the current sentence
//            // a CoreLabel is a CoreMap with additional token-specific methods
//            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
//              // this is the text of the token
//              String word = token.get(CoreAnnotations.TextAnnotation.class);
//              // this is the POS tag of the token
//              String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
//              // this is the NER label of the token
//              String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
//              
//            }
//             
//            // this is the Stanford dependency graph of the current sentence
//            //SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
//        }
        
    }
    
    public static void initialize() throws UnknownHostException{
        Byusentiment.mongoClient = new MongoClient( "localhost" , 27017 );
        Byusentiment.db = mongoClient.getDB( "byu" );
        Byusentiment.coll = db.getCollection("tweets");
        //boolean auth = db.authenticate(myUserName, myPassword);
    }
    
    //Getting all the tweets from the database
    public static List<Tweet> getTweets(){
        
        try{
            DBCursor c = Byusentiment.coll.find();
            List<Tweet> tweets = new ArrayList<Tweet>();
            
            for(DBObject row : c){
                Tweet t = new Tweet((BasicDBObject)row);
                tweets.add(t);
            }
        
            return tweets;
        } catch(Exception ex){
            return null;
        }
        
    }
    
    //Loading a list of tweets into the database
    public static void loadTweets() throws IOException{
        try {
            CSVReader reader = new CSVReader(new FileReader("tweets-2.csv"));
            //reader.readNext();
            List myEntries = reader.readAll();
            String[] headers = (String[]) myEntries.get(0);
            
            //Adding data to the DbObject
            List<DBObject> toInsert = new ArrayList<DBObject>();
            for(int i = 1; i < myEntries.size(); i++){
                BasicDBObject doc = new BasicDBObject();
                String[] row = (String[]) myEntries.get(i);
                
                for(int j = 0; j < row.length; j++){
                    doc.append(headers[j], row[j]);
                }
                toInsert.add(doc);
            }
        
            //Inserting data into the database             
            Byusentiment.coll.insert(toInsert);

        
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Byusentiment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
