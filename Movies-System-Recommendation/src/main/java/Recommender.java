import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.apache.log4j.PropertyConfigurator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import java.io.FileReader;  
import java.sql.Timestamp;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.util.*; 

import org.apache.log4j.Logger;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;


public class Recommender {
    private List<RecommendedItem> recommendations;
    public static void main(String[] args) {
        
    }

    Recommender(Map<String, Integer> movieReviews) {
        try { 
            File file = new File("movie_ratings.csv");
            FileWriter outputfile = new FileWriter(file, true);
            CSVWriter writer = new CSVWriter(outputfile, ',', CSVWriter.NO_QUOTE_CHARACTER);
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            Long userID = timestamp.getTime();
            for (Map.Entry<String, Integer> set:movieReviews.entrySet()){
                String[] line = {userID+"", set.getKey(), set.getValue().toString()};
                writer.writeNext(line);
            }
            writer.close();
            DataModel model = new FileDataModel(new File("movie_ratings.csv"));
            UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
            UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.7,similarity, model);
            UserBasedRecommender recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);

            recommendations = recommender.recommend(userID, 5);
        } catch (Exception e) {
            System.out.println("Ocorreram os seguintes erros:");
            System.out.print(e);
        }
    }

    public Vector<String> getRecommendations(){
        Vector<String> recommendedMovies = new Vector<String>();
        for (RecommendedItem item: recommendations){
            recommendedMovies.add(item.getItemID()+"");
        }
        return recommendedMovies;
    }

}
